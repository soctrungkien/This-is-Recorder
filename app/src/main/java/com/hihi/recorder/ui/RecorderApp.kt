package com.hihi.recorder.ui

import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.NavigationBarStyle
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecordingPhase
import com.hihi.recorder.data.RecordingState
import com.hihi.recorder.data.SavedRecording
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.data.UiStyle
import com.hihi.recorder.net.UpdateChecker
import com.hihi.recorder.net.UpdateFailure
import com.hihi.recorder.net.UpdateResult
import com.hihi.recorder.shell.AdbDaemon
import com.hihi.recorder.shell.ShellChecker
import com.hihi.recorder.shell.ShellFailure
import com.hihi.recorder.shell.ShellStatus
import com.hihi.recorder.ui.liquid.IosLiquidGlassNavigationBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
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

    @Serializable
    data object About : AppRoute

    @Serializable
    data object MitLicense : AppRoute

    @Serializable
    data object ApacheLicense : AppRoute
}

private val AppRoute.tabIndex: Int
    get() = when (this) {
        AppRoute.Record -> 0
        AppRoute.Videos -> 1
        AppRoute.Settings,
        AppRoute.About,
        AppRoute.MitLicense,
        AppRoute.ApacheLicense -> 2
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
    checkingShell: Boolean,
    autoStartToken: Int,
    showMicPermissionDialog: Boolean,
    showShellUnavailableDialog: Boolean,
    shellFailure: ShellFailure?,
    onSettingsChange: (RecorderSettings) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismissMicPermissionDialog: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissShellUnavailableDialog: () -> Unit,
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
    var shellStatus by remember { mutableStateOf<ShellStatus>(ShellStatus.Unknown) }
    var shellCheckRequest by remember { mutableIntStateOf(0) }
    var timerMs by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current
    val navigationBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    var navigationBarHeightPx by remember { mutableIntStateOf(0) }

    val showRecordingToolbar =
        recordingState.phase == RecordingPhase.RECORDING && currentRoute != AppRoute.Record
    // Scroll views reserve room for the floating toolbar. Animate the reserved
    // space together with the bar so it does not jump when the bar appears.
    val extraBottomPadding by animateDpAsState(
        targetValue = if (showRecordingToolbar) 72.dp else 0.dp,
        animationSpec = tween(durationMillis = 260),
        label = "recordingToolbarPadding",
    )

    // Miuix's Scaffold normally places a FloatingToolbar above its bottomBar.
    // Here the bar is a standalone overlay, so measure it and reserve exactly
    // 10 dp between the bar's top edge and the toolbar's bottom edge. The
    // estimates only cover the first frame before onSizeChanged reports.
    val useLiquidNavigation = settings.navigationBarStyle == NavigationBarStyle.LIQUID_IOS
    val estimatedNavigationBarHeight = when {
        useLiquidNavigation -> 64.dp + if (navigationBarInset != 0.dp) {
            8.dp + navigationBarInset
        } else {
            // The upstream liquid bar uses a 36 dp gap when the inset is zero.
            36.dp
        }

        LocalUiStyle.current == UiStyle.MIUIX -> 64.dp + navigationBarInset
        else -> 80.dp + navigationBarInset
    }
    val navigationBarHeight = if (navigationBarHeightPx > 0) {
        with(density) { navigationBarHeightPx.toDp() }
    } else {
        estimatedNavigationBarHeight
    }
    val navigationBarHeightAboveInset =
        (navigationBarHeight - navigationBarInset).coerceAtLeast(0.dp)

    // Blur is a general capability: the liquid bar and the About page can both
    // sample a backdrop. Allocate one whenever the user enabled blur and
    // RuntimeShader exists (Android 13+), regardless of the navbar style.
    //
    // Two layers are needed because a layer must never contain the surfaces
    // that sample it: the recorded display list would reference its own
    // RenderNode, and HWUI would recurse through prepareTreeImpl until the
    // RenderThread stack overflows.
    val canBlur = settings.navigationBlur && Build.VERSION.SDK_INT >= 33
    // Sampled by glass surfaces that live inside the page tree (top app bars,
    // About cards), so it may only record the ambient background behind them.
    val surfaceBackdrop = if (canBlur) {
        rememberLayerBackdrop {
            drawRect(palette.background)
            drawContent()
        }
    } else {
        null
    }
    // Sampled by the floating navigation bar, which is a sibling overlay rather
    // than a descendant of the page, so it can safely record the page content.
    val navigationBackdrop = if (canBlur && useLiquidNavigation) {
        rememberLayerBackdrop {
            drawRect(palette.background)
            drawAmbientGlow(palette)
            drawContent()
        }
    } else {
        null
    }

    val navItems = listOf(
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
    )
    val selectTab: (Int) -> Unit = { index ->
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
    }

    LaunchedEffect(autoStartToken) {
        if (autoStartToken > 0) onStart()
    }

    LaunchedEffect(settings.captureMode, settings.shellBackend, shellCheckRequest) {
        if (settings.captureMode == CaptureMode.SHELL) {
            shellStatus = ShellStatus.Checking
            shellStatus = ShellChecker.check(
                context = context,
                backend = settings.shellBackend,
                force = shellCheckRequest > 0,
            )
            if (settings.shellBackend == ShellBackend.ADB) {
                // The daemon may be started from a computer while this screen
                // is open. A cheap heartbeat poll makes the status recover
                // without asking the user to tap refresh.
                while (true) {
                    delay(2_000L)
                    shellStatus = ShellChecker.check(
                        context = context,
                        backend = settings.shellBackend,
                        force = true,
                    )
                }
            }
        } else {
            shellStatus = ShellStatus.Unknown
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
            onSettingsChange(settings.copy(captureMode = mode))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        // Ambient background layer: recorded for the top bars and About cards.
        // It deliberately contains no blur consumers.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (surfaceBackdrop != null) {
                        Modifier.layerBackdrop(surfaceBackdrop)
                    } else {
                        Modifier
                    },
                ),
        ) {
            AmbientGlow(palette = palette)
        }

        // Page content layer: recorded for the floating navigation bar, which
        // is drawn outside this subtree.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (navigationBackdrop != null) {
                        Modifier.layerBackdrop(navigationBackdrop)
                    } else {
                        Modifier
                    },
                ),
        ) {
            CompositionLocalProvider(
                LocalExtraBottomPadding provides extraBottomPadding,
                LocalAppBackdrop provides surfaceBackdrop,
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
                            shellStatus = shellStatus,
                            checkingShell = checkingShell || shellStatus == ShellStatus.Checking,
                            snackbarHostState = snackbarHostState,
                            onSettingsChange = onSettingsChange,
                            onCaptureModeChange = changeCaptureMode,
                            onShellBackendChange = { backend ->
                                if (backend != settings.shellBackend) {
                                    onSettingsChange(settings.copy(shellBackend = backend))
                                }
                            },
                            onCheckShell = { shellCheckRequest++ },
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
                            onOpenAbout = {
                                if (backStack.lastOrNull() != AppRoute.About) {
                                    backStack.add(AppRoute.About)
                                }
                            },
                        )
                    }

                    entry<AppRoute.About>(swipeDismiss = swipeDismiss) {
                        AboutPage(
                            snackbarHostState = snackbarHostState,
                            blurEnabled = canBlur,
                            onBack = {
                                if (backStack.size > 1) backStack.removeLastOrNull()
                            },
                            onOpenUrl = onOpenUrl,
                            onShowHelp = { showHelp = true },
                            onOpenMitLicense = {
                                if (backStack.lastOrNull() != AppRoute.MitLicense) {
                                    backStack.add(AppRoute.MitLicense)
                                }
                            },
                            onOpenApacheLicense = {
                                if (backStack.lastOrNull() != AppRoute.ApacheLicense) {
                                    backStack.add(AppRoute.ApacheLicense)
                                }
                            },
                        )
                    }

                    entry<AppRoute.MitLicense>(swipeDismiss = swipeDismiss) {
                        LicensePage(
                            kind = LicenseKind.MIT,
                            snackbarHostState = snackbarHostState,
                            onBack = {
                                if (backStack.size > 1) backStack.removeLastOrNull()
                            },
                        )
                    }

                    entry<AppRoute.ApacheLicense>(swipeDismiss = swipeDismiss) {
                        LicensePage(
                            kind = LicenseKind.APACHE,
                            snackbarHostState = snackbarHostState,
                            onBack = {
                                if (backStack.size > 1) backStack.removeLastOrNull()
                            },
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { navigationBarHeightPx = it.height },
        ) {
            if (useLiquidNavigation) {
                IosLiquidGlassNavigationBar(
                    items = navItems.map { item ->
                        NavigationItem(label = item.label, icon = item.icon)
                    },
                    selectedIndex = selectedTab,
                    onItemClick = selectTab,
                    backdrop = navigationBackdrop,
                    isBlurActive = canBlur,
                    isDark = palette.isDark,
                    badge = { index ->
                        val count = navItems[index].badgeCount
                        if (count > 0) {
                            {
                                MiuixBadge {
                                    MiuixText(text = navBadgeText(count))
                                }
                            }
                        } else {
                            null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                AppNavigationBar(
                    items = navItems,
                    selectedIndex = selectedTab,
                    onSelect = selectTab,
                    selectedLabelsOnly = settings.navigationIosLabels,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnimatedVisibility(
            visible = showRecordingToolbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                slideInVertically(
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = FastOutSlowInEasing,
                    ),
                    initialOffsetY = { height -> height },
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutLinearInEasing,
                    ),
                    targetOffsetY = { height -> height },
                ),
        ) {
            FloatingToolbar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = navigationBarHeightAboveInset + 10.dp,
                    )
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

    if (showShellUnavailableDialog) {
        val adbCommand = remember {
            runCatching { AdbDaemon.activationCommand(context) }.getOrDefault("")
        }
        val message = when (shellFailure) {
            ShellFailure.ADB_NOT_RUNNING -> stringResource(
                R.string.shell_required_adb_message,
                adbCommand,
            )

            ShellFailure.SHIZUKU_NOT_RUNNING ->
                stringResource(R.string.shell_required_shizuku_message)

            ShellFailure.SHIZUKU_NO_PERMISSION ->
                stringResource(R.string.shell_required_shizuku_permission_message)

            ShellFailure.NO_SU -> stringResource(R.string.shell_required_root_message)
            ShellFailure.PROMPT_TIMEOUT -> stringResource(R.string.error_shell_root_timeout)
            ShellFailure.NO_SCREENRECORD -> stringResource(R.string.error_shell_no_screenrecord)
            ShellFailure.BACKEND_ERROR, null -> stringResource(R.string.shell_required_message)
        }
        AppDialog(
            title = stringResource(R.string.shell_required_title),
            message = message,
            onDismissRequest = onDismissShellUnavailableDialog,
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismissShellUnavailableDialog,
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
                    title = stringResource(R.string.help_shell_title),
                    body = stringResource(R.string.help_shell_body),
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
            .drawBehind { drawAmbientGlow(palette) },
    )
}

/**
 * Shared ambient gradient. It is drawn on screen and also recorded into the
 * bar backdrops, so the glass bars keep the same tint as the page behind them.
 */
private fun DrawScope.drawAmbientGlow(palette: AppPalette) {
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
