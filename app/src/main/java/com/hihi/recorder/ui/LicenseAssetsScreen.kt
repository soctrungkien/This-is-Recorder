@file:OptIn(ExperimentalScrollBarApi::class)

package com.hihi.recorder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

data class LicenseAsset(
    val file: String,
    val titleRes: Int,
)

private val LicenseAssets = listOf(
    LicenseAsset("app-mit.txt", R.string.license_app),
    LicenseAsset("apache-2.0.txt", R.string.license_miuix),
    LicenseAsset("apache-2.0.txt", R.string.license_zeroui),
    LicenseAsset("apache-2.0.txt", R.string.license_kyant),
    LicenseAsset("apache-2.0.txt", R.string.license_markdown),
    LicenseAsset("apache-2.0.txt", R.string.license_material_icons),
    LicenseAsset("crashhandler-gpl-3.0.txt", R.string.license_crashhandler),
    LicenseAsset("coui-mpl-2.0.txt", R.string.license_coui),
    LicenseAsset("lucide-isc.txt", R.string.license_lucide),
    LicenseAsset("solar-cc-by-4.0.txt", R.string.license_solar),
    LicenseAsset("shizuku-mit.txt", R.string.license_shizuku),
)

/** List of all bundled licenses. */
@Composable
fun LicenseListPage(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpen: (LicenseAsset) -> Unit,
) {
    AppScaffold(
        title = stringResource(R.string.licenses_title),
        subtitle = stringResource(R.string.licenses_subtitle),
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
                top = padding.calculateTopPadding() + 8.dp,
                bottom = appBottomContentPadding(),
            ),
        ) {
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    LicenseAssets.forEachIndexed { index, entry ->
                        if (index > 0) AppDivider()
                        AppRow(
                            title = stringResource(entry.titleRes),
                            summary = entry.file,
                            leadingIcon = MiuixIcons.File,
                            onClick = { onOpen(entry) },
                        )
                    }
                }
            }
        }
    }
}

/** Full text of a bundled license, read from assets/licenses. */
@Composable
fun AssetLicensePage(
    asset: LicenseAsset,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(asset.file) {
        runCatching {
            context.assets.open("licenses/${asset.file}").bufferedReader().use { it.readText() }
        }.getOrDefault(context.getString(R.string.crash_no_log))
    }
    val scrollState = rememberScrollState()

    AppScaffold(
        title = stringResource(asset.titleRes),
        subtitle = asset.file,
        snackbarHostState = remember { SnackbarHostState() },
        navigationIcon = {
            AppIconButton(
                icon = MiuixIcons.Back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SelectionContainer {
                AppText(
                    text = text,
                    style = AppTextStyle.Footnote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = appBottomContentPadding(),
                        ),
                )
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxSize(),
            )
        }
    }
}
