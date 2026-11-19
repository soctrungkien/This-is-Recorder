
package com.hihi.recorder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.acutecoder.crashhandler.core.ErrorLog
import com.acutecoder.crashhandler.util.crashHandler
import com.hihi.recorder.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * Crash screen shown by the crash handler.
 * Uses a fixed Miuix theme instead of app preferences.
 */
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val dark = isSystemInDarkTheme()
            val controller = remember(dark) {
                ThemeController(
                    colorSchemeMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ColorSchemeMode.MonetSystem
                    } else {
                        ColorSchemeMode.System
                    },
                    colorSpec = ThemeColorSpec.Spec2025,
                    paletteStyle = ThemePaletteStyle.TonalSpot,
                    isDark = dark,
                )
            }

            MiuixTheme(controller = controller) {
                CrashScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
private fun CrashScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var log by remember { mutableStateOf<ErrorLog?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                context.crashHandler.loadErrorLog()
            }.getOrNull()
        }

        log = result
        loaded = true
    }

    AppScaffold(
        title = stringResource(R.string.crash_title),
        subtitle = stringResource(R.string.crash_subtitle),
        snackbarHostState = snackbarHostState,
        navigationIcon = {
            AppIconButton(
                icon = MiuixIcons.Back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onClose,
            )
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            val time = log?.lastErrorTime

            if (!time.isNullOrBlank()) {
                AppText(
                    text = stringResource(R.string.crash_last_time, time),
                    style = AppTextStyle.Footnote,
                    color = appPalette().onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp,
                    ),
                )
            }

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
                contentPadding = PaddingValues(16.dp),
            ) {
                val text = when {
                    !loaded -> stringResource(R.string.crash_loading)
                    log == null -> stringResource(R.string.crash_no_log)
                    else -> log!!.simplifiedLog()
                }

                SelectionContainer {
                    AppText(
                        text = text,
                        style = AppTextStyle.SmallBody,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppButton(
                    text = stringResource(R.string.crash_clear),
                    modifier = Modifier.height(48.dp),
                    onClick = {
                        runCatching {
                            context.crashHandler.clearAll()
                        }
                        onClose()
                    },
                )

                Spacer(Modifier.weight(1f))

                AppTextButton(
                    text = stringResource(R.string.crash_copy),
                    primary = true,
                    modifier = Modifier.height(48.dp),
                    onClick = {
                        val value = log?.simplifiedLog().orEmpty()

                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as? ClipboardManager

                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("Crash log", value),
                        )

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.crash_copied),
                            )
                        }
                    },
                )
            }
        }
    }
}
