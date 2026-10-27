package com.hihi.recorder.media

import android.content.Context
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
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
 * without MediaProjection consent. Audio is captured through the same
 * [PcmAudioSource] + [AacEncoder] pipeline as standard mode, so microphone,
 * phone audio and the mix all work. The audio start timestamp is the timeline
 * origin; every video segment is offset against it when the files are joined,
 * keeping picture and sound aligned in wall-clock time.
 */
class RootRecorder(
    private val context: Context,
    private val settings: RecorderSettings,
    private val outputFile: File,
    private val projection: MediaProjection?,
    private val onProjectionStopped: () -> Unit,
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
    private var audioFile: File? = null
    private var audioCapture: AudioCapture? = null
    private var audioMuxer: MuxerController? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var segmentJob: Job? = null
    private var timelineStartElapsedMs = 0L
    private var audioStartElapsedMs = 0L
    private var audioStarted = false
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
            startAudio(directory)
        }
        timelineStartElapsedMs = if (audioStarted) audioStartElapsedMs else 0L

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

        // Stop the soundtrack as soon as the user stops. The video process may
        // need a moment to finalize its moov atom; recording extra audio after
        // the stop would only add an unrelated tail to the file.
        stopAudio()

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

        val recorded = synchronized(segmentLock) { segments.toList() }
        if (recorded.isEmpty()) {
            val error = failure
            cleanup()
            if (error != null) throw error
            throw IllegalStateException(context.getString(R.string.error_root_no_video))
        }

        val audio = audioFile?.takeIf { it.isFile && it.length() > 0L }
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
        runCatching { audioCapture?.release() }
        audioCapture = null
        runCatching { audioMuxer?.abort() }
        audioMuxer = null
        stopProjection()
        audioFile?.delete()
        audioFile = null
        audioStarted = false
        cleanup()
        runCatching { outputFile.delete() }
    }

    private fun startAudio(directory: File) {
        try {
            startAudioInternal(directory)
        } catch (_: Throwable) {
            // Audio is optional in root mode: the screenrecord video must still
            // be recorded when the projector or encoder cannot be opened.
            warning = context.getString(
                if (settings.audioSource.usesDeviceAudio) {
                    R.string.warning_internal_audio_unavailable
                } else {
                    R.string.warning_audio_unavailable
                },
            )
            runCatching { audioCapture?.release() }
            audioCapture = null
            runCatching { audioMuxer?.abort() }
            audioMuxer = null
            stopProjection()
            audioFile?.delete()
            audioFile = null
            audioStarted = false
        }
    }

    private fun startAudioInternal(directory: File) {
        val needsProjection = settings.audioSource.usesDeviceAudio
        val activeProjection = projection
        if (needsProjection) {
            if (activeProjection == null) {
                warning = context.getString(R.string.warning_internal_audio_unavailable)
                return
            }
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    // Ignore the stop we initiate ourselves, either while
                    // tearing down or when playback capture failed to start.
                    if (stopping.get() || !audioStarted) return
                    onProjectionStopped()
                }
            }
            val registered = runCatching {
                // Android 14 requires a callback before the projection is used.
                activeProjection.registerCallback(callback, Handler(Looper.getMainLooper()))
            }.isSuccess
            if (!registered) {
                warning = context.getString(R.string.warning_internal_audio_unavailable)
                return
            }
            projectionCallback = callback
        }

        val file = File(directory, "audio.m4a")
        val muxer = MuxerController(file, expectVideo = false)
        muxer.setExpectAudio(true)
        val capture = AudioCapture(
            context = context,
            audioSource = settings.audioSource,
            projection = activeProjection,
            muxer = muxer,
        )
        if (!capture.start()) {
            warning = capture.warning
            runCatching { muxer.abort() }
            file.delete()
            stopProjection()
            return
        }

        audioCapture = capture
        audioMuxer = muxer
        audioFile = file
        audioStarted = true
        audioStartElapsedMs = capture.startedAtElapsedMs
        warning = capture.warning
    }

    private fun stopAudio() {
        val capture = audioCapture
        val muxer = audioMuxer
        audioCapture = null
        audioMuxer = null

        if (capture != null) {
            val error = capture.stop()
            if (error != null) {
                warning = context.getString(R.string.warning_audio_stopped_video_saved)
            }
        }
        if (muxer != null) {
            runCatching { muxer.stop() }.onFailure {
                warning = context.getString(R.string.warning_audio_stopped_video_saved)
            }
        }

        audioStarted = false
        stopProjection()
    }

    private fun stopProjection() {
        val active = projection
        projectionCallback?.let { callback ->
            runCatching { active?.unregisterCallback(callback) }
        }
        projectionCallback = null
        runCatching { active?.stop() }
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
                val segmentEndedAt = SystemClock.elapsedRealtime()
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
                        segments += RootSegment(file, segmentStartedAt, segmentEndedAt)
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
        runCatching { audioCapture?.release() }
        audioCapture = null
        runCatching { audioMuxer?.abort() }
        audioMuxer = null
        stopProjection()

        synchronized(segmentLock) { segments.clear() }
        sessionDir?.deleteRecursively()
        sessionDir = null
        audioFile = null
        timelineStartElapsedMs = 0L
        audioStarted = false
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
