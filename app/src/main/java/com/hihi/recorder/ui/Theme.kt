package com.hihi.recorder.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.hihi.recorder.data.MaterialSpec
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecorderTheme
import com.hihi.recorder.data.UiStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

val LocalUiStyle = staticCompositionLocalOf { UiStyle.MIUIX }
val LocalMaterialSpec = staticCompositionLocalOf { MaterialSpec.SPEC_2025 }

@Composable
fun RecorderTheme(
    settings: RecorderSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.theme) {
        RecorderTheme.SYSTEM -> isSystemInDarkTheme()
        RecorderTheme.LIGHT -> false
        RecorderTheme.DARK -> true
    }
    val controller = remember(
        settings.theme,
        settings.monetColor,
        settings.materialSpec,
        dark,
    ) {
        ThemeController(
            colorSchemeMode = when (settings.theme) {
                RecorderTheme.SYSTEM -> if (settings.monetColor) {
                    ColorSchemeMode.MonetSystem
                } else {
                    ColorSchemeMode.System
                }

                RecorderTheme.LIGHT -> if (settings.monetColor) {
                    ColorSchemeMode.MonetLight
                } else {
                    ColorSchemeMode.Light
                }

                RecorderTheme.DARK -> if (settings.monetColor) {
                    ColorSchemeMode.MonetDark
                } else {
                    ColorSchemeMode.Dark
                }
            },
            colorSpec = if (settings.materialSpec == MaterialSpec.SPEC_2025) {
                ThemeColorSpec.Spec2025
            } else {
                ThemeColorSpec.Spec2021
            },
            paletteStyle = ThemePaletteStyle.TonalSpot,
            isDark = dark,
        )
    }

    SystemBarsEffect(dark)

    MiuixTheme(controller = controller) {
        val materialColors = MiuixTheme.colorScheme.toMaterialColors(dark)
        val specification = settings.materialSpec
        MaterialTheme(
            colorScheme = materialColors,
            shapes = if (specification == MaterialSpec.SPEC_2025) ExpressiveShapes else ClassicShapes,
            typography = if (specification == MaterialSpec.SPEC_2025) {
                ExpressiveTypography
            } else {
                Typography()
            },
        ) {
            CompositionLocalProvider(
                LocalUiStyle provides settings.uiStyle,
                LocalMaterialSpec provides specification,
                content = content,
            )
        }
    }
}

private fun Colors.toMaterialColors(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = secondary,
        onTertiary = onSecondary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariantSummary,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surface,
        surfaceContainerLowest = background,
        outline = outline,
        outlineVariant = dividerLine,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        surfaceTint = primary,
        scrim = Color.Black,
    )
}

private val ClassicShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val ExpressiveTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
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
