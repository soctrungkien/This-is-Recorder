package com.hihi.recorder.data

/** Where the soundtrack of a recording comes from. */
enum class AudioSource(
    val requiresAudioPermission: Boolean,
    val usesMicrophone: Boolean,
    val usesDeviceAudio: Boolean,
) {
    NONE(
        requiresAudioPermission = false,
        usesMicrophone = false,
        usesDeviceAudio = false,
    ),
    MICROPHONE(
        requiresAudioPermission = true,
        usesMicrophone = true,
        usesDeviceAudio = false,
    ),
    DEVICE(
        requiresAudioPermission = true,
        usesMicrophone = false,
        usesDeviceAudio = true,
    ),
    MIC_AND_DEVICE(
        requiresAudioPermission = true,
        usesMicrophone = true,
        usesDeviceAudio = true,
    ),
}

/** Chooses how Android captures the screen. */
enum class CaptureMode {
    /** MediaProjection + MediaCodec. Supports phone audio through playback capture. */
    STANDARD,

    /** Root shell + Android's screenrecord tool. No MediaProjection consent required. */
    ROOT,
}

/** The short edge of the requested output video. */
enum class VideoResolution(val shortEdge: Int?) {
    P720(720),
    P1080(1080),
    P1440(1440),
    NATIVE(null),
}

enum class VideoFrameRate(val value: Int) {
    FPS_24(24),
    FPS_30(30),
    FPS_60(60),
}

enum class VideoBitrate(val megabitsPerSecond: Int?) {
    AUTO(null),
    MBPS_8(8),
    MBPS_12(12),
    MBPS_20(20),
    MBPS_35(35),
}

enum class Countdown(val seconds: Int) {
    OFF(0),
    SECONDS_3(3),
    SECONDS_10(10),
}

enum class AutoStop(val minutes: Int?) {
    NEVER(null),
    MINUTES_5(5),
    MINUTES_15(15),
    MINUTES_30(30),
    MINUTES_60(60),
}

enum class RecorderTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Selects which design language the app uses for its chrome. */
enum class UiStyle {
    MIUIX,
    MATERIAL,
}

/** Material 3 color/token generation used for Monet and Material components. */
enum class MaterialSpec {
    SPEC_2021,
    SPEC_2025,
}

/**
 * Which bottom navigation bar to render.
 *
 * [MONET] is the standard Miuix/Material component bar. [LIQUID_IOS] is the
 * upstream Miuix `IosLiquidGlassNavigationBar` example (blur, refraction, and
 * drag-driven selection) adapted for this app.
 */
enum class NavigationBarStyle {
    MONET,
    LIQUID_IOS,
}

/**
 * Immutable snapshot of all user preferences. The recording service takes a
 * snapshot when a session starts, so changing a switch while recording cannot
 * affect the current encoder.
 */
data class RecorderSettings(
    val captureMode: CaptureMode = CaptureMode.STANDARD,
    val audioSource: AudioSource = AudioSource.DEVICE,
    val resolution: VideoResolution = VideoResolution.P1080,
    val frameRate: VideoFrameRate = VideoFrameRate.FPS_30,
    val bitrate: VideoBitrate = VideoBitrate.AUTO,
    val countdown: Countdown = Countdown.OFF,
    val autoStop: AutoStop = AutoStop.NEVER,
    val hapticFeedback: Boolean = true,
    val stopOnScreenOff: Boolean = false,
    val theme: RecorderTheme = RecorderTheme.SYSTEM,
    val uiStyle: UiStyle = UiStyle.MIUIX,
    val materialSpec: MaterialSpec = MaterialSpec.SPEC_2025,
    val monetColor: Boolean = true,
    val navigationBarStyle: NavigationBarStyle = NavigationBarStyle.LIQUID_IOS,
    val navigationIosLabels: Boolean = true,
    val saveFolder: String = DEFAULT_SAVE_FOLDER,
) {
    val usesAudio: Boolean get() = audioSource != AudioSource.NONE

    val effectiveAudio: AudioSource
        get() = if (captureMode == CaptureMode.ROOT && audioSource.usesDeviceAudio) {
            AudioSource.MICROPHONE
        } else {
            audioSource
        }

    val storagePath: String get() = "Movies/${normalizeSaveFolder(saveFolder)}"

    companion object {
        const val DEFAULT_SAVE_FOLDER = "ThisIsRecorder"
    }
}

/**
 * Keeps custom storage paths inside the public Movies collection. MediaStore
 * creates nested folders as needed, so no broad storage permission is needed.
 */
fun normalizeSaveFolder(raw: String): String {
    val stripped = raw
        .trim()
        .replace('\\', '/')
        .removePrefix("content://")
        .trim('/')
        .removePrefix("Movies/")
        .removePrefix("Movies")
        .trim('/')

    val pieces = stripped.split('/').map { piece ->
        piece
            .trim()
            .replace(Regex("[\\u0000-\\u001f:*?\"<>|]"), "_")
            .trim('.', ' ')
    }.filter { it.isNotEmpty() && it != "." && it != ".." }

    return pieces.joinToString("/").ifBlank { RecorderSettings.DEFAULT_SAVE_FOLDER }.take(120)
}
