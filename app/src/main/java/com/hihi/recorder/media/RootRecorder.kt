package com.hihi.recorder.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.hihi.recorder.R
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.root.RootChecker
import com.hihi.recorder.root.RootFailure
import com.hihi.recorder.root.RootShell
import com.hihi.recorder.root.RootStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Root recording engine. `screenrecord` captures video in 180 second chunks
 * without MediaProjection consent, while a MediaRecorder captures the
 * microphone continuously. On stop the chunks and microphone track are remuxed
 * into a single MP4.
 */
class RootRecorder(
    private val context: Context,
    private val settings: RecorderSettings,
    private val outputFile: File,
    private val onUnexpectedStop: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stopping = AtomicBoolean(false)
    private val currentProcess = AtomicReference<Process?>()
    private val currentPid = AtomicInteger(0)
    private val firstSegmentReady = CompletableDeferred<Unit>()
    private val segments = mutableListOf<RootSegment>()
    private val segmentLock = Any()

    private var sessionDir: File? = null
    private var micFile: File? = null
    private var recorder: MediaRecorder? = null
    private var segmentJob: Job? = null
    private var timelineStartElapsedMs = 0L
    private var audioStartElapsedMs = 0L
    private var recorderStarted = false
    private var failure: Throwable? = null

    @Volatile
    var warning: String? = null
        private set

    suspend fun start() = withContext(Dispatchers.IO) {
        when (val root = RootChecker.check(context, force = false)) {
            RootStatus.Available -> Unit
            is RootStatus.Unavailable -> {
                val message = when (root.reason) {
                    RootFailure.NO_SU -> context.getString(R.string.error_root_unavailable)
                    RootFailure.PROMPT_TIMEOUT -> context.getString(R.string.error_root_timeout)
                    RootFailure.NO_SCREENRECORD -> context.getString(R.string.error_root_screenrecord_missing)
                }
                throw IllegalStateException(message)
            }

            else -> throw IllegalStateException(context.getString(R.string.error_root_unavailable))
        }

        val size = resolveCaptureSize(context, settings.resolution)
        val bitRate = settings.bitrate.bitsPerSecond(
            width = size.width,
            height = size.height,
            frameRate = settings.frameRate.value,
        )

        val directory = com.hihi.recorder.data.RecordingStorage.newSessionDirectory(context)
        sessionDir = directory
        val script = File(directory, "record_segment.sh").apply {
            writeText(SEGMENT_SCRIPT)
        }

        if (settings.usesAudio) {
            startMicrophone(directory)
        }
        timelineStartElapsedMs = if (recorderStarted) audioStartElapsedMs else 0L

        segmentJob = scope.launch {
            segmentLoop(
                directory = directory,
                script = script,
                size = size,
                bitRate = bitRate,
            )
        }

        try {
            withTimeoutOrNull(25_000L) { firstSegmentReady.await() }
                ?: throw IllegalStateException(context.getString(R.string.error_root_start_timeout))
        } catch (error: Throwable) {
            abort()
            throw error
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        stopping.set(true)
        signalCurrentProcess()

        segmentJob?.let { job ->
            withTimeoutOrNull(20_000L) { job.join() }
            if (job.isActive) {
                currentProcess.getAndSet(null)?.destroyForcibly()
                job.cancel()
                // Give the loop a moment to observe the destroyed process and
                // leave the session directory before it is deleted below.
                withTimeoutOrNull(3_000L) { job.join() }
            }
        }
        segmentJob = null

        stopMicrophone()

        val recorded = synchronized(segmentLock) { segments.toList() }
        if (recorded.isEmpty()) {
            val error = failure
            cleanup()
            if (error != null) throw error
            throw IllegalStateException(context.getString(R.string.error_root_no_video))
        }

        val audio = micFile?.takeIf { it.isFile && it.length() > 0L }
        try {
            RootMuxer.combine(
                segments = recorded,
                audioFile = audio,
                outputFile = outputFile,
                timelineStartElapsedMs = timelineStartElapsedMs,
            )
        } catch (error: Throwable) {
            cleanup()
            throw error
        }

        cleanup()
        if (!outputFile.isFile || outputFile.length() <= 0L) {
            throw IllegalStateException(context.getString(R.string.error_empty_recording))
        }
    }

    fun abort() {
        stopping.set(true)
        signalCurrentProcess()
        currentProcess.getAndSet(null)?.destroyForcibly()
        segmentJob?.cancel()
        segmentJob = null
        stopMicrophone(deleteFile = true)
        cleanup()
        runCatching { outputFile.delete() }
    }

    private suspend fun segmentLoop(
        directory: File,
        script: File,
        size: CaptureSize,
        bitRate: Int,
    ) {
        var index = 0
        try {
            while (!stopping.get()) {
                val file = File(directory, String.format(Locale.US, "segment_%03d.mp4", index))
                val errorFile = File(directory, String.format(Locale.US, "segment_%03d.err", index))
                file.delete()
                errorFile.delete()

                val segmentStartedAt = SystemClock.elapsedRealtime()
                if (timelineStartElapsedMs == 0L) {
                    timelineStartElapsedMs = segmentStartedAt
                }

                val command = "sh ${RootShell.quote(script.absolutePath)} " +
                    "${RootShell.quote(file.absolutePath)} " +
                    "${RootShell.quote(size.spec)} " +
                    RootShell.quote(bitRate.toString())
                val process = ProcessBuilder("su", "-c", command).start()
                currentProcess.set(process)

                val pidSignal = CompletableDeferred<Int>()
                val errorText = StringBuilder()
                scope.launch {
                    runCatching {
                        process.inputStream.bufferedReader().useLines { lines ->
                            for (line in lines) {
                                if (line.startsWith(PID_PREFIX)) {
                                    val pid = line.removePrefix(PID_PREFIX).trim().toIntOrNull()
                                    if (pid != null && !pidSignal.isCompleted) pidSignal.complete(pid)
                                }
                            }
                        }
                    }
                }
                scope.launch {
                    runCatching {
                        process.errorStream.bufferedReader().useLines { lines ->
                            for (line in lines) {
                                synchronized(errorText) {
                                    if (errorText.length < 8_000) {
                                        errorText.append(line).append('\n')
                                    }
                                }
                            }
                        }
                    }
                }

                val pid = withTimeoutOrNull(15_000L) { pidSignal.await() }
                if (pid == null || pid <= 0) {
                    process.destroyForcibly()
                    throw IllegalStateException(context.getString(R.string.error_root_start_failed))
                }
                currentPid.set(pid)

                if (index == 0 && !firstSegmentReady.isCompleted) {
                    firstSegmentReady.complete(Unit)
                }

                val exitCode = process.waitFor()
                currentProcess.compareAndSet(process, null)
                currentPid.set(0)

                RootShell.run(
                    "chmod 666 ${RootShell.quote(file.absolutePath)} " +
                        RootShell.quote(errorFile.absolutePath),
                    timeoutMs = 5_000L,
                )

                val elapsed = SystemClock.elapsedRealtime() - segmentStartedAt
                val valid = file.isFile && file.length() > 0L
                if (valid) {
                    synchronized(segmentLock) {
                        segments += RootSegment(file, segmentStartedAt)
                    }
                }

                if (stopping.get()) break

                if (exitCode != 0 || !valid) {
                    val detail = synchronized(errorText) { errorText.toString().trim() }
                    val fileDetail = runCatching {
                        errorFile.takeIf { it.isFile }?.readText()?.trim()
                    }.getOrNull().orEmpty()
                    val message = listOf(detail, fileDetail)
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")
                        .take(1_500)
                    throw IllegalStateException(
                        if (message.isNotEmpty()) {
                            context.getString(R.string.error_root_screenrecord_failed_with_reason, message)
                        } else {
                            context.getString(R.string.error_root_screenrecord_failed)
                        },
                    )
                }
                if (elapsed < 1_500L) {
                    throw IllegalStateException(context.getString(R.string.error_root_stopped_early))
                }
                index++
            }
        } catch (error: Throwable) {
            failure = error
            if (!firstSegmentReady.isCompleted) {
                firstSegmentReady.completeExceptionally(error)
            }
            if (!stopping.get()) {
                warning = context.getString(R.string.warning_root_unexpected_stop)
                onUnexpectedStop()
            }
        }
    }

    private fun startMicrophone(directory: File) {
        if (settings.audioSource.usesDeviceAudio) {
            warning = context.getString(R.string.warning_root_mic_only)
        }
        val file = File(directory, "microphone.m4a")
        micFile = file
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder = created

        try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioSamplingRate(AUDIO_SAMPLE_RATE)
            created.setAudioEncodingBitRate(128_000)
            created.setAudioChannels(1)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
            recorderStarted = true
            audioStartElapsedMs = SystemClock.elapsedRealtime()
        } catch (error: Throwable) {
            runCatching { created.release() }
            recorder = null
            recorderStarted = false
            file.delete()
            warning = context.getString(R.string.warning_audio_unavailable)
        }
    }

    private fun stopMicrophone(deleteFile: Boolean = false) {
        val active = recorder
        recorder = null
        if (active != null) {
            if (recorderStarted) {
                runCatching { active.stop() }
            }
            runCatching { active.release() }
        }
        recorderStarted = false
        if (deleteFile) micFile?.delete()
    }

    private fun signalCurrentProcess() {
        val pid = currentPid.get()
        if (pid > 0) {
            scope.launch {
                RootShell.run("kill -2 $pid", timeoutMs = 5_000L)
            }
        } else {
            currentProcess.get()?.let { process ->
                runCatching { process.destroy() }
            }
        }
    }

    private fun cleanup() {
        synchronized(segmentLock) { segments.clear() }
        sessionDir?.deleteRecursively()
        sessionDir = null
        micFile = null
        timelineStartElapsedMs = 0L
        scope.cancel()
    }

    companion object {
        private const val PID_PREFIX = "PID:"

        private val SEGMENT_SCRIPT = """
            #!/system/bin/sh
            out="${'$'}1"
            size="${'$'}2"
            bitrate="${'$'}3"
            err="${'$'}out.err"
            screenrecord --size "${'$'}size" --bit-rate "${'$'}bitrate" --time-limit 180 "${'$'}out" 2>"${'$'}err" &
            child=${'$'}!
            echo "PID:${'$'}child"
            wait "${'$'}child"
            rc=${'$'}?
            chmod 666 "${'$'}out" "${'$'}err" 2>/dev/null
            exit ${'$'}rc
        """.trimIndent() + "\n"
    }
}
