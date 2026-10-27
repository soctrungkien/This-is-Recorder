package com.hihi.recorder.media

import android.content.Context
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.hihi.recorder.R
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecordingStorage
import com.hihi.recorder.shell.ShellException
import com.hihi.recorder.shell.ShellSessions
import com.hihi.recorder.shell.message
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

/**
 * Shell recording engine. `screenrecord` captures video in 180 second chunks
 * through ADB, Shizuku or root without a video consent prompt. Audio is
 * captured through the same [PcmAudioSource] + [AacEncoder] pipeline as
 * standard mode, so microphone, phone audio and the mix all work. The audio
 * start timestamp is the timeline origin; every video segment is offset
 * against it when the files are joined, keeping picture and sound aligned in
 * wall-clock time.
 */
class ShellRecorder(
    private val context: Context,
    private val settings: RecorderSettings,
    private val outputFile: File,
    private val projection: MediaProjection?,
    private val onProjectionStopped: () -> Unit,
    private val onUnexpectedStop: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stopping = AtomicBoolean(false)
    private val firstSegmentReady = CompletableDeferred<Unit>()
    private val segments = mutableListOf<ShellSegment>()
    private val segmentLock = Any()
    private val session = ShellSessions.create(context, settings.shellBackend)

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
    private var videoActive = false

    @Volatile
    var warning: String? = null
        private set

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            session.open()
        } catch (error: ShellException) {
            throw IllegalStateException(error.failure.message(context), error)
        }

        val size = resolveCaptureSize(context, settings.resolution)
        val bitRate = settings.bitrate.bitsPerSecond(
            width = size.width,
            height = size.height,
            frameRate = settings.frameRate.value,
        )

        val directory = RecordingStorage.newShellSessionDirectory(context)
        sessionDir = directory

        if (settings.usesAudio) {
            startAudio(directory)
        }
        timelineStartElapsedMs = if (audioStarted) audioStartElapsedMs else 0L

        segmentJob = scope.launch {
            segmentLoop(
                directory = directory,
                size = size,
                bitRate = bitRate,
            )
        }

        try {
            withTimeoutOrNull(25_000L) { firstSegmentReady.await() }
                ?: throw IllegalStateException(context.getString(R.string.error_shell_start_timeout))
        } catch (error: Throwable) {
            abort()
            throw error
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        stopping.set(true)
        // A stop that arrives while startVideo() is still handshaking is
        // handled by segmentLoop right after the segment becomes active.
        if (videoActive) {
            runCatching { session.stopVideo() }
        }

        // Stop the soundtrack as soon as the user stops. The video process may
        // need a moment to finalize its moov atom; recording extra audio after
        // the stop would only add an unrelated tail to the file.
        stopAudio()

        segmentJob?.let { job ->
            withTimeoutOrNull(20_000L) { job.join() }
            if (job.isActive) {
                runCatching { session.close() }
                job.cancel()
                // Give the loop a moment to observe the closed session and
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
            throw IllegalStateException(context.getString(R.string.error_shell_no_video))
        }

        val audio = audioFile?.takeIf { it.isFile && it.length() > 0L }
        try {
            ShellMuxer.combine(
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
        videoActive = false
        runCatching { session.close() }
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
            // Audio is optional in shell mode: the screenrecord video must
            // still be recorded when the projector or encoder cannot open.
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
        size: CaptureSize,
        bitRate: Int,
    ) {
        var index = 0
        try {
            while (!stopping.get()) {
                val file = File(directory, String.format(Locale.US, "segment_%03d.mp4", index))
                file.delete()

                val segmentStartedAt = session.startVideo(
                    file = file,
                    size = size.spec,
                    bitRate = bitRate,
                    timeLimitSeconds = SEGMENT_SECONDS,
                )
                videoActive = true
                if (stopping.get()) {
                    runCatching { session.stopVideo() }
                }

                if (index == 0 && !firstSegmentReady.isCompleted) {
                    firstSegmentReady.complete(Unit)
                }

                val end = try {
                    session.awaitVideoEnd()
                } finally {
                    videoActive = false
                }
                val valid = file.isFile && file.length() > 0L
                if (valid) {
                    synchronized(segmentLock) {
                        segments += ShellSegment(
                            file = file,
                            startElapsedMs = segmentStartedAt,
                            endElapsedMs = end.endedAtElapsedMs,
                        )
                    }
                }

                if (stopping.get()) break

                if (end.exitCode != 0 || !valid) {
                    val message = end.error.trim().take(1_500)
                    throw IllegalStateException(
                        if (message.isNotEmpty()) {
                            context.getString(
                                R.string.error_shell_screenrecord_failed_with_reason,
                                message,
                            )
                        } else {
                            context.getString(R.string.error_shell_screenrecord_failed)
                        },
                    )
                }
                if (end.endedAtElapsedMs - segmentStartedAt < 1_500L) {
                    throw IllegalStateException(context.getString(R.string.error_shell_stopped_early))
                }
                index++
            }
        } catch (error: Throwable) {
            failure = error
            if (!firstSegmentReady.isCompleted) {
                firstSegmentReady.completeExceptionally(error)
            }
            if (!stopping.get()) {
                warning = context.getString(R.string.warning_shell_unexpected_stop)
                onUnexpectedStop()
            }
        }
    }

    private fun cleanup() {
        videoActive = false
        runCatching { audioCapture?.release() }
        audioCapture = null
        runCatching { audioMuxer?.abort() }
        audioMuxer = null
        stopProjection()
        runCatching { session.close() }

        synchronized(segmentLock) { segments.clear() }
        sessionDir?.deleteRecursively()
        sessionDir = null
        audioFile = null
        timelineStartElapsedMs = 0L
        audioStarted = false
        scope.cancel()
    }

    private companion object {
        const val SEGMENT_SECONDS = 180
    }
}
