@file:OptIn(ExperimentalScrollBarApi::class)

package com.hihi.recorder.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hihi.recorder.data.UiStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/** One entry in `assets/credit.json`. Add or remove credits by editing that file only. */
@Serializable
data class CreditEntry(
    val id: String,
    val name: String,
    val author: String = "",
    val licenseId: String,
    val url: String = "",
)

@Serializable
data class CreditData(
    val licenses: Map<String, String> = emptyMap(),
    val credits: List<CreditEntry> = emptyList(),
)

/**
 * Loads the single credit file. No more per-license txt files in assets or
 * res/raw: everything lives in `assets/credit.json` so a credit can be added
 * or removed with one JSON object.
 */
object CreditRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: CreditData? = null

    fun load(context: Context): CreditData = cache ?: run {
        val data = runCatching {
            context.assets.open("credit.json").bufferedReader().use { reader ->
                json.decodeFromString<CreditData>(reader.readText())
            }
        }.getOrDefault(CreditData())
        cache = data
        data
    }
}

/** List of every credit defined in `credit.json`. */
@Composable
fun CreditListPage(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpen: (CreditEntry) -> Unit,
) {
    val context = LocalContext.current
    val credits = remember { CreditRepository.load(context).credits }
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
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = appBottomContentPadding(),
            ),
        ) {
            items(credits, key = { it.id }) { entry ->
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    AppRow(
                        title = entry.name,
                        summary = listOf(entry.author, entry.licenseId)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        leadingIcon = if (entry.url.isBlank()) MiuixIcons.Info else MiuixIcons.File,
                        onClick = { onOpen(entry) },
                    )
                }
            }
        }
    }
}

/** Full license text plus attribution for one credit. */
@Composable
fun CreditDetailPage(
    creditId: String,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val data = remember { CreditRepository.load(context) }
    val entry = remember(data, creditId) { data.credits.firstOrNull { it.id == creditId } }
    val licenseText = remember(data, entry) {
        data.licenses[entry?.licenseId].orEmpty().replace("\r\n", "\n").trim()
    }
    val palette = appPalette()
    val scrollState = rememberScrollState()
    val bottomContentPadding = appBottomContentPadding()

    AppScaffold(
        title = entry?.name ?: stringResource(R.string.licenses_title),
        subtitle = listOfNotNull(entry?.author.takeUnless { it.isNullOrBlank() }, entry?.licenseId)
            .joinToString(" · "),
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
                            text = licenseText.ifBlank {
                                stringResource(R.string.license_not_available)
                            },
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
