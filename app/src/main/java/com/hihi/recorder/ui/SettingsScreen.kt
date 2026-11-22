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
import com.hihi.recorder.data.IconStyle
import com.hihi.recorder.data.MaterialSpec
import com.hihi.recorder.data.NavigationBarStyle
import com.hihi.recorder.data.PaletteStyle
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
    ICON_STYLE,
    MATERIAL_SPEC,
    PALETTE_STYLE,
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
    var showCustomColor by remember { mutableStateOf(false) }
    var showSaveFolderDialog by remember { mutableStateOf(false) }
    var showVerificationInfo by remember { mutableStateOf(false) }
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
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(toggleBackdrop),
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
                    AppDivider()
                    AppSelectRow(
                        title = stringResource(R.string.pref_icon_style),
                        value = stringResource(
                            when (settings.iconStyle) {
                                IconStyle.MIUIX -> R.string.icon_style_miuix
                                IconStyle.COUI -> R.string.icon_style_coui
                                IconStyle.LUCIDE -> R.string.icon_style_lucide
                                IconStyle.MATERIAL -> R.string.icon_style_material
                                IconStyle.SOLAR -> R.string.icon_style_solar
                            },
                        ),
                        onClick = { picker = SettingsPicker.ICON_STYLE },
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
                            UiStyle.COUI to stringResource(R.string.ui_style_coui),
                        ),
                        selected = settings.uiStyle,
                        onSelect = { onSettingsChange(settings.copy(uiStyle = it)) },
                    )
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.pref_monet),
                        summary = stringResource(
                            if (dynamicColorAvailable) R.string.pref_monet_summary
                            else R.string.pref_monet_unavailable,
                        ),
                        checked = settings.monetColor,
                        enabled = dynamicColorAvailable,
                    ) { onSettingsChange(settings.copy(monetColor = it)) }
                    AppDivider()
                    AppSelectRow(
                        title = stringResource(R.string.pref_material_spec),
                        value = settings.materialSpec.label(),
                        onClick = { picker = SettingsPicker.MATERIAL_SPEC },
                    )
                    AppDivider()
                    AppSelectRow(
                        title = stringResource(R.string.monet_palette_style),
                        value = PaletteStyleLabel(settings.monetPalette),
                        onClick = { picker = SettingsPicker.PALETTE_STYLE },
                    )
                }
                AppCard(contentPadding = PaddingValues(top = 12.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_monet_key_color),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = AppTextStyle.Subtitle,
                    )
                    AppText(
                        text = stringResource(R.string.pref_monet_key_color_summary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                    MonetKeyColorRow(
                        selected = settings.monetKeyColor,
                        enabled = settings.monetColor && dynamicColorAvailable,
                        onSelect = { onSettingsChange(settings.copy(monetKeyColor = it)) },
                        onCustom = { showCustomColor = true },
                    )
                }
            }
            item {
                val liquidGlass = settings.toggleStyle == ToggleStyle.LIQUID &&
                    settings.navigationBarStyle == NavigationBarStyle.LIQUID_IOS
                AppCard(contentPadding = PaddingValues(14.dp)) {
                    AppText(
                        text = stringResource(R.string.pref_liquid_glass),
                        style = AppTextStyle.Subtitle,
                    )
                    Spacer(Modifier.height(3.dp))
                    AppText(
                        text = stringResource(R.string.pref_liquid_glass_summary),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppSegmentedControl(
                        options = listOf(
                            false to stringResource(R.string.liquid_off),
                            true to stringResource(R.string.liquid_on),
                        ),
                        selected = liquidGlass,
                        onSelect = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    toggleStyle = if (enabled) ToggleStyle.LIQUID else ToggleStyle.UI,
                                    navigationBarStyle = if (enabled) {
                                        NavigationBarStyle.LIQUID_IOS
                                    } else {
                                        NavigationBarStyle.MONET
                                    },
                                ),
                            )
                        },
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

        SettingsPicker.ICON_STYLE -> AppSelectDialog(
            title = stringResource(R.string.pref_icon_style),
            options = IconStyle.entries.map { value ->
                SelectOption(
                    value,
                    stringResource(
                        when (value) {
                            IconStyle.MIUIX -> R.string.icon_style_miuix
                            IconStyle.COUI -> R.string.icon_style_coui
                            IconStyle.LUCIDE -> R.string.icon_style_lucide
                            IconStyle.MATERIAL -> R.string.icon_style_material
                            IconStyle.SOLAR -> R.string.icon_style_solar
                        },
                    ),
                )
            },
            selected = settings.iconStyle,
            onSelect = { value -> onSettingsChange(settings.copy(iconStyle = value)) },
            onDismissRequest = { picker = null },
        )

        SettingsPicker.MATERIAL_SPEC -> AppSelectDialog(
            title = stringResource(R.string.pref_material_spec),
            options = MaterialSpec.entries.map { value -> SelectOption(value, value.label()) },
            selected = settings.materialSpec,
            onSelect = { value -> onSettingsChange(settings.copy(materialSpec = value)) },
            onDismissRequest = { picker = null },
        )

        SettingsPicker.PALETTE_STYLE -> AppSelectDialog(
            title = stringResource(R.string.monet_palette_style),
            options = PaletteStyle.entries.map { value ->
                SelectOption(value, PaletteStyleLabel(value))
            },
            selected = settings.monetPalette,
            onSelect = { value -> onSettingsChange(settings.copy(monetPalette = value)) },
            onDismissRequest = { picker = null },
        )

        null -> Unit
    }

    if (showCustomColor) {
        CustomMonetColorDialog(
            initial = initialCustomColor(settings.monetKeyColor),
            onDismiss = { showCustomColor = false },
            onConfirm = { argb ->
                showCustomColor = false
                onSettingsChange(settings.copy(monetKeyColor = argb))
            },
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
