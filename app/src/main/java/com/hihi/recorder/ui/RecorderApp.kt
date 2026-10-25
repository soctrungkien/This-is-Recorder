package com.hihi.recorder.ui

import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import com.hihi.recorder.data.AudioSource
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecordingPhase
import com.hihi.recorder.data.RecordingState
import com.hihi.recorder.data.SavedRecording
import com.hihi.recorder.net.UpdateChecker
import com.hihi.recorder.net.UpdateFailure
import com.hihi.recorder.net.UpdateResult
import com.hihi.recorder.root.RootChecker
import com.hihi.recorder.root.RootStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

@Serializable
private sealed interface AppRoute : NavKey {
    @Serializable
    data object Record : AppRoute

    @Serializable
    data object Videos : AppRoute

    @Serializable
    data object Settings : AppRoute
}

private val AppRoute.tabIndex: Int
    get() = when (this) {
        AppRoute.Record -> 0
        AppRoute.Videos -> 1
        AppRoute.Settings -> 2
    }

private fun appRouteAt(index: Int): AppRoute = when (index) {
    0 -> AppRoute.Record
    1 -> AppRoute.Videos
    else -> AppRoute.Settings
}

@Composable
fun RecorderApp(
    settings: RecorderSettings,
    recordingState: RecordingState,
    lastRecording: SavedRecording?,
    recordings: List<SavedRecording>,
    recordingsLoading: Boolean,
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
    onRefreshRecordings: () -> Unit,
    onRenameRecording: (SavedRecording, String) -> Unit,
    onDeleteRecording: (SavedRecording) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = appPalette()
    val updateUpToDate = stringResource(R.string.update_up_to_date)
    val rootMicSwitched = stringResource(R.string.root_mic_switched)
    val updateFailureMessages = mapOf(
        UpdateFailure.NO_RELEASES to stringResource(R.string.update_no_releases),
        UpdateFailure.NETWORK to stringResource(R.string.update_network_error),
        UpdateFailure.RATE_LIMIT to stringResource(R.string.update_rate_limited),
        UpdateFailure.SERVER to stringResource(R.string.update_server_error),
        UpdateFailure.INVALID_RESPONSE to stringResource(R.string.update_invalid_response),
    )
    // The tab bar drives a real miuix-nav back stack: selecting a tab pushes
    // it (or pops back to it when it is already below the top). That makes the
    // bar honest about what back does, keeps every visited tab's state alive,
    // and gives the page transition a continuous float to animate.
    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Record)
    val currentRoute = backStack.last() as AppRoute
    val selectedTab = currentRoute.tabIndex
    val navCornerRadius = rememberNavSystemCornerRadius()
    val swipeDismiss = if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
        NavSwipeDirection.LeftToRight
    } else {
        // NavSwipeDirection values are physical finger directions, so the
        // mirror is explicit instead of relying on layout direction.
        NavSwipeDirection.RightToLeft
    }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.hihi.recorder.net.UpdateInfo?>(null) }
    var updateChecking by remember { mutableStateOf(false) }
    var rootStatus by remember { mutableStateOf<RootStatus>(RootStatus.Unknown) }
    var rootCheckRequest by remember { mutableIntStateOf(0) }
    var timerMs by remember { mutableLongStateOf(0L) }

    val showRecordingToolbar =
        recordingState.phase == RecordingPhase.RECORDING && currentRoute != AppRoute.Record

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

    LaunchedEffect(
        recordingState.phase,
        recordingState.startedAtElapsedRealtime,
    ) {
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

    LaunchedEffect(Unit) {
        if (!UpdateChecker.shouldCheckAutomatically(context)) return@LaunchedEffect
        when (val result = UpdateChecker.check()) {
            is UpdateResult.Available -> updateInfo = result.info
            else -> Unit
        }
    }

    val checkForUpdates: () -> Unit = {
        if (!updateChecking) {
            updateChecking = true
            scope.launch {
                when (val result = UpdateChecker.check()) {
                    is UpdateResult.Available -> updateInfo = result.info
                    UpdateResult.UpToDate -> {
                        snackbarHostState.showSnackbar(updateUpToDate)
                    }

                    is UpdateResult.Failed -> {
                        snackbarHostState.showSnackbar(
                            updateFailureMessages.getValue(result.reason),
                        )
                    }
                }
                updateChecking = false
            }
        }
    }

    val changeCaptureMode: (CaptureMode) -> Unit = { mode ->
        if (mode != settings.captureMode) {
            var next = settings.copy(captureMode = mode)
            if (mode == CaptureMode.ROOT && settings.audioSource.usesDeviceAudio) {
                next = next.copy(audioSource = AudioSource.MICROPHONE)
                scope.launch {
                    snackbarHostState.showSnackbar(rootMicSwitched)
                }
            }
            onSettingsChange(next)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AmbientGlow(palette = palette)
            CompositionLocalProvider(
                LocalExtraBottomPadding provides if (showRecordingToolbar) 72.dp else 0.dp,
            ) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = {
                        // Never empty the stack: at the root the system back
                        // gesture exits the app instead.
                        if (backStack.size > 1) backStack.removeLastOrNull()
                    },
                    transition = NavTransitions.MiuixDefault,
                    effects = NavDisplayEffects(
                        // Follow the physical screen corner while the incoming
                        // page slides over the covered one, and let the default
                        // transition own the scrim curve along the motion.
                        cornerClipRadius = navCornerRadius,
                        dimAmount = 0.32f,
                        blockInputDuringTransition = true,
                        backdropColor = palette.background,
                    ),
                ) {
                    entry<AppRoute.Record> {
                        RecordScreen(
                            settings = settings,
                            recordingState = recordingState,
                            lastRecording = lastRecording,
                            timerMs = timerMs,
                            rootStatus = rootStatus,
                            checkingRoot = checkingRoot || rootStatus == RootStatus.Checking,
                            snackbarHostState = snackbarHostState,
                            onSettingsChange = onSettingsChange,
                            onCaptureModeChange = changeCaptureMode,
                            onCheckRoot = {
                                rootCheckRequest++
                                if (settings.captureMode != CaptureMode.ROOT) {
                                    changeCaptureMode(CaptureMode.ROOT)
                                }
                            },
                            onStart = onStart,
                            onStop = onStop,
                            onOpenRecording = onOpenRecording,
                            onShareRecording = onShareRecording,
                        )
                    }

                    entry<AppRoute.Videos>(swipeDismiss = swipeDismiss) {
                        VideosScreen(
                            recordings = recordings,
                            isLoading = recordingsLoading,
                            snackbarHostState = snackbarHostState,
                            onRefresh = onRefreshRecordings,
                            onOpen = onOpenRecording,
                            onShare = onShareRecording,
                            onRename = onRenameRecording,
                            onDelete = onDeleteRecording,
                        )
                    }

                    entry<AppRoute.Settings>(swipeDismiss = swipeDismiss) {
                        SettingsScreen(
                            settings = settings,
                            snackbarHostState = snackbarHostState,
                            updateChecking = updateChecking,
                            onSettingsChange = onSettingsChange,
                            onCheckForUpdates = checkForUpdates,
                            onOpenUrl = onOpenUrl,
                            onShowHelp = { showHelp = true },
                        )
                    }
                }
            }
        }

        AppNavigationBar(
            items = listOf(
                AppNavItem(
                    label = stringResource(R.string.nav_record),
                    icon = MiuixIcons.ScreenCapture,
                ),
                AppNavItem(
                    label = stringResource(R.string.nav_videos),
                    icon = MiuixIcons.Album,
                    badgeCount = recordings.size,
                ),
                AppNavItem(
                    label = stringResource(R.string.nav_settings),
                    icon = MiuixIcons.Settings,
                ),
            ),
            selectedIndex = selectedTab,
            onSelect = { index ->
                val target = appRouteAt(index)
                val existing = backStack.indexOf(target)
                if (existing >= 0) {
                    // Re-selecting a tab that is already below the top is a
                    // multi-pop: miuix-nav sweeps the whole depth range in one
                    // continuous reverse motion instead of cross-fading.
                    while (backStack.size > existing + 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                } else {
                    backStack.add(target)
                }
            },
            selectedLabelsOnly = settings.navigationIosLabels,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showRecordingToolbar) {
            FloatingToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = 20.dp, bottom = 94.dp)
                    .fillMaxWidth(),
                color = palette.surfaceContainerHigh,
                cornerRadius = 26.dp,
                outSidePadding = PaddingValues(0.dp),
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(palette.error, CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = stringResource(R.string.status_recording_title),
                            style = AppTextStyle.Subtitle,
                            color = palette.onSurface,
                        )
                        AppText(
                            text = formatDuration(timerMs),
                            style = AppTextStyle.Footnote,
                            color = palette.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    AppTextButton(
                        text = stringResource(R.string.action_stop),
                        onClick = onStop,
                        destructive = true,
                    )
                }
            }
        }
    }

    if (showMicPermissionDialog) {
        AppConfirmDialog(
            title = stringResource(R.string.permission_mic_title),
            message = stringResource(R.string.permission_mic_message),
            confirmLabel = stringResource(R.string.action_open_settings),
            onConfirm = {
                onDismissMicPermissionDialog()
                onOpenAppSettings()
            },
            onDismissRequest = onDismissMicPermissionDialog,
            dismissLabel = stringResource(R.string.action_close),
        )
    }

    if (showRootUnavailableDialog) {
        AppDialog(
            title = stringResource(R.string.root_required_title),
            message = stringResource(R.string.root_required_message),
            onDismissRequest = onDismissRootUnavailableDialog,
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismissRootUnavailableDialog,
                )
            },
        )
    }

    if (showHelp) {
        AppDialog(
            title = stringResource(R.string.help_title),
            onDismissRequest = { showHelp = false },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.action_close),
                    onClick = { showHelp = false },
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
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
                    body = stringResource(
                        R.string.help_storage_body,
                        settings.storagePath,
                    ),
                )
            }
        }
    }

    updateInfo?.let { info ->
        AppDialog(
            title = stringResource(R.string.update_available_title, info.versionName),
            message = stringResource(R.string.update_available_message),
            onDismissRequest = { updateInfo = null },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.action_later),
                    onClick = { updateInfo = null },
                )
                Spacer(Modifier.padding(start = 4.dp))
                AppTextButton(
                    text = stringResource(R.string.action_download),
                    onClick = {
                        updateInfo = null
                        onOpenUrl(info.releaseUrl)
                    },
                    primary = true,
                )
            },
        ) {
            if (info.notes.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    AppText(
                        text = info.notes.take(2_000),
                        style = AppTextStyle.SmallBody,
                        color = palette.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientGlow(palette: AppPalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(alpha = if (palette.isDark) 0.16f else 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.minDimension * 0.7f,
                    ),
                    radius = size.minDimension * 0.7f,
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primary.copy(
                                alpha = if (palette.isDark) 0.09f else 0.07f,
                            ),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.05f, size.height * 0.82f),
                        radius = size.minDimension * 0.55f,
                    ),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.05f, size.height * 0.82f),
                )
            },
    )
}

@Composable
private fun HelpEntry(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        AppText(
            text = title,
            style = AppTextStyle.Subtitle,
        )
        Spacer(Modifier.height(4.dp))
        AppText(
            text = body,
            style = AppTextStyle.SmallBody,
            color = appPalette().onSurfaceVariant,
        )
    }
}
