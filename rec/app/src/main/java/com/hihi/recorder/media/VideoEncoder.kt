package com.hihi.recorder.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Surface
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int,
    private val bitRate: Int,
    private val muxer: MuxerController,
) {
    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    private var drainThread: Thread? = null
    private val stopped = CountDownLatch(1)
    private val failure = AtomicReference<Throwable?>(null)

    fun start(): Surface {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoderName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format)
        val created = if (encoderName != null) {
            MediaCodec.createByCodecName(encoderName)
        } else {
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        }
        codec = created

        runCatching {
            val capabilities = created.codecInfo.encoderCapabilities
            if (capabilities != null && capabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            }
        }

        try {
            created.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val input = created.createInputSurface()
            surface = input
            created.start()
            drainThread = Thread({ drain() }, "MiuixVideoDrain").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            return input
        } catch (error: Throwable) {
            releaseCodec()
            throw error
        }
    }

    fun signalEndOfStream() {
        runCatching { codec?.signalEndOfInputStream() }
    }

    fun awaitStopped(timeoutMs: Long = 10_000L): Boolean =
        stopped.await(timeoutMs, TimeUnit.MILLISECONDS)

    val error: Throwable? get() = failure.get()

    fun release() {
        surface?.let { runCatching { it.release() } }
        surface = null
        releaseCodec()
    }

    private fun releaseCodec() {
        codec?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        codec = null
    }

    private fun drain() {
        val localCodec = codec ?: return
        try {
            val info = MediaCodec.BufferInfo()
            var sawEnd = false
            while (!sawEnd) {
                when (val index = localCodec.dequeueOutputBuffer(info, 10_000L)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxer.setVideoFormat(localCodec.outputFormat)
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (index >= 0) {
                        val buffer = localCodec.getOutputBuffer(index)
                        if (buffer != null && info.size > 0 &&
                            info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            muxer.writeVideo(info, buffer)
                        }
                        sawEnd = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        localCodec.releaseOutputBuffer(index, false)
                    }
                }
            }
        } catch (error: Throwable) {
            failure.set(error)
        } finally {
            stopped.countDown()
        }
    }
}
