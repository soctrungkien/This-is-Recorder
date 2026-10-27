package com.hihi.recorder.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer

data class ShellSegment(
    val file: File,
    val startElapsedMs: Long,
    val endElapsedMs: Long,
)

/**
 * Joins the short screenrecord segments produced by shell mode into one MP4
 * and optionally adds the microphone/phone-audio track. Video gaps between
 * segments are kept so the continuous audio track stays in sync with
 * wall-clock time.
 *
 * The start of a segment is derived from its end time and its encoded duration
 * rather than from the moment the shell process was spawned. `su`, Shizuku,
 * the ADB daemon and MediaCodec initialization add a variable delay before
 * `screenrecord` captures its first frame, and using the spawn time would make
 * the picture lead the sound by that delay.
 */
object ShellMuxer {

    private const val SAMPLE_BUFFER_SIZE = 8 * 1024 * 1024

    fun combine(
        segments: List<ShellSegment>,
        audioFile: File?,
        outputFile: File,
        timelineStartElapsedMs: Long,
    ) {
        val valid = validate(segments, timelineStartElapsedMs)
        if (valid.isEmpty()) {
            throw IllegalStateException("Không có đoạn video hợp lệ")
        }

        outputFile.parentFile?.mkdirs()
        runCatching { outputFile.delete() }

        val audioUsable = audioFile != null && audioFile.isFile && audioFile.length() > 0
        try {
            combineInternal(valid, audioFile.takeIf { audioUsable }, outputFile)
        } catch (firstError: Throwable) {
            runCatching { outputFile.delete() }
            if (audioFile != null) {
                // A broken audio track must not cost the user the whole video.
                try {
                    combineInternal(valid, null, outputFile)
                    return
                } catch (_: Throwable) {
                    runCatching { outputFile.delete() }
                }
            }
            throw firstError
        }
    }

    private data class ValidSegment(
        val segment: ShellSegment,
        val format: MediaFormat,
        val offsetUs: Long,
        val width: Int,
        val height: Int,
    )

    private data class MeasuredSegment(
        val segment: ShellSegment,
        val format: MediaFormat,
        val startElapsedMs: Long,
        val width: Int,
        val height: Int,
    )

