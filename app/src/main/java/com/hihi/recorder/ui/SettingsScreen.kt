package com.hihi.recorder.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hihi.recorder.BuildConfig
import com.hihi.recorder.R
import com.hihi.recorder.data.AudioSource
import com.hihi.recorder.data.AutoStop
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.Countdown
import com.hihi.recorder.data.MaterialSpec
import com.hihi.recorder.data.NavigationBarStyle
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecorderTheme
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.data.UiStyle
import com.hihi.recorder.data.VideoBitrate
import com.hihi.recorder.data.VideoFrameRate
import com.hihi.recorder.data.VideoResolution
import com.hihi.recorder.data.normalizeSaveFolder
import com.zeroui.kit.glass.LiquidBackdrop
import com.zeroui.kit.glass.liquidGlass
import com.zeroui.kit.glass.liquidSource
import com.zeroui.kit.glass.rememberLiquidBackdrop
import com.zeroui.kit.theme.GlassMode
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.icon.extended.ZoomOut

private data class Picker(
    val title: String,
    val options: List<SelectOption<Any>>,
    val selected: Any,
    val onSelect: (Any) -> Unit,
)

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    AppRow(
        title = title,
        summary = summary,
        leadingIcon = icon,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = { AppSwitch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

@Composable
private fun SelectRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    value: String,
    onClick: () -> Unit,
) {
    AppRow(
        title = title,
        summary = summary,
        leadingIcon = icon,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    text = value,
                    modifier = Modifier.widthIn(max = 150.dp),
                    style = AppTextStyle.Footnote,
                    color = appPalette().onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                AppIcon(icon = MiuixIcons.ChevronForward, contentDescription = null)
            }
        },
    )
}

/** Row that opens a single-choice dialog; everything is composed up front. */
@Composable
private fun <T : Enum<T>> PickerRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    values: List<T>,
    labels: List<String>,
    selected: T,
    value: String,
    onSelect: (T) -> Unit,
    onOpen: (Picker) -> Unit,
) {
    val options = values.mapIndexed { index, item -> SelectOption(item as Any, labels[index]) }
    SelectRow(icon = icon, title = title, summary = summary, value = value) {
        onOpen(Picker(title, options, selected as Any) { chosen -> onSelect(chosen as T) })
    }
}

/**
 * Settings page using the app's original card/margin/Miuix-icon system.
 * ZeroUI liquid glass is applied to cards only for Miuix + Liquid glass mode.
 */
