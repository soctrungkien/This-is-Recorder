package com.hihi.recorder.ui

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import com.hihi.recorder.data.AudioSource
import com.hihi.recorder.data.AutoStop
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.Countdown
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecordingPhase
import com.hihi.recorder.data.RecordingState
import com.hihi.recorder.data.SavedRecording
import com.hihi.recorder.data.VideoBitrate
import com.hihi.recorder.data.VideoFrameRate
import com.hihi.recorder.data.VideoResolution
import com.hihi.recorder.media.resolveCaptureSize
import com.hihi.recorder.root.RootStatus
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture

private enum class RecordPicker {
    AUDIO,
    RESOLUTION,
    FRAME_RATE,
    BITRATE,
    COUNTDOWN,
    AUTO_STOP,
}

@Composable
fun RecordScreen(
    settings: RecorderSettings,
    recordingState: RecordingState,
    lastRecording: SavedRecording?,
    timerMs: Long,
    rootStatus: RootStatus,
    checkingRoot: Boolean,
    snackbarHostState: SnackbarHostState,
    onSettingsChange: (RecorderSettings) -> Unit,
    onCaptureModeChange: (CaptureMode) -> Unit,
    onCheckRoot: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenRecording: (Uri) -> Unit,
    onShareRecording: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val locked = recordingState.isActive
    var picker by remember { mutableStateOf<RecordPicker?>(null) }
    val isRoot = settings.captureMode == CaptureMode.ROOT
    val audioSource = settings.audioSource
    val captureSize = remember(settings.resolution) {
        resolveCaptureSize(context, settings.resolution)
    }
    val bottomContentPadding = appBottomContentPadding()

    AppScaffold(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.app_subtitle),
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = bottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                RecorderHeroCard(
                    settings = settings,
                    state = recordingState,
                    lastRecording = lastRecording,
                    timerMs = timerMs,
                    checkingRoot = checkingRoot,
                    onStart = onStart,
                    onStop = onStop,
                )
            }

            item {
                AppSectionTitle(stringResource(R.string.section_capture_mode))
            }
            item {
                AppCard(contentPadding = PaddingValues(14.dp)) {
                    AppSegmentedControl(
                        options = listOf(
                            CaptureMode.STANDARD to stringResource(R.string.mode_standard_short),
                            CaptureMode.ROOT to stringResource(R.string.mode_root_short),
                        ),
                        selected = settings.captureMode,
                        onSelect = onCaptureModeChange,
                        enabled = !locked,
                    )
                    Spacer(Modifier.height(9.dp))
                    AppText(
                        text = settings.captureMode.summary(),
                        style = AppTextStyle.Footnote,
                        color = appPalette().onSurfaceVariant,
                    )
                    if (locked) {
                        Spacer(Modifier.height(5.dp))
                        AppText(
                            text = stringResource(R.string.recording_locked_hint),
                            style = AppTextStyle.Footnote,
                            color = appPalette().onSurfaceVariant,
                        )
                    }
                }
            }

            if (isRoot) {
                item {
                    AppCard(contentPadding = PaddingValues(0.dp)) {
                        AppRow(
                            title = stringResource(R.string.pref_root_status),
                            summary = rootStatus.summary(),
                            onClick = onCheckRoot,
                            trailing = {
                                if (rootStatus == RootStatus.Checking) {
                                    AppSpinner(size = 18.dp)
                                } else {
                                    AppIcon(
                                        icon = MiuixIcons.Refresh,
                                        contentDescription = stringResource(R.string.action_check_root),
                                        modifier = Modifier.size(19.dp),
                                        tint = appPalette().onSurfaceVariant,
                                    )
                                }
                            },
                        )
                        AppDivider()
                        Column(modifier = Modifier.padding(16.dp)) {
                            AppText(
                                text = stringResource(R.string.root_note_title),
                                style = AppTextStyle.Subtitle,
                            )
                            Spacer(Modifier.height(4.dp))
                            AppText(
                                text = stringResource(R.string.root_note),
                                style = AppTextStyle.Footnote,
                                color = appPalette().onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                LastRecordingCard(
                    recording = lastRecording,
                    storagePath = settings.storagePath,
                    onOpen = onOpenRecording,
                    onShare = onShareRecording,
                )
            }

            item { AppSectionTitle(stringResource(R.string.section_audio)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppSelectRow(
                        title = stringResource(R.string.pref_audio_source),
                        summary = audioSource.summary(),
                        value = audioSource.label(),
                        enabled = !locked,
                        onClick = { picker = RecordPicker.AUDIO },
                    )
                    if (audioSource.usesDeviceAudio) {
                        AppDivider()
                        AppHint(text = stringResource(R.string.audio_device_warning))
                    }
                }
            }

            item { AppSectionTitle(stringResource(R.string.section_video)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppSelectRow(
                        title = stringResource(R.string.pref_resolution),
                        summary = "${settings.resolution.label()} · ${captureSize.spec}",
                        value = settings.resolution.label(),
                        enabled = !locked,
                        onClick = { picker = RecordPicker.RESOLUTION },
                    )
                    AppDivider()
                    AppSelectRow(
                        title = stringResource(R.string.pref_frame_rate),
                        summary = if (isRoot) {
                            stringResource(R.string.fps_root_summary)
                        } else {
                            null
                        },
                        value = settings.frameRate.label(),
                        enabled = !locked && !isRoot,
                        onClick = { picker = RecordPicker.FRAME_RATE },
                    )
                    AppDivider()
                    AppSelectRow(
                        title = stringResource(R.string.pref_bitrate),
                        value = settings.bitrate.label(),
                        enabled = !locked,
                        onClick = { picker = RecordPicker.BITRATE },
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.section_timing)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppSelectRow(
                        title = stringResource(R.string.pref_countdown),
                        value = settings.countdown.label(),
                        enabled = !locked,
                        onClick = { picker = RecordPicker.COUNTDOWN },
                    )
                    AppDivider()
                    AppSelectRow(
                        title = stringResource(R.string.pref_auto_stop),
                        value = settings.autoStop.label(),
                        enabled = !locked,
                        onClick = { picker = RecordPicker.AUTO_STOP },
                    )
                }
            }
        }
    }

    when (picker) {
        RecordPicker.AUDIO -> AppSelectDialog(
            title = stringResource(R.string.pref_audio_source),
            options = AudioSource.entries.map { source ->
                SelectOption(source, source.label(), source.summary())
            },
            selected = audioSource,
            onSelect = { source ->
                onSettingsChange(settings.copy(audioSource = source))
            },
            onDismissRequest = { picker = null },
        )

        RecordPicker.RESOLUTION -> AppSelectDialog(
            title = stringResource(R.string.pref_resolution),
            options = VideoResolution.entries.map { value ->
                SelectOption(value, value.label())
            },
            selected = settings.resolution,
            onSelect = { value -> onSettingsChange(settings.copy(resolution = value)) },
            onDismissRequest = { picker = null },
        )

        RecordPicker.FRAME_RATE -> AppSelectDialog(
            title = stringResource(R.string.pref_frame_rate),
            options = VideoFrameRate.entries.map { value ->
                SelectOption(value, value.label())
            },
            selected = settings.frameRate,
            onSelect = { value -> onSettingsChange(settings.copy(frameRate = value)) },
            onDismissRequest = { picker = null },
        )

        RecordPicker.BITRATE -> AppSelectDialog(
            title = stringResource(R.string.pref_bitrate),
            options = VideoBitrate.entries.map { value ->
                SelectOption(value, value.label())
            },
            selected = settings.bitrate,
            onSelect = { value -> onSettingsChange(settings.copy(bitrate = value)) },
            onDismissRequest = { picker = null },
        )

        RecordPicker.COUNTDOWN -> AppSelectDialog(
            title = stringResource(R.string.pref_countdown),
            options = Countdown.entries.map { value ->
                SelectOption(value, value.label())
            },
            selected = settings.countdown,
            onSelect = { value -> onSettingsChange(settings.copy(countdown = value)) },
            onDismissRequest = { picker = null },
        )

        RecordPicker.AUTO_STOP -> AppSelectDialog(
            title = stringResource(R.string.pref_auto_stop),
            options = AutoStop.entries.map { value ->
                SelectOption(value, value.label())
            },
            selected = settings.autoStop,
            onSelect = { value -> onSettingsChange(settings.copy(autoStop = value)) },
            onDismissRequest = { picker = null },
        )

        null -> Unit
    }
}

@Composable
private fun RecorderHeroCard(
    settings: RecorderSettings,
    state: RecordingState,
    lastRecording: SavedRecording?,
    timerMs: Long,
    checkingRoot: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val palette = appPalette()
    val phase = state.phase
    val isError = phase == RecordingPhase.ERROR
    val container = if (isError) palette.error else palette.heroContainer
    val content = if (isError) palette.onError else palette.onHeroContainer
    val timerText = when (phase) {
        RecordingPhase.IDLE -> "00:00"
        RecordingPhase.PREPARING -> if (state.countdownSeconds > 0) {
            state.countdownSeconds.toString()
        } else {
            "…"
        }

        RecordingPhase.RECORDING -> formatDuration(timerMs)
        RecordingPhase.STOPPING -> if (timerMs > 0L) formatDuration(timerMs) else "…"
        RecordingPhase.SAVED -> lastRecording?.let { formatDuration(it.durationMs) } ?: "—"
        RecordingPhase.ERROR -> "!"
    }
    val title = stringResource(
        when (phase) {
            RecordingPhase.IDLE -> R.string.status_idle_title
            RecordingPhase.PREPARING -> if (state.countdownSeconds > 0) {
                R.string.status_countdown_title
            } else {
                R.string.status_preparing_title
            }

            RecordingPhase.RECORDING -> R.string.status_recording_title
            RecordingPhase.STOPPING -> R.string.status_stopping_title
            RecordingPhase.SAVED -> R.string.status_saved_title
            RecordingPhase.ERROR -> R.string.status_error_title
        },
    )
    val summary = when (phase) {
        RecordingPhase.IDLE, RecordingPhase.RECORDING -> recordingSummary(settings)
        RecordingPhase.PREPARING -> stringResource(R.string.status_preparing_summary)
        RecordingPhase.STOPPING -> stringResource(R.string.status_stopping_summary)
        RecordingPhase.SAVED -> stringResource(R.string.status_saved_summary, settings.storagePath)
        RecordingPhase.ERROR -> state.errorMessage
            ?: stringResource(R.string.status_error_summary)
    }

    AppCard(
        containerColor = container,
        contentColor = content,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(content.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    icon = MiuixIcons.ScreenCapture,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = content,
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = stringResource(R.string.app_name),
                    style = AppTextStyle.Subtitle,
                    color = content,
                    fontWeight = FontWeight.SemiBold,
                )
                AppText(
                    text = stringResource(R.string.app_subtitle),
                    style = AppTextStyle.Footnote,
                    color = content.copy(alpha = 0.68f),
                )
            }
            if (phase == RecordingPhase.RECORDING) {
                PulsingDot(color = Color(0xFFFF4D5A))
            }
        }

        Spacer(Modifier.height(18.dp))
        AppText(
            text = timerText,
            style = AppTextStyle.Display,
            color = content,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        AppText(
            text = title,
            style = AppTextStyle.Title,
            color = content,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(5.dp))
        AppText(
            text = summary,
            style = AppTextStyle.Footnote,
            color = content.copy(alpha = 0.74f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )

        state.warningMessage?.takeIf { it.isNotBlank() }?.let { warning ->
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                AppIcon(
                    icon = MiuixIcons.Info,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(17.dp),
                    tint = content.copy(alpha = 0.82f),
                )
                Spacer(Modifier.width(8.dp))
                AppText(
                    text = warning,
                    style = AppTextStyle.Footnote,
                    color = content.copy(alpha = 0.82f),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        if (state.isActive) {
            AppButton(
                text = stringResource(
                    if (phase == RecordingPhase.RECORDING) {
                        R.string.action_stop
                    } else {
                        R.string.action_cancel
                    },
                ),
                onClick = onStop,
                enabled = phase != RecordingPhase.STOPPING,
                destructive = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppButton(
                text = stringResource(
                    if (phase == RecordingPhase.ERROR) {
                        R.string.action_retry
                    } else {
                        R.string.action_start
                    },
                ),
                onClick = onStart,
                enabled = !checkingRoot,
                loading = checkingRoot,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LastRecordingCard(
    recording: SavedRecording?,
    storagePath: String,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    val palette = appPalette()
    AppCard(contentPadding = PaddingValues(14.dp)) {
        if (recording == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(palette.surfaceContainerHigh, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        icon = MiuixIcons.ScreenCapture,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = palette.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = stringResource(R.string.last_none_title),
                        style = AppTextStyle.Subtitle,
                    )
                    Spacer(Modifier.height(2.dp))
                    AppText(
                        text = stringResource(R.string.last_none_summary, storagePath),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecordingThumbnail(
                    uri = recording.uri,
                    modifier = Modifier
                        .width(112.dp)
                        .height(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = recording.displayName,
                        style = AppTextStyle.Subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    AppText(
                        text = stringResource(
                            R.string.last_saved_at,
                            formatSavedAt(recording.savedAtEpochMs),
                        ),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    AppText(
                        text = stringResource(
                            R.string.last_meta,
                            formatDuration(recording.durationMs),
                            formatBytes(recording.sizeBytes),
                        ),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            AppDivider(horizontalPadding = 0.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AppTextButton(
                    text = stringResource(R.string.action_open),
                    onClick = { onOpen(recording.uri) },
                )
                AppTextButton(
                    text = stringResource(R.string.action_share),
                    onClick = { onShare(recording.uri) },
                )
            }
        }
    }
}

@Composable
private fun AppHint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AppIcon(
            icon = MiuixIcons.Info,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
            tint = appPalette().onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        AppText(
            text = text,
            style = AppTextStyle.Footnote,
            color = appPalette().onSurfaceVariant,
        )
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .size(11.dp)
            .background(color.copy(alpha = alpha), CircleShape),
    )
}
