@file:OptIn(ExperimentalScrollBarApi::class)

package com.hihi.recorder.ui

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import com.hihi.recorder.data.UiStyle
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/**
 * Licenses bundled with the app and shown as selectable text on [LicensePage].
 *
 * Each entry points to the notice for that specific component. Third-party MIT
 * libraries must have their own attribution text; do not reuse the app's MIT
 * file for them.
 */
enum class LicenseKind(
    @RawRes val rawRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
) {
    MIT(
        rawRes = R.raw.license_app_mit,
        titleRes = R.string.about_license_mit,
        summaryRes = R.string.about_license_mit_summary,
    ),
    APACHE(
        rawRes = R.raw.license_apache_2_0,
        titleRes = R.string.about_license_apache,
        summaryRes = R.string.about_license_apache_summary,
    ),
    SHIZUKU(
        rawRes = R.raw.license_shizuku_mit,
        titleRes = R.string.about_license_shizuku,
        summaryRes = R.string.about_license_shizuku_summary,
    ),
}

/**
 * Full licence text rendered inside the app. The raw resource keeps the
 * original wording and line breaks, and the selection container makes the
 * body copyable without opening a browser.
 */
@Composable
fun LicensePage(
    kind: LicenseKind,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val resources = LocalResources.current
    val palette = appPalette()
    val scrollState = rememberScrollState()
    val bottomContentPadding = appBottomContentPadding()
    val licenseText = remember(kind, resources) {
        resources.openRawResource(kind.rawRes)
            .bufferedReader()
            .use { reader -> reader.readText() }
            .replace("\r\n", "\n")
            .trim()
    }

    AppScaffold(
        title = stringResource(kind.titleRes),
        subtitle = stringResource(kind.summaryRes),
        snackbarHostState = snackbarHostState,
        navigationIcon = {
            AppIconButton(
                icon = MiuixIcons.Back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = bottomContentPadding,
                    ),
            ) {
                AppCard(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    SelectionContainer {
                        AppText(
                            text = licenseText,
                            style = AppTextStyle.SmallBody,
                            color = palette.onSurface,
                        )
                    }
                }
            }

            if (LocalUiStyle.current == UiStyle.MIUIX) {
                VerticalScrollBar(
                    adapter = rememberScrollBarAdapter(scrollState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    trackPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = bottomContentPadding,
                    ),
                )
            }
        }
    }
}