@Composable
fun SettingsScreen(
    settings: RecorderSettings,
    snackbarHostState: SnackbarHostState,
    updateChecking: Boolean,
    onSettingsChange: (RecorderSettings) -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowHelp: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var picker by remember { mutableStateOf<Picker?>(null) }
    var showSaveFolderDialog by remember { mutableStateOf(false) }
    var showSeedColorDialog by remember { mutableStateOf(false) }
    var showVerificationInfo by remember { mutableStateOf(false) }
    val palette = appPalette()
    val bottomContentPadding = appBottomContentPadding()
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val liquidBackdrop = rememberLiquidBackdrop(always = true)
    val useLiquidCards = settings.uiStyle == UiStyle.MIUIX &&
        settings.glassMode == GlassMode.Liquid && liquidBackdrop != null

    fun cardModifier(backdrop: LiquidBackdrop?): Modifier =
        if (useLiquidCards && backdrop != null) {
            Modifier.liquidGlass(
                backdrop = backdrop,
                shape = { RoundedCornerShape(18.dp) },
                intensity = settings.glassIntensity / 100f,
            )
        } else {
            Modifier
        }

    AppScaffold(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (useLiquidCards) {
                val backdrop = liquidBackdrop
                Box(
                    Modifier
                        .fillMaxSize()
                        .liquidSource(backdrop)
                        .drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.primary.copy(alpha = 0.10f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                        },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                item { AppSectionTitle(stringResource(R.string.zero_capture)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        PickerRow(
                            icon = MiuixIcons.ScreenCapture,
                            title = stringResource(R.string.pref_capture_mode),
                            values = CaptureMode.entries,
                            labels = CaptureMode.entries.map { it.label() },
                            selected = settings.captureMode,
                            value = settings.captureMode.label(),
                            onSelect = { onSettingsChange(settings.copy(captureMode = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        if (settings.captureMode == CaptureMode.SHELL) {
                            PickerRow(
                                icon = MiuixIcons.Tune,
                                title = stringResource(R.string.pref_shell_backend),
                                values = ShellBackend.entries,
                                labels = listOf("ADB", "Shizuku", "Root"),
                                selected = settings.shellBackend,
                                value = when (settings.shellBackend) {
                                    ShellBackend.ADB -> "ADB"
                                    ShellBackend.SHIZUKU -> "Shizuku"
                                    ShellBackend.ROOT -> "Root"
                                },
                                onSelect = { onSettingsChange(settings.copy(shellBackend = it)) },
                                onOpen = { picker = it },
                            )
                            AppDivider()
                        }
                        PickerRow(
                            icon = MiuixIcons.VolumeUp,
                            title = stringResource(R.string.pref_audio_source),
                            values = AudioSource.entries,
                            labels = AudioSource.entries.map { it.label() },
                            selected = settings.audioSource,
                            value = settings.audioSource.label(),
                            onSelect = { onSettingsChange(settings.copy(audioSource = it)) },
                            onOpen = { picker = it },
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.zero_video)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        PickerRow(
                            icon = MiuixIcons.Image,
                            title = stringResource(R.string.pref_resolution),
                            values = VideoResolution.entries,
                            labels = VideoResolution.entries.map { it.label() },
                            selected = settings.resolution,
                            value = settings.resolution.label(),
                            onSelect = { onSettingsChange(settings.copy(resolution = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        PickerRow(
                            icon = MiuixIcons.Stopwatch,
                            title = stringResource(R.string.pref_frame_rate),
                            values = VideoFrameRate.entries,
                            labels = VideoFrameRate.entries.map { it.label() },
                            selected = settings.frameRate,
                            value = settings.frameRate.label(),
                            onSelect = { onSettingsChange(settings.copy(frameRate = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        PickerRow(
                            icon = MiuixIcons.Tune,
                            title = stringResource(R.string.pref_bitrate),
                            values = VideoBitrate.entries,
                            labels = VideoBitrate.entries.map { it.label() },
                            selected = settings.bitrate,
                            value = settings.bitrate.label(),
                            onSelect = { onSettingsChange(settings.copy(bitrate = it)) },
                            onOpen = { picker = it },
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.zero_timer)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        PickerRow(
                            icon = MiuixIcons.Stopwatch,
                            title = stringResource(R.string.pref_countdown),
                            values = Countdown.entries,
                            labels = Countdown.entries.map { it.label() },
                            selected = settings.countdown,
                            value = settings.countdown.label(),
                            onSelect = { onSettingsChange(settings.copy(countdown = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        PickerRow(
                            icon = MiuixIcons.Pause,
                            title = stringResource(R.string.pref_auto_stop),
                            values = AutoStop.entries,
                            labels = AutoStop.entries.map { it.label() },
                            selected = settings.autoStop,
                            value = settings.autoStop.label(),
                            onSelect = { onSettingsChange(settings.copy(autoStop = it)) },
                            onOpen = { picker = it },
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.zero_behavior)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        SwitchRow(
                            icon = MiuixIcons.VolumeUp,
                            title = stringResource(R.string.pref_haptics),
                            checked = settings.hapticFeedback,
                        ) { onSettingsChange(settings.copy(hapticFeedback = it)) }
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Info,
                            title = stringResource(R.string.pref_stop_screen_off),
                            checked = settings.stopOnScreenOff,
                        ) { onSettingsChange(settings.copy(stopOnScreenOff = it)) }
                    }
                }

                item { AppSectionTitle(stringResource(R.string.section_appearance)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        PickerRow(
                            icon = MiuixIcons.Theme,
                            title = stringResource(R.string.pref_theme),
                            summary = stringResource(R.string.zero_theme_sub),
                            values = RecorderTheme.entries,
                            labels = RecorderTheme.entries.map { it.label() },
                            selected = settings.theme,
                            value = settings.theme.label(),
                            onSelect = { onSettingsChange(settings.copy(theme = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        PickerRow(
                            icon = MiuixIcons.VerticalSplit,
                            title = stringResource(R.string.pref_ui_style),
                            summary = stringResource(R.string.pref_ui_style_summary),
                            values = UiStyle.entries,
                            labels = UiStyle.entries.map { it.label() },
                            selected = settings.uiStyle,
                            value = settings.uiStyle.label(),
                            onSelect = { onSettingsChange(settings.copy(uiStyle = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        PickerRow(
                            icon = MiuixIcons.Layers,
                            title = stringResource(R.string.pref_material_spec),
                            values = MaterialSpec.entries,
                            labels = MaterialSpec.entries.map { it.label() },
                            selected = settings.materialSpec,
                            value = settings.materialSpec.label(),
                            onSelect = { onSettingsChange(settings.copy(materialSpec = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Background,
                            title = stringResource(R.string.pref_monet),
                            summary = stringResource(R.string.pref_monet_summary),
                            checked = settings.monetColor,
                            enabled = dynamicColorAvailable,
                        ) { onSettingsChange(settings.copy(monetColor = it)) }
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Background,
                            title = stringResource(R.string.zero_pure_black),
                            checked = settings.pureBlack,
                        ) { onSettingsChange(settings.copy(pureBlack = it)) }
                        AppDivider()
                        PickerRow(
                            icon = MiuixIcons.Theme,
                            title = stringResource(R.string.zero_palette),
                            values = com.zeroui.kit.theme.PaletteStyle.entries,
                            labels = com.zeroui.kit.theme.PaletteStyle.entries.map { it.name },
                            selected = settings.palette,
                            value = settings.palette.name,
                            onSelect = { onSettingsChange(settings.copy(palette = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.zero_seed_color),
                            summary = settings.seedColor.toString(),
                            leadingIcon = MiuixIcons.Theme,
                            onClick = { showSeedColorDialog = true },
                        )
                        AppDivider()
                        val motionValues = com.zeroui.kit.theme.MotionMode.entries
                        PickerRow(
                            icon = MiuixIcons.Sort,
                            title = stringResource(R.string.zero_motion),
                            values = motionValues,
                            labels = motionValues.map { stringResource(it.labelRes) },
                            selected = settings.motion,
                            value = stringResource(settings.motion.labelRes),
                            onSelect = { onSettingsChange(settings.copy(motion = it)) },
                            onOpen = { picker = it },
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.zero_interface)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        val glassValues = GlassMode.entries
                        PickerRow(
                            icon = MiuixIcons.Layers,
                            title = stringResource(R.string.zero_glass),
                            values = glassValues,
                            labels = glassValues.map { stringResource(it.labelRes) },
                            selected = settings.glassMode,
                            value = stringResource(settings.glassMode.labelRes),
                            onSelect = { onSettingsChange(settings.copy(glassMode = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        SelectRow(
                            icon = MiuixIcons.ListView,
                            title = stringResource(R.string.zero_list_density),
                            value = stringResource(
                                if (settings.compactList) {
                                    com.zeroui.kit.R.string.list_density_compact
                                } else {
                                    com.zeroui.kit.R.string.list_density_standard
                                },
                            ),
                        ) { onSettingsChange(settings.copy(compactList = !settings.compactList)) }
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Album,
                            title = stringResource(R.string.zero_smooth_corners),
                            checked = settings.smoothCorners,
                        ) { onSettingsChange(settings.copy(smoothCorners = it)) }
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Sort,
                            title = stringResource(R.string.zero_elastic_scroll),
                            checked = settings.elasticScroll,
                        ) { onSettingsChange(settings.copy(elasticScroll = it)) }
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Background,
                            title = stringResource(R.string.zero_top_bar_blur),
                            checked = settings.topBarBlur,
                        ) { onSettingsChange(settings.copy(topBarBlur = it)) }
                        AppDivider()
                        SelectRow(
                            icon = MiuixIcons.Info,
                            title = stringResource(R.string.zero_dialog_position),
                            value = stringResource(
                                if (settings.dialogCentered) {
                                    com.zeroui.kit.R.string.dialog_position_center
                                } else {
                                    com.zeroui.kit.R.string.dialog_position_bottom
                                },
                            ),
                        ) { onSettingsChange(settings.copy(dialogCentered = !settings.dialogCentered)) }
                        AppDivider()
                        val iconValues = com.zeroui.kit.components.SettingIconStyle.entries
                        PickerRow(
                            icon = MiuixIcons.Image,
                            title = stringResource(R.string.zero_icon_style),
                            values = iconValues,
                            labels = iconValues.map { stringResource(it.labelRes) },
                            selected = settings.iconStyle,
                            value = stringResource(settings.iconStyle.labelRes),
                            onSelect = { onSettingsChange(settings.copy(iconStyle = it)) },
                            onOpen = { picker = it },
                        )
                    }
                }

                item {
                    AppCard(
                        modifier = cardModifier(liquidBackdrop),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        AppText(
                            text = stringResource(R.string.zero_ui_scale) + ": ${settings.uiScale}%",
                            style = AppTextStyle.Subtitle,
                        )
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.material3.Slider(
                            value = settings.uiScale.toFloat(),
                            onValueChange = { onSettingsChange(settings.copy(uiScale = it.toInt())) },
                            valueRange = 80f..130f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    AppCard(
                        modifier = cardModifier(liquidBackdrop),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        AppText(
                            text = stringResource(R.string.zero_glass_intensity) +
                                ": ${settings.glassIntensity}%",
                            style = AppTextStyle.Subtitle,
                        )
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.material3.Slider(
                            value = settings.glassIntensity.toFloat(),
                            onValueChange = {
                                onSettingsChange(settings.copy(glassIntensity = it.toInt()))
                            },
                            valueRange = 25f..200f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.pref_navigation_bar)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        PickerRow(
                            icon = MiuixIcons.Settings,
                            title = stringResource(R.string.pref_navigation_bar),
                            summary = stringResource(R.string.pref_navigation_bar_summary),
                            values = NavigationBarStyle.entries,
                            labels = listOf(
                                stringResource(R.string.navigation_bar_monet),
                                stringResource(R.string.navigation_bar_liquid),
                            ),
                            selected = settings.navigationBarStyle,
                            value = stringResource(
                                if (settings.navigationBarStyle == NavigationBarStyle.LIQUID_IOS) {
                                    R.string.navigation_bar_liquid
                                } else {
                                    R.string.navigation_bar_monet
                                },
                            ),
                            onSelect = { onSettingsChange(settings.copy(navigationBarStyle = it)) },
                            onOpen = { picker = it },
                        )
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Background,
                            title = stringResource(R.string.pref_navigation_blur),
                            summary = stringResource(R.string.pref_navigation_blur_summary),
                            checked = settings.navigationBlur,
                        ) { onSettingsChange(settings.copy(navigationBlur = it)) }
                        AppDivider()
                        SwitchRow(
                            icon = MiuixIcons.Rename,
                            title = stringResource(R.string.pref_navigation_ios_labels),
                            summary = stringResource(R.string.pref_navigation_ios_labels_summary),
                            checked = settings.navigationIosLabels,
                        ) { onSettingsChange(settings.copy(navigationIosLabels = it)) }
                    }
                }

                item { AppSectionTitle(stringResource(R.string.section_storage)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        AppRow(
                            title = stringResource(R.string.pref_save_folder),
                            summary = stringResource(R.string.pref_save_folder_summary),
                            leadingIcon = MiuixIcons.File,
                            onClick = { showSaveFolderDialog = true },
                            trailing = {
                                AppText(
                                    text = settings.storagePath,
                                    style = AppTextStyle.Footnote,
                                    color = appPalette().onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.section_updates)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        AppRow(
                            title = stringResource(R.string.pref_check_updates),
                            summary = stringResource(R.string.pref_check_updates_summary),
                            leadingIcon = MiuixIcons.Refresh,
                            enabled = !updateChecking,
                            onClick = onCheckForUpdates,
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_version),
                            leadingIcon = MiuixIcons.Info,
                            trailing = {
                                AppText(
                                    text = BuildConfig.VERSION_NAME,
                                    style = AppTextStyle.Footnote,
                                    color = appPalette().onSurfaceVariant,
                                )
                            },
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.section_about)) }
                item {
                    AppCard(modifier = cardModifier(liquidBackdrop)) {
                        AppRow(
                            title = stringResource(R.string.app_name),
                            summary = stringResource(R.string.about_subtitle),
                            leadingIcon = MiuixIcons.Info,
                            onClick = onOpenAbout,
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_github),
                            summary = stringResource(R.string.pref_github_summary),
                            leadingIcon = MiuixIcons.Community,
                            onClick = { onOpenUrl("https://github.com/soctrungkien") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_telegram),
                            summary = stringResource(R.string.pref_telegram_summary),
                            leadingIcon = MiuixIcons.Send,
                            onClick = { onOpenUrl("https://t.me/realknoname") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_verification),
                            summary = stringResource(R.string.pref_verification_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = { showVerificationInfo = true },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_help),
                            summary = stringResource(R.string.help_title),
                            leadingIcon = MiuixIcons.Help,
                            onClick = onShowHelp,
                        )
                    }
                }
            }
        }
    }

    picker?.let { active ->
        AppSelectDialog(
            title = active.title,
            options = active.options,
            selected = active.selected,
            onSelect = active.onSelect,
            onDismissRequest = { picker = null },
        )
    }

    if (showSaveFolderDialog) {
        AppInputDialog(
            title = stringResource(R.string.save_folder_title),
            message = stringResource(R.string.save_folder_message),
            initialValue = settings.saveFolder,
            placeholder = stringResource(R.string.save_folder_placeholder),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { value ->
                onSettingsChange(settings.copy(saveFolder = normalizeSaveFolder(value)))
                showSaveFolderDialog = false
            },
            onDismissRequest = { showSaveFolderDialog = false },
            dismissLabel = stringResource(R.string.action_cancel),
        )
    }

    if (showSeedColorDialog) {
        AppInputDialog(
            title = stringResource(R.string.zero_seed_color),
            message = stringResource(R.string.zero_seed_color),
            initialValue = settings.seedColor.toString(),
            placeholder = "0xFFF2B8C6",
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { value ->
                val parsed = runCatching {
                    value.trim().removePrefix("#").removePrefix("0x").toLong(16).toInt()
                }.getOrDefault(0)
                onSettingsChange(settings.copy(seedColor = parsed))
                showSeedColorDialog = false
            },
            onDismissRequest = { showSeedColorDialog = false },
            dismissLabel = stringResource(R.string.action_cancel),
        )
    }

    if (showVerificationInfo) {
        AppDialog(
            title = stringResource(R.string.verification_title),
            message = stringResource(R.string.verification_message),
            onDismissRequest = { showVerificationInfo = false },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.action_learn_more),
                    onClick = {
                        showVerificationInfo = false
                        onOpenUrl("https://keepandroidopen.org/")
                    },
                    primary = true,
                )
                AppTextButton(
                    text = stringResource(R.string.action_close),
                    onClick = { showVerificationInfo = false },
                )
            },
        )
    }
}
