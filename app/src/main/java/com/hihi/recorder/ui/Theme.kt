package com.hihi.recorder.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
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
import com.hihi.recorder.data.PaletteStyle
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecorderTheme
import com.hihi.recorder.data.UiStyle
import com.itos.coui.theme.CouiColorScheme
import com.itos.coui.theme.CouiPresets
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.CouiTypography
import com.itos.coui.theme.LocalCouiDarkTheme
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
    // Dynamic color is only available on Android 12+. A restored preference
    // from a newer device must not force a Monet controller on older systems.
    val useMonet = settings.monetColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val keyColor = if (useMonet && settings.monetKeyColor != 0) {
        Color(settings.monetKeyColor)
    } else {
        null
    }
    val controller = remember(
        settings.theme,
        useMonet,
        settings.materialSpec,
        settings.monetKeyColor,
        settings.monetPalette,
        dark,
    ) {
        ThemeController(
            colorSchemeMode = when (settings.theme) {
                RecorderTheme.SYSTEM -> if (useMonet) {
                    ColorSchemeMode.MonetSystem
                } else {
                    ColorSchemeMode.System
                }

                RecorderTheme.LIGHT -> if (useMonet) {
                    ColorSchemeMode.MonetLight
                } else {
                    ColorSchemeMode.Light
                }

                RecorderTheme.DARK -> if (useMonet) {
                    ColorSchemeMode.MonetDark
                } else {
                    ColorSchemeMode.Dark
                }
            },
            keyColor = keyColor,
            colorSpec = if (settings.materialSpec == MaterialSpec.SPEC_2025) {
                ThemeColorSpec.Spec2025
            } else {
                ThemeColorSpec.Spec2021
            },
            paletteStyle = if (keyColor != null) {
                settings.monetPalette.toMiuix()
            } else {
                ThemePaletteStyle.TonalSpot
            },
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
            val themedContent: @Composable () -> Unit = {
                CompositionLocalProvider(
                    LocalUiStyle provides settings.uiStyle,
                    LocalMaterialSpec provides specification,
                    LocalCouiDarkTheme provides dark,
                    content = content,
                )
            }
            // COUI must render with its native palette/typography. The global
            // CouiMaterial3Style only applies when no local CouiTheme exists.
            if (settings.uiStyle == UiStyle.COUI) {
                // Keep Monet dynamic colors: use COUI's neutral native surfaces
                // and only take the accent from the host palette. Feeding every
                // M3 surface into COUI would make it look like Material 3.
                val m3 = MaterialTheme.colorScheme
                val preset = if (dark) CouiPresets.Dark else CouiPresets.Light
                val accent = if (useMonet) m3.primary else preset.accent
                val couiColors = CouiColorScheme(
                    accent = accent,
                    onAccent = if (useMonet) m3.onPrimary else preset.onAccent,
                    surface = preset.surface,
                    container = preset.container,
                    onSurface = preset.onSurface,
                    onSurfaceVariant = preset.onSurfaceVariant,
                    outlineVariant = preset.outlineVariant,
                    accentOnSurface = accent,
                    menuSurface = preset.menuSurface,
                    checkboxAccent = accent,
                    menuCheckboxAccent = accent,
                    background = preset.background,
                    card = preset.card,
                )
                CouiTheme(
                    darkTheme = dark,
                    colorScheme = couiColors,
                    typography = CouiTypography(),
                    content = themedContent,
                )
            } else {
                themedContent()
            }
        }
    }
}

private fun PaletteStyle.toMiuix(): ThemePaletteStyle = when (this) {
    PaletteStyle.TONAL_SPOT -> ThemePaletteStyle.TonalSpot
    PaletteStyle.NEUTRAL -> ThemePaletteStyle.Neutral
    PaletteStyle.VIBRANT -> ThemePaletteStyle.Vibrant
    PaletteStyle.EXPRESSIVE -> ThemePaletteStyle.Expressive
    PaletteStyle.RAINBOW -> ThemePaletteStyle.Rainbow
    PaletteStyle.FRUIT_SALAD -> ThemePaletteStyle.FruitSalad
    PaletteStyle.MONOCHROME -> ThemePaletteStyle.Monochrome
    PaletteStyle.FIDELITY -> ThemePaletteStyle.Fidelity
    PaletteStyle.CONTENT -> ThemePaletteStyle.Content
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
