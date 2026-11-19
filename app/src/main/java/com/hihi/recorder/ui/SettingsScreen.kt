package com.hihi.recorder.ui

import android.os.Build
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hihi.recorder.BuildConfig
import com.hihi.recorder.R
import com.hihi.recorder.data.MaterialSpec
import com.hihi.recorder.data.NavigationBarStyle
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecorderTheme
import com.hihi.recorder.data.UiStyle
import com.hihi.recorder.data.ToggleStyle
import com.hihi.recorder.data.normalizeSaveFolder
import com.hihi.recorder.ui.liquid.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.BreadcrumbBar
import top.yukonga.miuix.kmp.basic.BreadcrumbItem
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Send

private enum class SettingsPicker {
    THEME,
}

/** Switch row that follows the user's Toggle style preference. */
@Composable
internal fun ToggleRow(
    settings: RecorderSettings,
    backdrop: Backdrop?,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val liquid = settings.toggleStyle == ToggleStyle.LIQUID &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (!liquid || backdrop == null) {
        AppSwitchRow(
            title = title,
            summary = summary,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
        return
    }

    // Never wrap the row in layerBackdrop(backdrop): the LiquidToggle inside
    // samples that same layer, and a layer must not contain the surface that
    // samples it — HWUI would recurse through prepareTreeImpl until the
    // RenderThread stack overflows. The backdrop is recorded by a sibling
    // source layer owned by the screen instead.
    AppRow(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            LiquidToggle(
                selected = { checked },
                onSelect = onCheckedChange,
                enabled = enabled,
                backdrop = backdrop,
                intensity = settings.glassIntensity / 100f,
            )
        },
    )
}

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
    var picker by remember { mutableStateOf<SettingsPicker?>(null) }
    var showSaveFolderDialog by remember { mutableStateOf(false) }
    var showVerificationInfo by remember { mutableStateOf(false) }
    var showKeyColorPicker by remember { mutableStateOf(false) }
    var showPalettePicker by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    val palette = appPalette()
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val bottomContentPadding = appBottomContentPadding()
    val liquidToggles = settings.toggleStyle == ToggleStyle.LIQUID &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // One shared backdrop for every switch. Its source layer below is a
    // SIBLING of the content — never an ancestor of a surface sampling it.
    val toggleBackdrop = rememberLayerBackdrop()

    AppScaffold(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (liquidToggles) {
                // Empty sibling source: recording a solid rect here used to
                // repaint the whole screen background when Liquid was enabled.
                Box(Modifier.fillMaxSize().layerBackdrop(toggleBackdrop))
            }
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
            item { AppSectionTitle(stringResource(R.string.section_appearance)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppSelectRow(
                        title = stringResource(R.string.pref_theme),
                        value = settings.theme.label(),
                        onClick = { picker = SettingsPicker.THEME },
                    )
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.pref_monet),
                        summary = stringResource(R.string.pref_monet_summary),
                        checked = settings.monetColor,
                        enabled = dynamicColorAvailable,
                    ) { onSettingsChange(settings.copy(monetColor = it)) }
                    if (settings.monetColor && dynamicColorAvailable) {
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_monet_key_color),
                            summary = when (settings.monetKeyColor) {
                                0 -> stringResource(R.string.monet_key_wallpaper)
                                1 -> stringResource(R.string.monet_key_background)
                                -1 -> stringResource(R.string.monet_key_custom)
                                else -> String.format("#%06X", settings.monetKeyColor and 0xFFFFFF)
                            },
                            onClick = { showKeyColorPicker = true },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_monet_palette_style),
                            summary = settings.monetPaletteStyle.name,
                            onClick = { showPalettePicker = true },
                        )
                    }
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_glass_intensity) +
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
            item {
                AppCard(contentPadding = PaddingValues(14.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_ui_style),
                        style = AppTextStyle.Subtitle,
                    )
                    Spacer(Modifier.height(3.dp))
                    AppText(
                        text = stringResource(R.string.pref_ui_style_summary),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppSegmentedControl(
                        options = listOf(
                            UiStyle.MIUIX to stringResource(R.string.ui_style_miuix),
                            UiStyle.MATERIAL to stringResource(R.string.ui_style_material),
                        ),
                        selected = settings.uiStyle,
                        onSelect = { onSettingsChange(settings.copy(uiStyle = it)) },
                    )
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(14.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_navigation_bar),
                        style = AppTextStyle.Subtitle,
                    )
                    Spacer(Modifier.height(3.dp))
                    AppText(
                        text = stringResource(R.string.pref_navigation_bar_summary),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppSegmentedControl(
                        options = listOf(
                            NavigationBarStyle.MONET to stringResource(R.string.navigation_bar_monet),
                            NavigationBarStyle.LIQUID_IOS to stringResource(R.string.navigation_bar_liquid),
                        ),
                        selected = settings.navigationBarStyle,
                        onSelect = { onSettingsChange(settings.copy(navigationBarStyle = it)) },
                    )
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.pref_navigation_blur),
                        summary = stringResource(R.string.pref_navigation_blur_summary),
                        checked = settings.navigationBlur,
                    ) { onSettingsChange(settings.copy(navigationBlur = it)) }
                    AppDivider()
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.pref_navigation_ios_labels),
                        summary = stringResource(R.string.pref_navigation_ios_labels_summary),
                        checked = settings.navigationIosLabels,
                    ) { onSettingsChange(settings.copy(navigationIosLabels = it)) }
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(14.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_toggle_style),
                        style = AppTextStyle.Subtitle,
                    )
                    Spacer(Modifier.height(3.dp))
                    AppText(
                        text = stringResource(R.string.pref_toggle_style_summary),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppSegmentedControl(
                        options = listOf(
                            ToggleStyle.UI to stringResource(R.string.toggle_style_ui),
                            ToggleStyle.LIQUID to stringResource(R.string.toggle_style_liquid),
                        ),
                        selected = settings.toggleStyle,
                        onSelect = { onSettingsChange(settings.copy(toggleStyle = it)) },
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.section_storage)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppSelectRow(
                        title = stringResource(R.string.pref_save_folder),
                        summary = stringResource(R.string.pref_save_folder_summary),
                        value = settings.storagePath,
                        onClick = { showSaveFolderDialog = true },
                    )
                    AppDivider()
                    val pathItems = remember(settings.storagePath) {
                        settings.storagePath
                            .split('/')
                            .filter { it.isNotBlank() }
                            .map { BreadcrumbItem(path = it) }
                    }
                    BreadcrumbBar(
                        items = pathItems,
                        onItemClick = { showSaveFolderDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        highlightIndex = pathItems.lastIndex,
                        itemMaxWidth = 160.dp,
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.section_recording)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.pref_haptics),
                        checked = settings.hapticFeedback,
                    ) { onSettingsChange(settings.copy(hapticFeedback = it)) }
                    AppDivider()
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.pref_stop_screen_off),
                        checked = settings.stopOnScreenOff,
                    ) { onSettingsChange(settings.copy(stopOnScreenOff = it)) }
                }
            }

            item { AppSectionTitle(stringResource(R.string.section_updates)) }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_check_updates),
                        style = AppTextStyle.Subtitle,
                    )
                    Spacer(Modifier.height(3.dp))
                    AppText(
                        text = stringResource(R.string.pref_check_updates_summary),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        text = stringResource(R.string.pref_check_updates),
                        onClick = onCheckForUpdates,
                        enabled = !updateChecking,
                        loading = updateChecking,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppRow(
                        title = stringResource(R.string.pref_version),
                        trailing = {
                            AppText(
                                text = BuildConfig.VERSION_NAME,
                                style = AppTextStyle.Footnote,
                                color = palette.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.section_about)) }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
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

    when (picker) {
        SettingsPicker.THEME -> AppSelectDialog(
            title = stringResource(R.string.pref_theme),
            options = RecorderTheme.entries.map { value ->
                SelectOption(value, value.label())
            },
            selected = settings.theme,
            onSelect = { value -> onSettingsChange(settings.copy(theme = value)) },
            onDismissRequest = { picker = null },
        )

        null -> Unit
    }

    if (showKeyColorPicker) {
        val presets = listOf(
            0xFF2196F3.toInt(), 0xFF3F51B5.toInt(), 0xFF9C27B0.toInt(),
            0xFFE91E63.toInt(), 0xFFFF5722.toInt(), 0xFF4CAF50.toInt(),
            0xFF009688.toInt(), 0xFFFFC107.toInt(),
        )
        AppSelectDialog(
            title = stringResource(R.string.pref_monet_key_color),
            options = listOf(
                SelectOption<Any>(0, stringResource(R.string.monet_key_wallpaper)),
                SelectOption<Any>(1, stringResource(R.string.monet_key_background)),
                SelectOption<Any>(-1, stringResource(R.string.monet_key_custom)),
            ) + presets.mapIndexed { index, color ->
                SelectOption<Any>(color, String.format("#%06X", color and 0xFFFFFF) + " ${index + 1}")
            },
            selected = settings.monetKeyColor,
            onSelect = { value ->
                val key = value as Int
                if (key == -1) {
                    showCustomColorDialog = true
                } else {
                    onSettingsChange(settings.copy(monetKeyColor = key))
                }
                showKeyColorPicker = false
            },
            onDismissRequest = { showKeyColorPicker = false },
        )
    }

    if (showPalettePicker) {
        AppSelectDialog(
            title = stringResource(R.string.pref_monet_palette_style),
            options = top.yukonga.miuix.kmp.theme.ThemePaletteStyle.entries.map {
                SelectOption(it, it.name)
            },
            selected = settings.monetPaletteStyle,
            onSelect = {
                onSettingsChange(settings.copy(monetPaletteStyle = it))
                showPalettePicker = false
            },
            onDismissRequest = { showPalettePicker = false },
        )
    }

    if (showCustomColorDialog) {
        AppInputDialog(
            title = stringResource(R.string.glass_custom_color),
            message = stringResource(R.string.glass_color_hint),
            initialValue = String.format("#%06X", settings.monetKeyColor and 0xFFFFFF),
            placeholder = "#FF2196F3",
            confirmLabel = stringResource(R.string.action_save),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = { value ->
                val hex = value.trim().removePrefix("#")
                val parsed = runCatching {
                    if (hex.length == 6) {
                        ("FF" + hex).toLong(16).toInt()
                    } else {
                        hex.toLong(16).toInt()
                    }
                }.getOrNull()
                if (parsed != null) {
                    onSettingsChange(settings.copy(monetKeyColor = parsed))
                }
                showCustomColorDialog = false
            },
            onDismissRequest = { showCustomColorDialog = false },
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
