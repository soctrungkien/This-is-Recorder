package com.hihi.recorder.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hihi.recorder.data.RecorderTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun RecorderTheme(
    theme: RecorderTheme,
    content: @Composable () -> Unit,
) {
    val controller = remember(theme) {
        ThemeController(
            colorSchemeMode = when (theme) {
                RecorderTheme.SYSTEM -> ColorSchemeMode.System
                RecorderTheme.LIGHT -> ColorSchemeMode.Light
                RecorderTheme.DARK -> ColorSchemeMode.Dark
            },
        )
    }
    val dark = when (theme) {
        RecorderTheme.SYSTEM -> isSystemInDarkTheme()
        RecorderTheme.LIGHT -> false
        RecorderTheme.DARK -> true
    }
    SystemBarsEffect(dark)
    MiuixTheme(controller = controller, content = content)
}

@Composable
private fun SystemBarsEffect(dark: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
