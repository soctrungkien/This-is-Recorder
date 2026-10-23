package com.snowddo.recorder.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import kotlin.math.max

const val AUDIO_SAMPLE_RATE = 48_000
const val AUDIO_CHANNELS = 1

/** A blocking 16-bit mono PCM source. */
interface PcmAudioSource {
    fun start()
    fun read(buffer: ShortArray, offset: Int, count: Int): Int
    fun stop()
    fun close()

    class Error(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
}

private fun audioRecordBuilder(sourceRate: Int): AudioRecord.Builder =
    AudioRecord.Builder()
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sourceRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build(),
        )
        .setBufferSizeInBytes(bufferSizeFor(sourceRate))

private fun bufferSizeFor(sampleRate: Int): Int {
    val minimum = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(4096)
    return max(minimum * 2, sampleRate / 5 * 2)
}

class MicrophoneAudioSource(
    private val preferVoiceCommunication: Boolean,
) : PcmAudioSource {

    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start() {
        val preferred = if (preferVoiceCommunication) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.MIC
        }
        val created = runCatching { create(preferred) }.getOrElse {
            if (preferred == MediaRecorder.AudioSource.VOICE_COMMUNICATION) {
                create(MediaRecorder.AudioSource.MIC)
            } else {
                throw PcmAudioSource.Error("Không thể mở micrô", it)
            }
        }
        record = created
        if (created.state != AudioRecord.STATE_INITIALIZED) {
            created.release()
            record = null
            throw PcmAudioSource.Error("Micrô chưa sẵn sàng")
        }
        runCatching { created.startRecording() }.onFailure {
            created.release()
            record = null
            throw PcmAudioSource.Error("Không thể bắt đầu ghi micrô", it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun create(source: Int): AudioRecord =
        audioRecordBuilder(AUDIO_SAMPLE_RATE)
            .setAudioSource(source)
            .build()

    override fun read(buffer: ShortArray, offset: Int, count: Int): Int =
        record?.read(buffer, offset, count, AudioRecord.READ_BLOCKING) ?: -1

    override fun stop() {
        runCatching { record?.stop() }
    }

    override fun close() {
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
    }
}

class PlaybackAudioSource(
    private val projection: MediaProjection,
) : PcmAudioSource {

    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start() {
        val configuration = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN)
            .build()

        val created = runCatching {
            audioRecordBuilder(AUDIO_SAMPLE_RATE)
                .setAudioPlaybackCaptureConfig(configuration)
                .build()
        }.getOrElse {
            throw PcmAudioSource.Error("Không thể ghi âm thanh của điện thoại", it)
        }

        record = created
        if (created.state != AudioRecord.STATE_INITIALIZED) {
            created.release()
            record = null
            throw PcmAudioSource.Error("Âm thanh của điện thoại chưa sẵn sàng")
        }
        runCatching { created.startRecording() }.onFailure {
            created.release()
            record = null
            throw PcmAudioSource.Error("Không thể bắt đầu ghi âm thanh của điện thoại", it)
        }
    }

    override fun read(buffer: ShortArray, offset: Int, count: Int): Int =
        record?.read(buffer, offset, count, AudioRecord.READ_BLOCKING) ?: -1

    override fun stop() {
        runCatching { record?.stop() }
    }

    override fun close() {
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
    }
}

/** Mixes two equally sized mono sources into one, clipping instead of overflowing. */
class MixedAudioSource(
    private val first: PcmAudioSource,
    private val second: PcmAudioSource,
) : PcmAudioSource {

    private var firstBuffer = ShortArray(0)
    private var secondBuffer = ShortArray(0)

    override fun start() {
        first.start()
        try {
            second.start()
        } catch (error: Throwable) {
            first.close()
            throw error
        }
    }

    override fun read(buffer: ShortArray, offset: Int, count: Int): Int {
        if (firstBuffer.size < count) {
            firstBuffer = ShortArray(count)
            secondBuffer = ShortArray(count)
        }
        val firstRead = first.read(firstBuffer, 0, count)
        val secondRead = second.read(secondBuffer, 0, count)
        val available = minOf(
            if (firstRead > 0) firstRead else 0,
            if (secondRead > 0) secondRead else 0,
        )
        if (available <= 0) return if (firstRead < 0 || secondRead < 0) -1 else 0

        for (index in 0 until available) {
            buffer[offset + index] = (firstBuffer[index].toInt() + secondBuffer[index].toInt())
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return available
    }

    override fun stop() {
        runCatching { first.stop() }
        runCatching { second.stop() }
    }

    override fun close() {
        runCatching { first.close() }
        runCatching { second.close() }
    }
}
