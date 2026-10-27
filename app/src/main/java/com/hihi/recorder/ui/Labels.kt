package com.hihi.recorder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hihi.recorder.R
import com.hihi.recorder.data.AudioSource
import com.hihi.recorder.data.AutoStop
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.Countdown
import com.hihi.recorder.data.MaterialSpec
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecorderTheme
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.data.UiStyle
import com.hihi.recorder.data.VideoBitrate
import com.hihi.recorder.data.VideoFrameRate
import com.hihi.recorder.data.VideoResolution
import com.hihi.recorder.shell.ShellStatus
import com.hihi.recorder.shell.messageRes
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureMode.label(): String = stringResource(
    when (this) {
        CaptureMode.STANDARD -> R.string.capture_standard
        CaptureMode.SHELL -> R.string.capture_shell
    },
)

@Composable
fun CaptureMode.summary(): String = stringResource(
    when (this) {
        CaptureMode.STANDARD -> R.string.capture_standard_summary
        CaptureMode.SHELL -> R.string.capture_shell_summary
    },
)

@Composable
fun ShellBackend.label(): String = stringResource(
    when (this) {
        ShellBackend.ADB -> R.string.shell_backend_adb
        ShellBackend.SHIZUKU -> R.string.shell_backend_shizuku
        ShellBackend.ROOT -> R.string.shell_backend_root
    },
)

@Composable
fun ShellBackend.summary(): String = stringResource(
    when (this) {
        ShellBackend.ADB -> R.string.shell_backend_adb_summary
        ShellBackend.SHIZUKU -> R.string.shell_backend_shizuku_summary
        ShellBackend.ROOT -> R.string.shell_backend_root_summary
    },
)

@Composable
fun AudioSource.label(): String = stringResource(
    when (this) {
        AudioSource.NONE -> R.string.audio_none
        AudioSource.MICROPHONE -> R.string.audio_mic
        AudioSource.DEVICE -> R.string.audio_device
        AudioSource.MIC_AND_DEVICE -> R.string.audio_mix
    },
)

@Composable
fun AudioSource.summary(): String = stringResource(
    when (this) {
        AudioSource.NONE -> R.string.audio_none_summary
        AudioSource.MICROPHONE -> R.string.audio_mic_summary
        AudioSource.DEVICE -> R.string.audio_device_summary
        AudioSource.MIC_AND_DEVICE -> R.string.audio_mix_summary
    },
)

@Composable
fun VideoResolution.label(): String = stringResource(
    when (this) {
        VideoResolution.P720 -> R.string.resolution_720
        VideoResolution.P1080 -> R.string.resolution_1080
        VideoResolution.P1440 -> R.string.resolution_1440
        VideoResolution.NATIVE -> R.string.resolution_native
    },
)

@Composable
fun VideoFrameRate.label(): String = stringResource(R.string.fps_value, value)

@Composable
fun VideoBitrate.label(): String = when (this) {
    VideoBitrate.AUTO -> stringResource(R.string.bitrate_auto)
    else -> stringResource(R.string.bitrate_value, megabitsPerSecond ?: 0)
}

@Composable
fun Countdown.label(): String = when (this) {
    Countdown.OFF -> stringResource(R.string.countdown_off)
    else -> stringResource(R.string.countdown_seconds, seconds)
}

@Composable
fun AutoStop.label(): String = when (this) {
    AutoStop.NEVER -> stringResource(R.string.auto_stop_never)
    else -> stringResource(R.string.auto_stop_minutes, minutes ?: 0)
}

@Composable
fun RecorderTheme.label(): String = stringResource(
    when (this) {
        RecorderTheme.SYSTEM -> R.string.theme_system
        RecorderTheme.LIGHT -> R.string.theme_light
        RecorderTheme.DARK -> R.string.theme_dark
    },
)

@Composable
fun UiStyle.label(): String = stringResource(
    when (this) {
        UiStyle.MIUIX -> R.string.ui_style_miuix
        UiStyle.MATERIAL -> R.string.ui_style_material
    },
)

@Composable
fun MaterialSpec.label(): String = stringResource(
    when (this) {
        MaterialSpec.SPEC_2021 -> R.string.material_spec_2021
        MaterialSpec.SPEC_2025 -> R.string.material_spec_2025
    },
)

@Composable
fun ShellStatus.summary(backend: ShellBackend): String = when (this) {
    ShellStatus.Unknown -> stringResource(R.string.shell_status_unknown)
    ShellStatus.Checking -> stringResource(R.string.shell_status_checking)
    is ShellStatus.Available -> stringResource(
        when (backend) {
            ShellBackend.ADB -> R.string.shell_status_adb_ready
            ShellBackend.SHIZUKU -> R.string.shell_status_shizuku_ready
            ShellBackend.ROOT -> R.string.shell_status_root_ready
        },
    )

    is ShellStatus.Unavailable -> {
        val unavailable = stringResource(R.string.shell_status_unavailable)
        val reasonText = stringResource(reason.messageRes())
        "$unavailable · $reasonText"
    }
}

@Composable
fun recordingSummary(settings: RecorderSettings): String {
    val mode = settings.captureMode.label()
    val audio = settings.audioSource.label()
    val resolution = settings.resolution.label()
    return if (settings.captureMode == CaptureMode.SHELL) {
        stringResource(R.string.recording_summary_shell, mode, audio, resolution)
    } else {
        stringResource(
            R.string.recording_summary,
            mode,
            audio,
            resolution,
            settings.frameRate.value,
        )
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = durationMs / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    }
}

fun formatSavedAt(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(epochMs))
