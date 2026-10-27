package com.hihi.recorder.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Owns the MediaMuxer and starts it only after all expected track formats are
 * available. Encoder drains call [writeVideo] / [writeAudio] from two threads.
 */
class MuxerController(
    outputFile: File,
    private val expectVideo: Boolean = true,
) {
    private val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val lock = Object()
    private var expectedAudio = false
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var started = false
    private var released = false
    private var videoTrack = -1
    private var audioTrack = -1
    private var firstVideoPtsUs = -1L
    private var firstAudioPtsUs = -1L
    private var lastVideoPtsUs = -1L
    private var lastAudioPtsUs = -1L

    fun setExpectAudio(expected: Boolean) {
        synchronized(lock) {
            expectedAudio = expected
            maybeStartLocked()
        }
    }

    fun setVideoFormat(format: MediaFormat) {
        synchronized(lock) {
            if (videoFormat == null) {
                videoFormat = format
                maybeStartLocked()
            }
        }
    }

    fun setAudioFormat(format: MediaFormat) {
        synchronized(lock) {
            if (audioFormat == null) {
                audioFormat = format
                maybeStartLocked()
            }
        }
    }

    fun writeVideo(info: MediaCodec.BufferInfo, buffer: java.nio.ByteBuffer) {
        if (!expectVideo) return
        synchronized(lock) {
            awaitStartLocked()
            if (videoTrack < 0 || info.size <= 0) return
            if (firstVideoPtsUs < 0L) firstVideoPtsUs = info.presentationTimeUs
            var pts = (info.presentationTimeUs - firstVideoPtsUs).coerceAtLeast(0L)
            if (pts <= lastVideoPtsUs) pts = lastVideoPtsUs + 1L
            lastVideoPtsUs = pts
            val output = MediaCodec.BufferInfo().apply {
                set(info.offset, info.size, pts, info.flags)
            }
            muxer.writeSampleData(videoTrack, buffer, output)
        }
    }

    fun writeAudio(info: MediaCodec.BufferInfo, buffer: java.nio.ByteBuffer) {
        synchronized(lock) {
            if (!expectedAudio) return
            awaitStartLocked()
            if (audioTrack < 0 || info.size <= 0) return
            if (firstAudioPtsUs < 0L) firstAudioPtsUs = info.presentationTimeUs
            var pts = (info.presentationTimeUs - firstAudioPtsUs).coerceAtLeast(0L)
            if (pts <= lastAudioPtsUs) pts = lastAudioPtsUs + 1L
            lastAudioPtsUs = pts
            val output = MediaCodec.BufferInfo().apply {
                set(info.offset, info.size, pts, info.flags)
            }
            muxer.writeSampleData(audioTrack, buffer, output)
        }
    }

    fun stop() {
        synchronized(lock) {
            if (released) return
            released = true
            try {
                if (started) {
                    started = false
                    muxer.stop()
                }
            } finally {
                runCatching { muxer.release() }
            }
        }
    }

    fun abort() {
        synchronized(lock) {
            if (released) return
            released = true
            started = false
            runCatching { muxer.release() }
        }
    }

    private fun maybeStartLocked() {
        if (started || released) return
        if (expectVideo && videoFormat == null) return
        if (expectedAudio && audioFormat == null) return
        // An audio-only muxer whose source failed has no tracks to start; it is
        // simply released without ever starting.
        if (!expectVideo && !expectedAudio) return

        if (expectVideo) {
            videoTrack = muxer.addTrack(videoFormat!!)
        }
        if (expectedAudio) {
            audioTrack = muxer.addTrack(audioFormat!!)
        }
        muxer.start()
        started = true
        lock.notifyAll()
    }

    private fun awaitStartLocked() {
        if (started) return
        val deadline = System.currentTimeMillis() + 10_000L
        while (!started && !released) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0L) {
                error("Bộ ghép video chưa sẵn sàng")
            }
            lock.wait(remaining)
        }
        if (!started) error("Bộ ghép video đã đóng")
    }
}
