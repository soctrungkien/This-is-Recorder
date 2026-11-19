package com.hihi.recorder.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.zeroui.kit.components.ArrowPreference
import com.zeroui.kit.components.Button
import com.zeroui.kit.components.DropdownPreference
import com.zeroui.kit.components.ListCard
import com.zeroui.kit.components.ListSection
import com.zeroui.kit.components.PreferenceIcon
import com.zeroui.kit.components.SettingIconStyle
import com.zeroui.kit.components.SettingIcons
import com.zeroui.kit.components.SwitchPreference
import com.zeroui.kit.components.Text
import com.zeroui.kit.glass.LiquidSlider
import com.zeroui.kit.theme.DialogPosition
import com.zeroui.kit.theme.GlassMode
import com.zeroui.kit.theme.ListDensity
import com.zeroui.kit.theme.MotionMode
import com.zeroui.kit.theme.PaletteStyle
import top.yukonga.miuix.kmp.basic.SnackbarHostState

private val SectionModifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)

/** Single-choice row backed by a kit dropdown. */
@Composable
private fun <T> ChoiceRow(
    title: String,
    values: List<T>,
    labels: List<String>,
    selected: T,
    onSelect: (T) -> Unit,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
) {
    DropdownPreference(
        title = title,
        summary = summary,
        items = labels,
        selectedIndex = values.indexOf(selected).coerceAtLeast(0),
        onSelectedIndexChange = { index -> onSelect(values[index]) },
        startAction = startAction,
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    ListCard(SectionModifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(text = label)
            LiquidSlider(
                value = { value },
                onValueChange = onChange,
                valueRange = range,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/**
 * Full settings page built from the ZeroUI kit. Every row writes straight into
 * [RecorderSettings] through [onSettingsChange], so the values persist and take
 * effect immediately.
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
    var showSaveFolderDialog by remember { mutableStateOf(false) }
    var showSeedColorDialog by remember { mutableStateOf(false) }
    var showVerificationInfo by remember { mutableStateOf(false) }
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val bottomPadding = appBottomContentPadding()

    AppScaffold(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
            snackbarHostState = snackbarHostState,
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = bottomPadding,
                ),
            ) {
                item {
                    ListSection(stringResource(R.string.zero_capture)) {
                        ChoiceRow(
                            title = stringResource(R.string.pref_capture_mode),
                            values = CaptureMode.entries,
                            labels = CaptureMode.entries.map { it.label() },
                            selected = settings.captureMode,
                            onSelect = { onSettingsChange(settings.copy(captureMode = it)) },
                            startAction = { PreferenceIcon(SettingIcons.tune) },
                        )
                        if (settings.captureMode == CaptureMode.SHELL) {
                            ChoiceRow(
                                title = stringResource(R.string.pref_shell_backend),
                                values = ShellBackend.entries,
                                labels = listOf("ADB", "Shizuku", "Root"),
                                selected = settings.shellBackend,
                                onSelect = { onSettingsChange(settings.copy(shellBackend = it)) },
                            )
                        }
                    }
                }

                item {
                    ListSection(stringResource(R.string.zero_audio)) {
                        ChoiceRow(
                            title = stringResource(R.string.pref_audio_source),
                            values = AudioSource.entries,
                            labels = AudioSource.entries.map { it.label() },
                            selected = settings.audioSource,
                            onSelect = { onSettingsChange(settings.copy(audioSource = it)) },
                            summary = settings.audioSource.summary(),
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.zero_video)) {
                        ChoiceRow(
                            title = stringResource(R.string.pref_resolution),
                            values = VideoResolution.entries,
                            labels = VideoResolution.entries.map { it.label() },
                            selected = settings.resolution,
                            onSelect = { onSettingsChange(settings.copy(resolution = it)) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.pref_frame_rate),
                            values = VideoFrameRate.entries,
                            labels = VideoFrameRate.entries.map { it.label() },
                            selected = settings.frameRate,
                            onSelect = { onSettingsChange(settings.copy(frameRate = it)) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.pref_bitrate),
                            values = VideoBitrate.entries,
                            labels = VideoBitrate.entries.map { it.label() },
                            selected = settings.bitrate,
                            onSelect = { onSettingsChange(settings.copy(bitrate = it)) },
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.zero_timer)) {
                        ChoiceRow(
                            title = stringResource(R.string.pref_countdown),
                            values = Countdown.entries,
                            labels = Countdown.entries.map { it.label() },
                            selected = settings.countdown,
                            onSelect = { onSettingsChange(settings.copy(countdown = it)) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.pref_auto_stop),
                            values = AutoStop.entries,
                            labels = AutoStop.entries.map { it.label() },
                            selected = settings.autoStop,
                            onSelect = { onSettingsChange(settings.copy(autoStop = it)) },
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.zero_behavior)) {
                        SwitchPreference(
                            title = stringResource(R.string.pref_haptics),
                            checked = settings.hapticFeedback,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(hapticFeedback = it))
                            },
                            startAction = { PreferenceIcon(SettingIcons.haptics) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.pref_stop_screen_off),
                            checked = settings.stopOnScreenOff,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(stopOnScreenOff = it))
                            },
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.section_appearance)) {
                        ChoiceRow(
                            title = stringResource(R.string.pref_theme),
                            values = RecorderTheme.entries,
                            labels = RecorderTheme.entries.map { it.label() },
                            selected = settings.theme,
                            onSelect = { onSettingsChange(settings.copy(theme = it)) },
                            summary = stringResource(R.string.zero_theme_sub),
                            startAction = { PreferenceIcon(SettingIcons.theme) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.pref_ui_style),
                            values = UiStyle.entries,
                            labels = UiStyle.entries.map { it.label() },
                            selected = settings.uiStyle,
                            onSelect = { onSettingsChange(settings.copy(uiStyle = it)) },
                            summary = stringResource(R.string.pref_ui_style_summary),
                            startAction = { PreferenceIcon(SettingIcons.uiStyle) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.pref_material_spec),
                            values = MaterialSpec.entries,
                            labels = MaterialSpec.entries.map { it.label() },
                            selected = settings.materialSpec,
                            onSelect = { onSettingsChange(settings.copy(materialSpec = it)) },
                            startAction = { PreferenceIcon(SettingIcons.material) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.pref_monet),
                            summary = stringResource(R.string.pref_monet_summary),
                            checked = settings.monetColor,
                            enabled = dynamicColorAvailable,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(monetColor = it))
                            },
                            startAction = { PreferenceIcon(SettingIcons.monet) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.zero_pure_black),
                            checked = settings.pureBlack,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(pureBlack = it))
                            },
                            startAction = { PreferenceIcon(SettingIcons.pureBlack) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.zero_palette),
                            values = PaletteStyle.entries,
                            labels = PaletteStyle.entries.map { it.name },
                            selected = settings.palette,
                            onSelect = { onSettingsChange(settings.copy(palette = it)) },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.zero_seed_color),
                            summary = settings.seedColor.toString(),
                            onClick = { showSeedColorDialog = true },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.zero_motion),
                            values = MotionMode.entries,
                            labels = MotionMode.entries.map { stringResource(it.labelRes) },
                            selected = settings.motion,
                            onSelect = { onSettingsChange(settings.copy(motion = it)) },
                            startAction = { PreferenceIcon(SettingIcons.reduceMotion) },
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.zero_interface)) {
                        ChoiceRow(
                            title = stringResource(R.string.zero_glass),
                            values = GlassMode.entries,
                            labels = GlassMode.entries.map { stringResource(it.labelRes) },
                            selected = settings.glassMode,
                            onSelect = { onSettingsChange(settings.copy(glassMode = it)) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.zero_list_density),
                            values = ListDensity.entries,
                            labels = ListDensity.entries.map { stringResource(it.labelRes) },
                            selected = if (settings.compactList) ListDensity.Compact else ListDensity.Standard,
                            onSelect = {
                                onSettingsChange(settings.copy(compactList = it == ListDensity.Compact))
                            },
                            startAction = { PreferenceIcon(SettingIcons.listDensity) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.zero_smooth_corners),
                            checked = settings.smoothCorners,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(smoothCorners = it))
                            },
                            startAction = { PreferenceIcon(SettingIcons.smoothCorners) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.zero_elastic_scroll),
                            checked = settings.elasticScroll,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(elasticScroll = it))
                            },
                            startAction = { PreferenceIcon(SettingIcons.elasticScroll) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.zero_top_bar_blur),
                            checked = settings.topBarBlur,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(topBarBlur = it))
                            },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.zero_dialog_position),
                            values = DialogPosition.entries,
                            labels = DialogPosition.entries.map { stringResource(it.labelRes) },
                            selected = if (settings.dialogCentered) DialogPosition.Center else DialogPosition.Bottom,
                            onSelect = {
                                onSettingsChange(settings.copy(dialogCentered = it == DialogPosition.Center))
                            },
                            startAction = { PreferenceIcon(SettingIcons.dialogPosition) },
                        )
                        ChoiceRow(
                            title = stringResource(R.string.zero_icon_style),
                            values = SettingIconStyle.entries,
                            labels = SettingIconStyle.entries.map { stringResource(it.labelRes) },
                            selected = settings.iconStyle,
                            onSelect = { onSettingsChange(settings.copy(iconStyle = it)) },
                        )
                    }
                }

                item {
                    SliderRow(
                        label = stringResource(R.string.zero_ui_scale) + ": ${settings.uiScale}%",
                        value = settings.uiScale.toFloat(),
                        range = 80f..130f,
                        onChange = { onSettingsChange(settings.copy(uiScale = it.toInt())) },
                    )
                }
                item {
                    SliderRow(
                        label = stringResource(R.string.zero_glass_intensity) + ": ${settings.glassIntensity}%",
                        value = settings.glassIntensity.toFloat(),
                        range = 25f..200f,
                        onChange = { onSettingsChange(settings.copy(glassIntensity = it.toInt())) },
                    )
                }

                item {
                    ListSection(stringResource(R.string.pref_navigation_bar)) {
                        ChoiceRow(
                            title = stringResource(R.string.pref_navigation_bar),
                            values = NavigationBarStyle.entries,
                            labels = listOf(
                                stringResource(R.string.navigation_bar_monet),
                                stringResource(R.string.navigation_bar_liquid),
                            ),
                            selected = settings.navigationBarStyle,
                            onSelect = {
                                onSettingsChange(settings.copy(navigationBarStyle = it))
                            },
                            summary = stringResource(R.string.pref_navigation_bar_summary),
                        )
                        SwitchPreference(
                            title = stringResource(R.string.pref_navigation_blur),
                            summary = stringResource(R.string.pref_navigation_blur_summary),
                            checked = settings.navigationBlur,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(navigationBlur = it))
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.pref_navigation_ios_labels),
                            summary = stringResource(R.string.pref_navigation_ios_labels_summary),
                            checked = settings.navigationIosLabels,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(navigationIosLabels = it))
                            },
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.section_storage)) {
                        ArrowPreference(
                            title = stringResource(R.string.pref_save_folder),
                            summary = settings.storagePath,
                            onClick = { showSaveFolderDialog = true },
                            startAction = { PreferenceIcon(SettingIcons.layers) },
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.section_updates)) {
                        ListCard(SectionModifier) {
                            Button(
                                onClick = onCheckForUpdates,
                                enabled = !updateChecking,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                Text(text = stringResource(R.string.pref_check_updates))
                            }
                        }
                        ArrowPreference(
                            title = stringResource(R.string.pref_version),
                            summary = BuildConfig.VERSION_NAME,
                        )
                    }
                }

                item {
                    ListSection(stringResource(R.string.section_about)) {
                        ArrowPreference(
                            title = stringResource(R.string.app_name),
                            summary = stringResource(R.string.about_subtitle),
                            onClick = onOpenAbout,
                            startAction = { PreferenceIcon(SettingIcons.about) },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.pref_github),
                            summary = stringResource(R.string.pref_github_summary),
                            onClick = { onOpenUrl("https://github.com/soctrungkien") },
                            startAction = { PreferenceIcon(SettingIcons.github) },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.pref_telegram),
                            summary = stringResource(R.string.pref_telegram_summary),
                            onClick = { onOpenUrl("https://t.me/realknoname") },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.pref_verification),
                            summary = stringResource(R.string.pref_verification_summary),
                            onClick = { showVerificationInfo = true },
                            startAction = { PreferenceIcon(SettingIcons.about) },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.pref_help),
                            summary = stringResource(R.string.help_title),
                            onClick = onShowHelp,
                            startAction = { PreferenceIcon(SettingIcons.about) },
                        )
                    }
                }
            }
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
