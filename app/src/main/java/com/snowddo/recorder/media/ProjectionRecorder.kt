package com.snowddo.recorder.media

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import com.snowddo.recorder.R
import com.snowddo.recorder.data.AudioSource
import com.snowddo.recorder.data.RecorderSettings
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Standard, rootless recorder. Screen frames go through MediaProjection into a
 * surface-backed H.264 encoder; optional audio is captured with AudioRecord and
 * encoded to AAC. MediaMuxer interleaves both tracks into one MP4.
 */
class ProjectionRecorder(
    private val context: Context,
    private val settings: RecorderSettings,
    private val outputFile: File,
    private val projection: MediaProjection,
    private val onProjectionStopped: () -> Unit,
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var muxer: MuxerController? = null
    private var videoEncoder: VideoEncoder? = null
    private var audioSource: PcmAudioSource? = null
    private var audioEncoder: AacEncoder? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private val stopping = AtomicBoolean(false)
    private val started = AtomicBoolean(false)

    var warning: String? = null
        private set

    fun start() {
        val size = resolveCaptureSize(context, settings.resolution)
        val frameRate = settings.frameRate.value
        val bitRate = settings.bitrate.bitsPerSecond(size.width, size.height, frameRate)

        val createdMuxer = MuxerController(outputFile)
        muxer = createdMuxer
        createdMuxer.setExpectAudio(settings.usesAudio)

        try {
            val video = VideoEncoder(
                width = size.width,
                height = size.height,
                frameRate = frameRate,
                bitRate = bitRate,
                muxer = createdMuxer,
            )
            videoEncoder = video
            val surface = video.start()

            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    if (stopping.get()) return
                    onProjectionStopped()
                }
            }
            projectionCallback = callback
            // Android 14 requires the callback to be registered before the
            // first virtual display is created.
            projection.registerCallback(callback, Handler(Looper.getMainLooper()))

            virtualDisplay = projection.createVirtualDisplay(
                "MiuixRecorder",
                size.width,
                size.height,
                resolveDisplayDensity(context),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null,
            )

            if (settings.usesAudio) {
                startAudio(createdMuxer)
            }
            started.set(true)
        } catch (error: Throwable) {
            abort()
            throw error
        }
    }

    private fun startAudio(createdMuxer: MuxerController) {
        val candidates = audioCandidates()
        for (candidate in candidates) {
            val source = candidate.source
            if (runCatching { source.start() }.isFailure) {
                source.close()
                continue
            }

            try {
                val encoder = AacEncoder(source, createdMuxer)
                encoder.start()
                audioSource = source
                audioEncoder = encoder
                warning = candidate.warning
                return
            } catch (error: Throwable) {
                runCatching { source.close() }
            }
        }

        warning = when {
            settings.audioSource.usesDeviceAudio ->
                context.getString(R.string.warning_internal_audio_unavailable)

            else -> context.getString(R.string.warning_audio_unavailable)
        }
        createdMuxer.setExpectAudio(false)
    }

    private data class AudioCandidate(
        val source: PcmAudioSource,
        val warning: String?,
    )

    private fun audioCandidates(): List<AudioCandidate> {
        val fallbackWarning = context.getString(R.string.warning_mixed_audio_fallback)
        return when (settings.audioSource) {
            AudioSource.NONE -> emptyList()

            AudioSource.MICROPHONE -> listOf(
                AudioCandidate(MicrophoneAudioSource(preferVoiceCommunication = false), null),
            )

            AudioSource.DEVICE -> listOf(
                AudioCandidate(PlaybackAudioSource(projection), null),
            )

            AudioSource.MIC_AND_DEVICE -> listOf(
                AudioCandidate(
                    MixedAudioSource(
                        MicrophoneAudioSource(preferVoiceCommunication = true),
                        PlaybackAudioSource(projection),
                    ),
                    null,
                ),
                AudioCandidate(PlaybackAudioSource(projection), fallbackWarning),
                AudioCandidate(
                    MicrophoneAudioSource(preferVoiceCommunication = true),
                    fallbackWarning,
                ),
            )
        }
    }

    /**
     * Stops the virtual display and encoders and finalizes the muxer. Returns
     * normally only when the MP4 is ready to be published.
     */
    fun stop() {
        if (!stopping.compareAndSet(false, true)) return
        val errors = mutableListOf<Throwable>()

        runCatching { virtualDisplay?.release() }
            .onFailure { errors += it }
        virtualDisplay = null

        runCatching { videoEncoder?.signalEndOfStream() }
            .onFailure { errors += it }

        runCatching { audioEncoder?.stopAndAwait() }
            .onFailure { errors += it }

        val videoStopped = runCatching {
            videoEncoder?.awaitStopped(10_000L) ?: true
        }.getOrDefault(false)
        if (!videoStopped) {
            errors += IllegalStateException(context.getString(R.string.error_video_drain_timeout))
        }
        videoEncoder?.error?.let { errors += it }
        // Losing the audio encoder must not throw away a healthy video file.
        // If the muxer itself was affected, its stop() below still reports it.
        audioEncoder?.error?.let {
            warning = context.getString(R.string.warning_audio_stopped_video_saved)
        }

        runCatching { videoEncoder?.release() }
            .onFailure { errors += it }
        audioEncoder?.release()
        audioEncoder = null
        audioSource = null

        val activeMuxer = muxer
        muxer = null
        activeMuxer?.let {
            runCatching { it.stop() }.onFailure { error -> errors += error }
        }

        projectionCallback?.let { callback ->
            runCatching { projection.unregisterCallback(callback) }
        }
        projectionCallback = null
        runCatching { projection.stop() }

        started.set(false)

        val failure = errors.firstOrNull()
        if (failure != null) {
            throw failure
        }
        if (!outputFile.isFile || outputFile.length() <= 0L) {
            throw IllegalStateException(context.getString(R.string.error_empty_recording))
        }
    }

    /** Releases everything without finalizing the output. */
    fun abort() {
        stopping.set(true)
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { videoEncoder?.signalEndOfStream() }
        runCatching { videoEncoder?.awaitStopped(2_000L) }
        runCatching { videoEncoder?.release() }
        videoEncoder = null
        runCatching { audioEncoder?.release() }
        audioEncoder = null
        runCatching { audioSource?.close() }
        audioSource = null
        projectionCallback?.let { callback ->
            runCatching { projection.unregisterCallback(callback) }
        }
        projectionCallback = null
        runCatching { projection.stop() }
        runCatching { muxer?.abort() }
        muxer = null
        started.set(false)
        runCatching { outputFile.delete() }
    }
}