    private fun validate(
        segments: List<ShellSegment>,
        timelineStartElapsedMs: Long,
    ): List<ValidSegment> {
        val measured = mutableListOf<MeasuredSegment>()
        for (segment in segments) {
            if (!segment.file.isFile || segment.file.length() <= 0L) continue
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(segment.file.absolutePath)
                val track = findTrack(extractor, "video/")
                if (track < 0) continue
                extractor.selectTrack(track)
                val format = extractor.getTrackFormat(track)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue
                if (!format.containsKey(MediaFormat.KEY_WIDTH) ||
                    !format.containsKey(MediaFormat.KEY_HEIGHT)
                ) {
                    continue
                }
                val width = format.getInteger(MediaFormat.KEY_WIDTH)
                val height = format.getInteger(MediaFormat.KEY_HEIGHT)
                if (width <= 0 || height <= 0) continue

                if (measured.isNotEmpty()) {
                    val first = measured.first()
                    if (first.width != width || first.height != height) continue
                }

                // Prefer end-minus-duration: it ignores the process startup
                // delay and therefore matches the first captured frame. Fall
                // back to the spawn timestamp if the track has no duration.
                val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    format.getLong(MediaFormat.KEY_DURATION)
                } else {
                    0L
                }
                val startElapsedMs = if (durationUs > 0L) {
                    segment.endElapsedMs - durationUs / 1_000L
                } else {
                    segment.startElapsedMs
                }

                measured += MeasuredSegment(
                    segment = segment,
                    format = format,
                    startElapsedMs = startElapsedMs,
                    width = width,
                    height = height,
                )
            } catch (_: Throwable) {
                // Skip a corrupt segment and keep the rest of the recording.
            } finally {
                runCatching { extractor.release() }
            }
        }

        if (measured.isEmpty()) return emptyList()
        // With audio, the caller passes the first audio sample's wall-clock
        // time. Without audio, the first segment defines the timeline origin,
        // so the video starts at zero instead of after a black gap.
        val originElapsedMs = if (timelineStartElapsedMs > 0L) {
            timelineStartElapsedMs
        } else {
            measured.first().startElapsedMs
        }

        return measured.map { item ->
            ValidSegment(
                segment = item.segment,
                format = item.format,
                offsetUs = ((item.startElapsedMs - originElapsedMs) * 1_000L)
                    .coerceAtLeast(0L),
                width = item.width,
                height = item.height,
            )
        }
    }

    private fun combineInternal(
        segments: List<ValidSegment>,
        audioFile: File?,
        outputFile: File,
    ) {
        val videoFormat = segments.first().format
        val videoCursor = SegmentVideoCursor(segments)
        var audioCursor: AudioSampleCursor? = null
        var muxer: MediaMuxer? = null
        try {
            audioCursor = audioFile?.let { AudioSampleCursor(it) }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val rotation = if (videoFormat.containsKey("rotation-degrees")) {
                videoFormat.getInteger("rotation-degrees")
            } else {
                0
            }
            if (rotation != 0) muxer.setOrientationHint(rotation)

            val videoTrack = muxer.addTrack(videoFormat)
            val audioFormat = audioCursor?.format
            val audioTrack = if (audioFormat != null) muxer.addTrack(audioFormat) else -1
            muxer.start()

            val info = MediaCodec.BufferInfo()
            var videoAvailable = videoCursor.next()
            var audioAvailable = audioCursor?.next() == true

            while (videoAvailable || audioAvailable) {
                val writeVideo = videoAvailable &&
                    (!audioAvailable || videoCursor.ptsUs <= audioCursor!!.ptsUs)

                if (writeVideo) {
                    videoCursor.buffer.position(0)
                    info.set(0, videoCursor.size, videoCursor.ptsUs, videoCursor.flags)
                    muxer.writeSampleData(videoTrack, videoCursor.buffer, info)
                    videoAvailable = videoCursor.next()
                } else {
                    val cursor = audioCursor ?: break
                    cursor.buffer.position(0)
                    info.set(0, cursor.size, cursor.ptsUs, cursor.flags)
                    muxer.writeSampleData(audioTrack, cursor.buffer, info)
                    audioAvailable = cursor.next()
                }
            }

            muxer.stop()
        } finally {
            runCatching { videoCursor.close() }
            runCatching { audioCursor?.close() }
            if (muxer != null) {
                runCatching { muxer.release() }
            }
        }
    }

    private fun findTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith(mimePrefix)) return index
        }
        return -1
    }

    private interface SampleCursor : Closeable {
        val ptsUs: Long
        val size: Int
        val flags: Int
        val buffer: ByteBuffer
        fun next(): Boolean
    }

    private class SegmentVideoCursor(
        private val segments: List<ValidSegment>,
    ) : SampleCursor {
        override val buffer: ByteBuffer = ByteBuffer.allocateDirect(SAMPLE_BUFFER_SIZE)
        override var ptsUs: Long = 0L
            private set
        override var size: Int = 0
            private set
        override var flags: Int = 0
            private set

        private var segmentIndex = -1
        private var extractor: MediaExtractor? = null
        private var firstPtsUs = -1L
        private var offsetUs = 0L
        private var lastPtsUs = -1L

        override fun next(): Boolean {
            while (true) {
                val active = extractor ?: if (openNext()) continue else return false
                val sampleSize = active.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    closeActive()
                    continue
                }
                if (sampleSize > buffer.capacity()) {
                    throw IllegalStateException("Mẫu video quá lớn để ghép")
                }

                val samplePts = active.sampleTime
                if (firstPtsUs < 0L) firstPtsUs = samplePts
                var normalized = offsetUs + (samplePts - firstPtsUs).coerceAtLeast(0L)
                if (normalized <= lastPtsUs) normalized = lastPtsUs + 1L
                lastPtsUs = normalized
                ptsUs = normalized
                size = sampleSize
                flags = if (active.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                active.advance()
                return true
            }
        }

        private fun openNext(): Boolean {
            closeActive()
            segmentIndex++
            if (segmentIndex >= segments.size) return false
            val segment = segments[segmentIndex]
            val created = MediaExtractor()
            val track = try {
                created.setDataSource(segment.segment.file.absolutePath)
                findTrack(created, "video/")
            } catch (_: Throwable) {
                -1
            }
            if (track < 0) {
                runCatching { created.release() }
                return openNext()
            }
            created.selectTrack(track)
            extractor = created
            firstPtsUs = -1L
            offsetUs = segment.offsetUs
            return true
        }

        private fun closeActive() {
            extractor?.let { runCatching { it.release() } }
            extractor = null
        }

        override fun close() = closeActive()
    }

    private class AudioSampleCursor(audioFile: File) : SampleCursor {
        override val buffer: ByteBuffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
        override var ptsUs: Long = 0L
            private set
        override var size: Int = 0
            private set
        override var flags: Int = 0
            private set

        val format: MediaFormat
        private val extractor = MediaExtractor()
        private var firstPtsUs = -1L
        private var lastPtsUs = -1L

        init {
            try {
                extractor.setDataSource(audioFile.absolutePath)
                val track = findTrack(extractor, "audio/")
                if (track < 0) throw IllegalStateException("Không tìm thấy âm thanh")
                extractor.selectTrack(track)
                format = extractor.getTrackFormat(track)
            } catch (error: Throwable) {
                runCatching { extractor.release() }
                throw error
            }
        }

        override fun next(): Boolean {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) return false
            if (sampleSize > buffer.capacity()) {
                throw IllegalStateException("Mẫu âm thanh quá lớn để ghép")
            }
            val samplePts = extractor.sampleTime
            if (firstPtsUs < 0L) firstPtsUs = samplePts
            var normalized = (samplePts - firstPtsUs).coerceAtLeast(0L)
            if (normalized <= lastPtsUs) normalized = lastPtsUs + 1L
            lastPtsUs = normalized
            ptsUs = normalized
            size = sampleSize
            flags = 0
            extractor.advance()
            return true
        }

        override fun close() {
            runCatching { extractor.release() }
        }
    }
}
