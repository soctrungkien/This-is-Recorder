package com.snowddo.recorder.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowddo.recorder.BuildConfig
import com.snowddo.recorder.R
import com.snowddo.recorder.data.AudioSource
import com.snowddo.recorder.data.AutoStop
import com.snowddo.recorder.data.CaptureMode
import com.snowddo.recorder.data.Countdown
import com.snowddo.recorder.data.RecorderSettings
import com.snowddo.recorder.data.RecorderTheme
import com.snowddo.recorder.data.RecordingPhase
import com.snowddo.recorder.data.RecordingState
import com.snowddo.recorder.data.SavedRecording
import com.snowddo.recorder.data.VideoBitrate
import com.snowddo.recorder.data.VideoFrameRate
import com.snowddo.recorder.data.VideoResolution
import com.snowddo.recorder.media.resolveCaptureSize
import com.snowddo.recorder.root.RootChecker
import com.snowddo.recorder.root.RootStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RecorderApp(
    settings: RecorderSettings,
    recordingState: RecordingState,
    lastRecording: SavedRecording?,
    snackbarHostState: SnackbarHostState,
    checkingRoot: Boolean,
    autoStartToken: Int,
    showMicPermissionDialog: Boolean,
    showRootUnavailableDialog: Boolean,
    onSettingsChange: (RecorderSettings) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismissMicPermissionDialog: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissRootUnavailableDialog: () -> Unit,
    onOpenRecording: (Uri) -> Unit,
    onShareRecording: (Uri) -> Unit,
) {
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    var showRootModeDialog by remember { mutableStateOf(false) }
    var rootStatus by remember { mutableStateOf<RootStatus>(RootStatus.Unknown) }
    var rootCheckRequest by remember { mutableIntStateOf(0) }
    var timerMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(autoStartToken) {
        if (autoStartToken > 0) onStart()
    }

    LaunchedEffect(settings.captureMode, rootCheckRequest) {
        if (settings.captureMode == CaptureMode.ROOT) {
            rootStatus = RootStatus.Checking
            rootStatus = RootChecker.check(context, force = rootCheckRequest > 0)
        } else {
            rootStatus = RootStatus.Unknown
        }
    }

    LaunchedEffect(recordingState.phase, recordingState.startedAtElapsedRealtime) {
        if (recordingState.phase == RecordingPhase.RECORDING) {
            while (true) {
                timerMs = (SystemClock.elapsedRealtime() - recordingState.startedAtElapsedRealtime)
                    .coerceAtLeast(0L)
                delay(500L)
            }
        } else if (recordingState.phase == RecordingPhase.IDLE) {
            timerMs = 0L
        }
    }

    val locked = recordingState.isActive
    val isRoot = settings.captureMode == CaptureMode.ROOT
    val audioOptions = if (isRoot) {
        listOf(AudioSource.NONE, AudioSource.MICROPHONE)
    } else {
        AudioSource.entries
    }
    val effectiveAudio = if (isRoot && settings.audioSource.usesDeviceAudio) {
        AudioSource.MICROPHONE
    } else {
        settings.audioSource
    }
    val audioIndex = audioOptions.indexOf(effectiveAudio).coerceAtLeast(0)
    val captureSize = resolveCaptureSize(context, settings.resolution)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_subtitle),
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            imageVector = MiuixIcons.Help,
                            contentDescription = stringResource(R.string.pref_help),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                HeroCard(
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
                LastRecordingCard(
                    recording = lastRecording,
                    onOpen = onOpenRecording,
                    onShare = onShareRecording,
                )
            }

            item { SectionTitle(stringResource(R.string.section_recording)) }
            item {
                PreferenceCard {
                    OverlayDropdownPreference(
                        items = listOf(
                            stringResource(R.string.capture_standard),
                            stringResource(R.string.capture_root),
                        ),
                        selectedIndex = if (isRoot) 1 else 0,
                        title = stringResource(R.string.pref_capture_mode),
                        summary = settings.captureMode.summary(),
                        enabled = !locked,
                        onSelectedIndexChange = { index ->
                            val mode = if (index == 1) CaptureMode.ROOT else CaptureMode.STANDARD
                            if (mode == settings.captureMode) return@OverlayDropdownPreference
                            if (mode == CaptureMode.ROOT && settings.audioSource.usesDeviceAudio) {
                                showRootModeDialog = true
                            } else {
                                onSettingsChange(settings.copy(captureMode = mode))
                            }
                        },
                    )
                }
            }

            if (isRoot) {
                item { SectionTitle(stringResource(R.string.section_root)) }
                item {
                    PreferenceCard {
                        ArrowPreference(
                            title = stringResource(R.string.pref_root_status),
                            summary = rootStatus.summary(),
                            enabled = !locked && rootStatus != RootStatus.Checking,
                            endActions = {
                                if (rootStatus == RootStatus.Checking) {
                                    CircularProgressIndicator(
                                        size = 18.dp,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            },
                            onClick = { rootCheckRequest++ },
                        )
                        PreferenceDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(18.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.root_note_title),
                                    style = MiuixTheme.textStyles.title4,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.root_note),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.section_audio)) }
            item {
                PreferenceCard {
                    OverlayDropdownPreference(
                        items = audioOptions.map { it.label() },
                        selectedIndex = audioIndex,
                        title = stringResource(R.string.pref_audio_source),
                        summary = effectiveAudio.summary(),
                        enabled = !locked,
                        onSelectedIndexChange = { index ->
                            val source = audioOptions.getOrNull(index) ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(audioSource = source))
                        },
                    )
                    if (effectiveAudio.usesDeviceAudio) {
                        PreferenceDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(18.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.audio_device_warning),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    } else if (isRoot && effectiveAudio != AudioSource.NONE) {
                        PreferenceDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(18.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.audio_root_warning),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.section_video)) }
            item {
                PreferenceCard {
                    OverlayDropdownPreference(
                        items = VideoResolution.entries.map { it.label() },
                        selectedIndex = VideoResolution.entries.indexOf(settings.resolution),
                        title = stringResource(R.string.pref_resolution),
                        summary = "${settings.resolution.label()} · ${captureSize.spec}",
                        enabled = !locked,
                        onSelectedIndexChange = { index ->
                            val value = VideoResolution.entries.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(resolution = value))
                        },
                    )
                    PreferenceDivider()
                    OverlayDropdownPreference(
                        items = VideoFrameRate.entries.map { it.label() },
                        selectedIndex = VideoFrameRate.entries.indexOf(settings.frameRate),
                        title = stringResource(R.string.pref_frame_rate),
                        summary = if (isRoot) {
                            stringResource(R.string.fps_root_summary)
                        } else {
                            settings.frameRate.label()
                        },
                        enabled = !locked && !isRoot,
                        onSelectedIndexChange = { index ->
                            val value = VideoFrameRate.entries.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(frameRate = value))
                        },
                    )
                    PreferenceDivider()
                    OverlayDropdownPreference(
                        items = VideoBitrate.entries.map { it.label() },
                        selectedIndex = VideoBitrate.entries.indexOf(settings.bitrate),
                        title = stringResource(R.string.pref_bitrate),
                        summary = settings.bitrate.label(),
                        enabled = !locked,
                        onSelectedIndexChange = { index ->
                            val value = VideoBitrate.entries.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(bitrate = value))
                        },
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.section_timing)) }
            item {
                PreferenceCard {
                    OverlayDropdownPreference(
                        items = Countdown.entries.map { it.label() },
                        selectedIndex = Countdown.entries.indexOf(settings.countdown),
                        title = stringResource(R.string.pref_countdown),
                        summary = settings.countdown.label(),
                        enabled = !locked,
                        onSelectedIndexChange = { index ->
                            val value = Countdown.entries.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(countdown = value))
                        },
                    )
                    PreferenceDivider()
                    OverlayDropdownPreference(
                        items = AutoStop.entries.map { it.label() },
                        selectedIndex = AutoStop.entries.indexOf(settings.autoStop),
                        title = stringResource(R.string.pref_auto_stop),
                        summary = settings.autoStop.label(),
                        enabled = !locked,
                        onSelectedIndexChange = { index ->
                            val value = AutoStop.entries.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(autoStop = value))
                        },
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.section_behavior)) }
            item {
                PreferenceCard {
                    SwitchPreference(
                        checked = settings.hapticFeedback,
                        onCheckedChange = {
                            onSettingsChange(settings.copy(hapticFeedback = it))
                        },
                        title = stringResource(R.string.pref_haptics),
                        enabled = !locked,
                    )
                    PreferenceDivider()
                    SwitchPreference(
                        checked = settings.stopOnScreenOff,
                        onCheckedChange = {
                            onSettingsChange(settings.copy(stopOnScreenOff = it))
                        },
                        title = stringResource(R.string.pref_stop_screen_off),
                        enabled = !locked,
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.section_appearance)) }
            item {
                PreferenceCard {
                    OverlayDropdownPreference(
                        items = RecorderTheme.entries.map { it.label() },
                        selectedIndex = RecorderTheme.entries.indexOf(settings.theme),
                        title = stringResource(R.string.pref_theme),
                        summary = settings.theme.label(),
                        enabled = true,
                        onSelectedIndexChange = { index ->
                            val value = RecorderTheme.entries.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            onSettingsChange(settings.copy(theme = value))
                        },
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.section_help)) }
            item {
                PreferenceCard {
                    ArrowPreference(
                        title = stringResource(R.string.pref_help),
                        summary = stringResource(R.string.help_title),
                        onClick = { showHelp = true },
                    )
                    PreferenceDivider()
                    InfoRow(
                        icon = MiuixIcons.Folder,
                        title = stringResource(R.string.pref_storage),
                        summary = stringResource(R.string.pref_storage_summary),
                    )
                    PreferenceDivider()
                    InfoRow(
                        icon = MiuixIcons.Info,
                        title = stringResource(R.string.pref_version),
                        summary = BuildConfig.VERSION_NAME,
                    )
                }
            }
        }
    }

    if (showRootModeDialog) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.root_mode_dialog_title),
            summary = stringResource(R.string.root_mode_dialog_message),
            onDismissRequest = { showRootModeDialog = false },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { showRootModeDialog = false },
                    modifier = Modifier.weight(1f),
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                )
                TextButton(
                    text = stringResource(R.string.action_enable_root),
                    onClick = {
                        showRootModeDialog = false
                        onSettingsChange(
                            settings.copy(
                                captureMode = CaptureMode.ROOT,
                                audioSource = AudioSource.MICROPHONE,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    if (showMicPermissionDialog) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.permission_mic_title),
            summary = stringResource(R.string.permission_mic_message),
            onDismissRequest = onDismissMicPermissionDialog,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismissMicPermissionDialog,
                    modifier = Modifier.weight(1f),
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                )
                TextButton(
                    text = stringResource(R.string.action_open_settings),
                    onClick = onOpenAppSettings,
                    modifier = Modifier.weight(1f),
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    if (showRootUnavailableDialog) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.root_required_title),
            summary = stringResource(R.string.root_required_message),
            onDismissRequest = onDismissRootUnavailableDialog,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismissRootUnavailableDialog,
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    if (showHelp) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.help_title),
            onDismissRequest = { showHelp = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                HelpEntry(
                    title = stringResource(R.string.help_quick_settings_title),
                    body = stringResource(R.string.help_quick_settings_body),
                )
                HelpEntry(
                    title = stringResource(R.string.help_audio_title),
                    body = stringResource(R.string.help_audio_body),
                )
                HelpEntry(
                    title = stringResource(R.string.help_root_title),
                    body = stringResource(R.string.help_root_body),
                )
                HelpEntry(
                    title = stringResource(R.string.help_storage_title),
                    body = stringResource(R.string.help_storage_body),
                )
                TextButton(
                    text = stringResource(R.string.action_close),
                    onClick = { showHelp = false },
                    modifier = Modifier.fillMaxWidth(),
                    minWidth = 0.dp,
                    minHeight = 40.dp,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    settings: RecorderSettings,
    state: RecordingState,
    lastRecording: SavedRecording?,
    timerMs: Long,
    checkingRoot: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val phase = state.phase
    val isError = phase == RecordingPhase.ERROR
    val container = if (isError) scheme.errorContainer else scheme.primaryVariant
    val content = if (isError) scheme.onErrorContainer else scheme.onPrimaryVariant
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
        RecordingPhase.SAVED -> lastRecording?.displayName
            ?: stringResource(R.string.status_saved_summary)
        RecordingPhase.ERROR -> state.errorMessage
            ?: stringResource(R.string.status_error_summary)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
        colors = CardDefaults.defaultColors(color = container, contentColor = content),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(content.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.ScreenCapture,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = content,
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MiuixTheme.textStyles.footnote2,
                    color = content.copy(alpha = 0.68f),
                )
            }
            if (phase == RecordingPhase.RECORDING) {
                PulsingDot(color = Color(0xFFFF4D5A))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = timerText,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = summary,
            style = MiuixTheme.textStyles.footnote1,
            color = content.copy(alpha = 0.74f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        state.warningMessage?.takeIf { it.isNotBlank() }?.let { warning ->
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = MiuixIcons.Info,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(17.dp),
                    tint = content.copy(alpha = 0.82f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = warning,
                    style = MiuixTheme.textStyles.footnote2,
                    color = content.copy(alpha = 0.82f),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        if (state.isActive) {
            Button(
                onClick = onStop,
                enabled = phase != RecordingPhase.STOPPING,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 50.dp,
                colors = ButtonDefaults.buttonColors(
                    color = scheme.error,
                    contentColor = scheme.onError,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (phase == RecordingPhase.RECORDING) {
                            R.string.action_stop
                        } else {
                            R.string.action_cancel
                        },
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Button(
                onClick = onStart,
                enabled = !checkingRoot,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 50.dp,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                if (checkingRoot) {
                    CircularProgressIndicator(
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = scheme.onPrimary,
                            backgroundColor = scheme.onPrimary.copy(alpha = 0.25f),
                        ),
                    )
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    text = stringResource(
                        if (phase == RecordingPhase.ERROR) {
                            R.string.action_retry
                        } else {
                            R.string.action_start
                        },
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LastRecordingCard(
    recording: SavedRecording?,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        if (recording == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            MiuixTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(13.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.ScreenCapture,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.last_none_title),
                        style = MiuixTheme.textStyles.title4,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.last_none_summary),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            val thumbnail by rememberThumbnail(recording.uri)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(recording.uri) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = thumbnail
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = MiuixIcons.ScreenCapture,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.displayName,
                        style = MiuixTheme.textStyles.title4,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(
                            R.string.last_saved_at,
                            formatSavedAt(recording.savedAtEpochMs),
                        ),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.last_meta,
                            formatDuration(recording.durationMs),
                            formatBytes(recording.sizeBytes),
                        ),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 2.dp),
                color = MiuixTheme.colorScheme.dividerLine,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.action_open),
                    onClick = { onOpen(recording.uri) },
                    minWidth = 0.dp,
                    minHeight = 34.dp,
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
                TextButton(
                    text = stringResource(R.string.action_share),
                    onClick = { onShare(recording.uri) },
                    minWidth = 0.dp,
                    minHeight = 34.dp,
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PreferenceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        content = content,
    )
}

@Composable
private fun PreferenceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MiuixTheme.colorScheme.dividerLine,
    )
}

@Composable
private fun SectionTitle(text: String) {
    SmallTitle(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun HelpEntry(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(850),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .size(11.dp)
            .background(color.copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun rememberThumbnail(uri: Uri): androidx.compose.runtime.State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { loadThumbnail(context, uri) }
    }
}

private fun loadThumbnail(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val bitmap = retriever.getScaledFrameAtTime(
            0L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            320,
            180,
        )
        bitmap?.asImageBitmap()
    } finally {
        runCatching { retriever.release() }
    }
}.getOrNull()
