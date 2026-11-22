package com.hihi.recorder.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import com.itos.coui.checkbox.Checkbox as CouiCheckbox
import com.itos.coui.radiobutton.RadioButton as CouiRadioButton
import com.itos.coui.segmentbutton.SegmentedControl as CouiSegmentedControl
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.icons.CouiIcons
import com.itos.coui.switch.Switch as CouiSwitch
import compose.icons.LucideIcons
import compose.icons.lucideicons.House as LucideHouse
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture
import kotlin.system.exitProcess

/** Easter-egg screen: every local component and dialog in one place. */
@Composable
fun TestScreen(
    settings: com.hihi.recorder.data.RecorderSettings,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onPreviewUpdate: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }
    var showSelect by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var taps by remember { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf("A") }
    var demoSwitch by remember { mutableStateOf(true) }
    var couiSwitch by remember { mutableStateOf(false) }
    var couiSlider by remember { mutableStateOf(0.4f) }
    var couiCheck by remember { mutableStateOf(true) }
    var couiRadio by remember { mutableStateOf(true) }
    var couiSegment by remember { mutableIntStateOf(0) }
    var demoSlider by remember { mutableStateOf(0.5f) }
    var demoInput by remember { mutableStateOf("") }
    var demoCheck by remember { mutableStateOf(false) }
    var demoRadio by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val palette = appPalette()
    val message = stringResource(R.string.showcase_dialog_message)

    AppScaffold(
        title = stringResource(R.string.showcase_title),
        subtitle = stringResource(R.string.showcase_subtitle),
        snackbarHostState = snackbarHostState,
        navigationIcon = {
            AppIconButton(
                icon = MiuixIcons.Back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
        },
    ) { padding ->
        val liquidToggles = settings.toggleStyle == com.hihi.recorder.data.ToggleStyle.LIQUID &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        val toggleBackdrop = rememberLayerBackdrop()
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
                bottom = appBottomContentPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { AppSectionTitle(stringResource(R.string.showcase_buttons)) }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppButton(
                            text = stringResource(R.string.showcase_normal_button),
                            onClick = { taps++ },
                        )
                        Spacer(Modifier.width(8.dp))
                        AppTextButton(
                            text = stringResource(R.string.showcase_primary_button),
                            onClick = { taps++ },
                            primary = true,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppButton(
                            text = stringResource(R.string.showcase_disabled_button),
                            onClick = {},
                            enabled = false,
                        )
                        Spacer(Modifier.width(8.dp))
                        AppText(
                            text = stringResource(R.string.action_taps, taps),
                            style = AppTextStyle.Footnote,
                            color = palette.onSurfaceVariant,
                        )
                    }
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_text_styles)) }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.Display)
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.Headline)
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.Title)
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.Subtitle)
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.Body)
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.SmallBody)
                    AppText(stringResource(R.string.showcase_title), style = AppTextStyle.Footnote)
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_switches)) }
            item {
                AppCard {
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.showcase_switches),
                        checked = demoSwitch,
                    ) { demoSwitch = it }
                    AppDivider()
                    ToggleRow(
                        settings = settings,
                        backdrop = toggleBackdrop,
                        title = stringResource(R.string.showcase_demo_value),
                        summary = demoSwitch.toString(),
                        checked = demoSwitch,
                    ) { demoSwitch = it }
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_cards)) }
            item {
                AppCard {
                    AppRow(
                        title = stringResource(R.string.showcase_demo_value),
                        summary = message,
                        leadingIcon = MiuixIcons.File,
                        onClick = { showInfo = true },
                    )
                    AppDivider()
                    AppRow(
                        title = stringResource(R.string.showcase_markdown),
                        summary = message,
                        leadingIcon = MiuixIcons.Refresh,
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_dialogs)) }
            item {
                AppCard {
                    AppRow(
                        title = stringResource(R.string.showcase_preview_update),
                        leadingIcon = MiuixIcons.Refresh,
                        onClick = onPreviewUpdate,
                    )
                    AppDivider()
                    AppRow(
                        title = stringResource(R.string.showcase_preview_confirm),
                        leadingIcon = MiuixIcons.Info,
                        onClick = { showConfirm = true },
                    )
                    AppDivider()
                    AppRow(
                        title = stringResource(R.string.showcase_preview_input),
                        leadingIcon = MiuixIcons.File,
                        onClick = { showInput = true },
                    )
                    AppDivider()
                    AppRow(
                        title = stringResource(R.string.showcase_preview_select),
                        leadingIcon = MiuixIcons.Info,
                        onClick = { showSelect = true },
                    )
                    AppDivider()
                    AppRow(
                        title = stringResource(R.string.showcase_preview_update),
                        summary = message,
                        leadingIcon = MiuixIcons.Info,
                        onClick = { showInfo = true },
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_feedback)) }
            item {
                AppCard {
                    AppRow(
                        title = stringResource(R.string.showcase_show_snackbar),
                        leadingIcon = MiuixIcons.Info,
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
            }

            item { AppSectionTitle("Controls") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    AppText("Segmented", style = AppTextStyle.Subtitle)
                    Spacer(Modifier.height(8.dp))
                    AppSegmentedControl(
                        options = listOf("One" to "One", "Two" to "Two", "Three" to "Three"),
                        selected = "One",
                        onSelect = {},
                    )
                    Spacer(Modifier.height(18.dp))
                    AppText("Slider: ${(demoSlider * 100).toInt()}%", style = AppTextStyle.Subtitle)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.Slider(
                        value = demoSlider,
                        onValueChange = { demoSlider = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppSpinner()
                        Spacer(Modifier.width(10.dp))
                        AppText("Loading indicator", style = AppTextStyle.SmallBody)
                    }
                }
            }

            item { AppSectionTitle("Theme colors") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    val colors = listOf(
                        "primary" to palette.primary,
                        "onPrimary" to palette.onPrimary,
                        "primaryContainer" to palette.primaryContainer,
                        "surface" to palette.surface,
                        "surfaceContainer" to palette.surfaceContainer,
                        "onSurface" to palette.onSurface,
                        "onSurfaceVariant" to palette.onSurfaceVariant,
                        "error" to palette.error,
                        "outline" to palette.outline,
                    )
                    colors.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (name, color) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    AppText(name, style = AppTextStyle.Footnote)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            item { AppSectionTitle("Input") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    OutlinedTextField(
                        value = demoInput,
                        onValueChange = { demoInput = it },
                        label = { AppText("Demo text field") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { AppSectionTitle("Selection") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = demoCheck, onCheckedChange = { demoCheck = it })
                        AppText("Checkbox: $demoCheck", style = AppTextStyle.SmallBody)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = demoRadio == 0, onClick = { demoRadio = 0 })
                        AppText("Option A", style = AppTextStyle.SmallBody)
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = demoRadio == 1, onClick = { demoRadio = 1 })
                        AppText("Option B", style = AppTextStyle.SmallBody)
                    }
                }
            }

            item { AppSectionTitle("Progress") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    LinearProgressIndicator(
                        progress = { demoSlider },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            item { AppSectionTitle("Icon packs") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    val icons = listOf(
                        "Miuix" to MiuixIcons.ScreenCapture,
                        "COUI" to CouiIcons.Outlined.Camera,
                        "Lucide" to LucideIcons.LucideHouse,
                        "Material" to Icons.Filled.Home,
                        "Solar" to com.hihi.recorder.ui.icons.SolarIcons.Home,
                    )
                    icons.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (name, icon) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AppIcon(icon = icon, contentDescription = name)
                                    Spacer(Modifier.height(4.dp))
                                    AppText(name, style = AppTextStyle.Footnote)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            item { AppSectionTitle("COUI components") }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText("COUI Switch", style = AppTextStyle.Body, modifier = Modifier.weight(1f))
                        CouiSwitch(
                            checked = couiSwitch,
                            onCheckedChange = { couiSwitch = it },
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    AppText("COUI Slider", style = AppTextStyle.Body)
                    Spacer(Modifier.height(10.dp))
                    CouiSlider(
                        value = couiSlider,
                        onValueChange = { couiSlider = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CouiCheckbox(checked = couiCheck)
                        Spacer(Modifier.width(8.dp))
                        AppText(
                            text = "COUI Checkbox: $couiCheck",
                            style = AppTextStyle.SmallBody,
                            modifier = Modifier.clickable { couiCheck = !couiCheck },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CouiRadioButton(selected = couiRadio)
                        Spacer(Modifier.width(8.dp))
                        AppText(
                            text = "COUI Radio: $couiRadio",
                            style = AppTextStyle.SmallBody,
                            modifier = Modifier.clickable { couiRadio = !couiRadio },
                        )
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        Spacer(Modifier.height(18.dp))
                        CouiSegmentedControl(
                            labels = listOf("A", "B", "C"),
                            selectedIndex = couiSegment,
                            onSelected = { couiSegment = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_test_title)) }
            item {
                val context = LocalContext.current
                AppCard {
                    AppRow(
                        title = stringResource(R.string.showcase_force_crash),
                        summary = stringResource(R.string.showcase_force_crash_summary),
                        leadingIcon = MiuixIcons.Info,
                        onClick = {
                            throw RuntimeException("Test crash from component showcase")
                        },
                    )
                    AppDivider()
                    AppRow(
                        title = stringResource(R.string.showcase_force_stop),
                        summary = stringResource(R.string.showcase_force_stop_summary),
                        leadingIcon = MiuixIcons.Back,
                        onClick = {
                            (context as? Activity)?.finishAffinity()
                            exitProcess(0)
                        },
                    )
                }
            }

            item { AppSectionTitle(stringResource(R.string.showcase_markdown)) }
            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    MarkdownText(
                        text = """
                            ## Heading

                            - **Bold**, *italic* and `code`
                            - [A link](https://github.com/soctrungkien/This-is-Recorder)

                            > Quoted text renders with an accent.
                        """.trimIndent(),
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
            }
        }
    }

    if (showConfirm) {
        AppConfirmDialog(
            title = stringResource(R.string.showcase_preview_confirm),
            message = message,
            confirmLabel = stringResource(R.string.action_save),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = { showConfirm = false },
            onDismissRequest = { showConfirm = false },
        )
    }

    if (showInput) {
        AppInputDialog(
            title = stringResource(R.string.showcase_preview_input),
            message = message,
            initialValue = stringResource(R.string.showcase_demo_value),
            placeholder = stringResource(R.string.showcase_demo_value),
            confirmLabel = stringResource(R.string.action_save),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = { _ -> showInput = false },
            onDismissRequest = { showInput = false },
        )
    }

    if (showSelect) {
        AppSelectDialog(
            title = stringResource(R.string.showcase_preview_select),
            options = listOf(
                SelectOption("A", "Option A"),
                SelectOption("B", "Option B"),
                SelectOption("C", "Option C"),
            ),
            selected = selection,
            onSelect = { selection = it },
            onDismissRequest = { showSelect = false },
        )
    }

    if (showInfo) {
        AppDialog(
            title = stringResource(R.string.showcase_title),
            message = message,
            onDismissRequest = { showInfo = false },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.action_close),
                    onClick = { showInfo = false },
                    primary = true,
                )
            },
        )
    }
}
