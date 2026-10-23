package com.hihi.recorder.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Reads PCM from an [PcmAudioSource] and encodes it to AAC-LC into the shared
 * muxer. The source owns its own blocking reads, so this class is also used by
 * the root recorder when only microphone audio is needed.
 */
class AacEncoder(
    private val source: PcmAudioSource,
    private val muxer: MuxerController,
) {
    private var codec: MediaCodec? = null
    private var thread: Thread? = null
    private val stopping = AtomicBoolean(false)
    private val stopped = CountDownLatch(1)
    private val failure = AtomicReference<Throwable?>(null)

    val error: Throwable? get() = failure.get()

    fun start() {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            AUDIO_SAMPLE_RATE,
            AUDIO_CHANNELS,
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }

        val created = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec = created
        try {
            created.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            created.start()
            thread = Thread({ encodeLoop() }, "MiuixAudioEncode").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (error: Throwable) {
            releaseCodec()
            throw error
        }
    }

    fun stopAndAwait(timeoutMs: Long = 8_000L) {
        stopping.set(true)
        source.stop()
        thread?.let { runCatching { it.join(timeoutMs) } }
        source.close()
        if (!stopped.await(1, TimeUnit.SECONDS)) {
            releaseCodec()
        }
    }

    fun release() {
        stopping.set(true)
        source.stop()
        source.close()
        thread?.let { runCatching { it.join(1_000L) } }
        releaseCodec()
    }

    private fun encodeLoop() {
        val localCodec = codec ?: return
        var totalFrames = 0L
        var eosQueued = false
        val pcm = ShortArray(2_048)
        val info = MediaCodec.BufferInfo()

        try {
            while (!eosQueued) {
                drainOutput(localCodec, info, allowBlocking = false)

                val inputIndex = localCodec.dequeueInputBuffer(10_000L)
                if (inputIndex < 0) continue

                if (stopping.get()) {
                    localCodec.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        presentationTimeUs(totalFrames),
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    )
                    eosQueued = true
                    continue
                }

                val frames = source.read(pcm, 0, pcm.size)
                val input = localCodec.getInputBuffer(inputIndex) ?: continue
                input.clear()

                if (frames > 0) {
                    input.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm, 0, frames)
                    localCodec.queueInputBuffer(
                        inputIndex,
                        0,
                        frames * 2,
                        presentationTimeUs(totalFrames),
                        0,
                    )
                    totalFrames += frames
                } else if (frames < 0 || stopping.get()) {
                    localCodec.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        presentationTimeUs(totalFrames),
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    )
                    eosQueued = true
                } else {
                    localCodec.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        presentationTimeUs(totalFrames),
                        0,
                    )
                }
            }

            var sawEnd = false
            while (!sawEnd) {
                sawEnd = drainOutput(localCodec, info, allowBlocking = true)
            }
        } catch (error: Throwable) {
            failure.set(error)
            // If audio dies before the muxer has started, unblock the video
            // track so the screen recording can still be saved without sound.
            runCatching { muxer.setExpectAudio(false) }
        } finally {
            releaseCodec()
            stopped.countDown()
        }
    }

    /** Returns true when the encoder emitted its end-of-stream buffer. */
    private fun drainOutput(
        localCodec: MediaCodec,
        info: MediaCodec.BufferInfo,
        allowBlocking: Boolean,
    ): Boolean {
        while (true) {
            val outputIndex = localCodec.dequeueOutputBuffer(
                info,
                if (allowBlocking) 10_000L else 0L,
            )
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    muxer.setAudioFormat(localCodec.outputFormat)
                }

                outputIndex >= 0 -> {
                    val output = localCodec.getOutputBuffer(outputIndex)
                    if (output != null &&
                        info.size > 0 &&
                        info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                    ) {
                        muxer.writeAudio(info, output)
                    }
                    val end = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    localCodec.releaseOutputBuffer(outputIndex, false)
                    if (end) return true
                }

                else -> return false
            }
        }
    }

    private fun presentationTimeUs(frames: Long): Long =
        frames * 1_000_000L / AUDIO_SAMPLE_RATE

    private fun releaseCodec() {
        codec?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        codec = null
    }
}
