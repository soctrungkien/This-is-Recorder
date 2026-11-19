package com.hihi.recorder.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import kotlin.system.exitProcess

/** Easter-egg screen: every local component and dialog in one place. */
@Composable
fun ShowcaseScreen(
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
                        title = stringResource(R.string.showcase_switches),
                        checked = demoSwitch,
                    ) { demoSwitch = it }
                    AppDivider()
                    ToggleRow(
                        settings = settings,
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
