package com.zeroui.kit.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

/** Material baseline seed. */
internal const val MaterialBaselineSeed: Int = 0xFF6750A4.toInt()

/** True when the platform uses the 2025 color specification. */
internal val UseColorSpec2025: Boolean get() = Build.VERSION.SDK_INT >= 36

/** Returns the Material color scheme for the given theme inputs. */
@Composable
internal fun rememberMaterialColorScheme(
    dark: Boolean,
    dynamicColor: Boolean,
    seedColor: Int,
    palette: PaletteStyle,
): ColorScheme {
    val context = LocalContext.current
    return remember(dark, dynamicColor, seedColor, palette) {
        when {
            // Wallpaper colors exist from Android 12; older systems fall through to
            // the baseline palette.
            dynamicColor && seedColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamicColor && seedColor != 0 -> colorSchemeFromSeed(seedColor, palette, dark)
            else -> colorSchemeFromSeed(MaterialBaselineSeed, PaletteStyle.TonalSpot, dark)
        }
    }
}

/** Generates a Material color scheme from [seed]. */
internal fun colorSchemeFromSeed(seed: Int, style: PaletteStyle, dark: Boolean): ColorScheme {
    // The 2025 spec only defines these four variants.
    val spec2025Supported =style == PaletteStyle.TonalSpot || style == PaletteStyle.Neutral ||
        style == PaletteStyle.Vibrant || style == PaletteStyle.Expressive
    val spec = if (UseColorSpec2025 && spec2025Supported) {
        ColorSpec.SpecVersion.SPEC_2025
    } else {
        ColorSpec.SpecVersion.SPEC_2021
    }
    val hct = Hct.fromInt(seed)
    val platform = DynamicScheme.Platform.PHONE
    val s: DynamicScheme = when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, dark, 0.0, spec, platform)
        PaletteStyle.Neutral -> SchemeNeutral(hct, dark, 0.0, spec, platform)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, dark, 0.0, spec, platform)
        PaletteStyle.Expressive -> SchemeExpressive(hct, dark, 0.0, spec, platform)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, dark, 0.0, spec, platform)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, dark, 0.0, spec, platform)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, dark, 0.0, spec, platform)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, dark, 0.0, spec, platform)
        PaletteStyle.Content -> SchemeContent(hct, dark, 0.0, spec, platform)
    }
    return ColorScheme(
        primary = Color(s.primary),
        onPrimary = Color(s.onPrimary),
        primaryContainer = Color(s.primaryContainer),
        onPrimaryContainer = Color(s.onPrimaryContainer),
        inversePrimary = Color(s.inversePrimary),
        secondary = Color(s.secondary),
        onSecondary = Color(s.onSecondary),
        secondaryContainer = Color(s.secondaryContainer),
        onSecondaryContainer = Color(s.onSecondaryContainer),
        tertiary = Color(s.tertiary),
        onTertiary = Color(s.onTertiary),
        tertiaryContainer = Color(s.tertiaryContainer),
        onTertiaryContainer = Color(s.onTertiaryContainer),
        background = Color(s.background),
        onBackground = Color(s.onBackground),
        surface = Color(s.surface),
        onSurface = Color(s.onSurface),
        surfaceVariant = Color(s.surfaceVariant),
        onSurfaceVariant = Color(s.onSurfaceVariant),
        surfaceTint = Color(s.surfaceTint),
        inverseSurface = Color(s.inverseSurface),
        inverseOnSurface = Color(s.inverseOnSurface),
        error = Color(s.error),
        onError = Color(s.onError),
        errorContainer = Color(s.errorContainer),
        onErrorContainer = Color(s.onErrorContainer),
        outline = Color(s.outline),
        outlineVariant = Color(s.outlineVariant),
        scrim = Color(s.scrim),
        surfaceBright = Color(s.surfaceBright),
        surfaceDim = Color(s.surfaceDim),
        surfaceContainer = Color(s.surfaceContainer),
        surfaceContainerHigh = Color(s.surfaceContainerHigh),
        surfaceContainerHighest = Color(s.surfaceContainerHighest),
        surfaceContainerLow = Color(s.surfaceContainerLow),
        surfaceContainerLowest = Color(s.surfaceContainerLowest),
        primaryFixed = Color(s.primaryFixed),
        primaryFixedDim = Color(s.primaryFixedDim),
        onPrimaryFixed = Color(s.onPrimaryFixed),
        onPrimaryFixedVariant = Color(s.onPrimaryFixedVariant),
        secondaryFixed = Color(s.secondaryFixed),
        secondaryFixedDim = Color(s.secondaryFixedDim),
        onSecondaryFixed = Color(s.onSecondaryFixed),
        onSecondaryFixedVariant = Color(s.onSecondaryFixedVariant),
        tertiaryFixed = Color(s.tertiaryFixed),
        tertiaryFixedDim = Color(s.tertiaryFixedDim),
        onTertiaryFixed = Color(s.onTertiaryFixed),
        onTertiaryFixedVariant = Color(s.onTertiaryFixedVariant),
    )
}
