package com.hihi.recorder.media

import android.content.Context
import android.media.projection.MediaProjection
import android.os.SystemClock
import com.hihi.recorder.R
import com.hihi.recorder.data.AudioSource

/**
 * Shared audio pipeline for both capture modes. It opens the best available
 * [PcmAudioSource] for the requested [AudioSource], feeds it to [AacEncoder]
 * and writes the AAC track into the caller's muxer.
 *
 * Standard mode uses it next to the video encoder. Root mode uses it with an
 * audio-only muxer, which is why the muxer stays owned by the caller.
 */
internal class AudioCapture(
    private val context: Context,
    private val audioSource: AudioSource,
    private val projection: MediaProjection?,
    private val muxer: MuxerController,
) {
    private var source: PcmAudioSource? = null
    private var encoder: AacEncoder? = null

    /** Warning produced during setup, or null when the requested source worked. */
    var warning: String? = null
        private set

    /**
     * Elapsed-realtime timestamp of the first PCM frame requested from the
     * source. Root mode uses it as the origin of the audio timeline.
     */
    var startedAtElapsedMs: Long = 0L
        private set

    /** Starts the first working source. Returns false when audio is unavailable. */
    fun start(): Boolean {
        try {
            for (candidate in audioCandidates()) {
                val candidateSource = candidate.source
                if (runCatching { candidateSource.start() }.isFailure) {
                    candidateSource.close()
                    continue
                }
                // AudioRecord starts filling its buffer as soon as start()
                // returns, so this is the wall-clock origin of sample 0.
                startedAtElapsedMs = SystemClock.elapsedRealtime()

                try {
                    val created = AacEncoder(candidateSource, muxer)
                    created.start()
                    source = candidateSource
                    encoder = created
                    warning = candidate.warning
                    return true
                } catch (_: Throwable) {
                    runCatching { candidateSource.close() }
                }
            }
        } catch (_: Throwable) {
            // Building a capture configuration can fail before a candidate is
            // even created. Audio is optional, so fall through to the warning.
        }

        warning = when {
            audioSource.usesDeviceAudio ->
                context.getString(R.string.warning_internal_audio_unavailable)

            else -> context.getString(R.string.warning_audio_unavailable)
        }
        muxer.setExpectAudio(false)
        return false
    }

    /**
     * Stops the source, drains the encoder and returns the first failure. The
     * caller still owns [muxer] and must stop it afterwards.
     */
    fun stop(): Throwable? {
        val active = encoder ?: return null
        encoder = null
        source = null
        val stopError = runCatching { active.stopAndAwait() }.exceptionOrNull()
        return stopError ?: active.error
    }

    /** Releases the encoder without finalizing it (used on aborted sessions). */
    fun release() {
        val active = encoder
        encoder = null
        source = null
        runCatching { active?.release() }
    }

    private data class Candidate(
        val source: PcmAudioSource,
        val warning: String?,
    )

    private fun audioCandidates(): List<Candidate> {
        val fallbackWarning = context.getString(R.string.warning_mixed_audio_fallback)
        return when (audioSource) {
            AudioSource.NONE -> emptyList()

            AudioSource.MICROPHONE -> listOf(
                Candidate(MicrophoneAudioSource(preferVoiceCommunication = false), null),
            )

            AudioSource.DEVICE -> projection?.let { activeProjection ->
                listOf(Candidate(PlaybackAudioSource(activeProjection), null))
            }.orEmpty()

            AudioSource.MIC_AND_DEVICE -> if (projection != null) {
                listOf(
                    Candidate(
                        MixedAudioSource(
                            MicrophoneAudioSource(preferVoiceCommunication = true),
                            PlaybackAudioSource(projection),
                        ),
                        null,
                    ),
                    Candidate(PlaybackAudioSource(projection), fallbackWarning),
                    Candidate(
                        MicrophoneAudioSource(preferVoiceCommunication = true),
                        fallbackWarning,
                    ),
                )
            } else {
                // Root mode without a projection token can still record the
                // microphone, but the UI requests consent before this happens.
                listOf(
                    Candidate(
                        MicrophoneAudioSource(preferVoiceCommunication = true),
                        fallbackWarning,
                    ),
                )
            }
        }
    }
}
