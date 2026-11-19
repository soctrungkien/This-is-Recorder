package com.zeroui.kit.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.zeroui.kit.BuildConfig
import com.zeroui.kit.components.LocalSettingIconStyle
import com.zeroui.kit.glass.LocalLiquidIntensity
import com.zeroui.kit.motion.LocalReducedMotion
import com.zeroui.kit.motion.rememberSystemAnimationsOff
import com.zeroui.kit.navigation.LocalPredictiveBackMode
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor

/** Haptic feedback that ignores every request. */
private object SilentHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/** Active component family. */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.Miuix }

/** Resolved dark state of the app. */
val LocalIsDark = staticCompositionLocalOf { false }

/** True when the Material component family is active. */
@Composable
@ReadOnlyComposable
fun isMaterial(): Boolean = when {
    BuildConfig.LOCK_MIUIX -> false
    BuildConfig.LOCK_MATERIAL -> true
    else -> LocalUiStyle.current == UiStyle.Material
}

/** False when the build fixes the style with the `zeroui.style` Gradle property. */
val UiStyleSelectable: Boolean = !BuildConfig.LOCK_MIUIX && !BuildConfig.LOCK_MATERIAL

/** The style the build fixes, or [selected] when the build does not fix one. */
fun resolveUiStyle(selected: UiStyle): UiStyle = when {
    BuildConfig.LOCK_MIUIX -> UiStyle.Miuix
    BuildConfig.LOCK_MATERIAL -> UiStyle.Material
    else -> selected
}

/** Entry point for the semantic theme values. */
object ZeroTheme {
    val colors: ZeroColors
        @Composable
        @ReadOnlyComposable
        get() = LocalZeroColors.current

    val typography: ZeroTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalZeroTypography.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDark.current
}

/** Sets the content color of both component families. */
@Composable
internal fun ProvideContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        MaterialLocalContentColor provides color,
        MiuixLocalContentColor provides color,
        content = content,
    )
}

/** Applies colors, typography, motion and scale to [content]. */
@Composable
fun ZeroTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit,
) {
    val style = resolveUiStyle(settings.style)
    val material = style == UiStyle.Material
    val dark = when (settings.darkMode) {
        DarkMode.System -> isSystemInDarkTheme()
        DarkMode.Light -> false
        DarkMode.Dark -> true
    }
    val context = LocalContext.current
    SystemBarAppearance(dark)
    LaunchedEffect(settings.darkMode) { syncApplicationNightMode(context, settings.darkMode) }

    val controller = remember(dark, settings.dynamicColor, settings.seedColor, settings.palette, material) {
        val keyed = settings.dynamicColor && settings.seedColor != 0
        ThemeController(
            colorSchemeMode = if (settings.dynamicColor || material) {
                ColorSchemeMode.MonetSystem
            } else {
                ColorSchemeMode.System
            },
            keyColor = when {
                keyed -> Color(settings.seedColor)
                material && !settings.dynamicColor -> Color(MaterialBaselineSeed)
                else -> null
            },
            colorSpec = if (UseColorSpec2025) ThemeColorSpec.Spec2025 else ThemeColorSpec.Spec2021,
            paletteStyle = if (keyed) settings.palette.toMiuix() else ThemePaletteStyle.TonalSpot,
            isDark = dark,
        )
    }
    val black = settings.pureBlack && dark
    val materialColors = if (material) {
        val scheme = rememberMaterialColorScheme(dark, settings.dynamicColor, settings.seedColor, settings.palette)
        remember(scheme, black) { if (black) scheme.pureBlack() else scheme }
    } else {
        null
    }
    val systemAnimationsOff = rememberSystemAnimationsOff()
    val reduced = when (settings.motion) {
        MotionMode.System -> systemAnimationsOff
        MotionMode.Reduced -> true
        MotionMode.Full -> false
    }
    val baseDensity = LocalDensity.current
    // sp is resolved through density too, so scaling density alone scales text with
    // everything else. fontScale stays whatever the user set in system settings.
    val density = remember(baseDensity, settings.uiScale) {
        Density(baseDensity.density * settings.uiScale / 100f, baseDensity.fontScale)
    }
    val systemHaptics = LocalHapticFeedback.current

    MiuixTheme(controller = controller) {
        PureBlackLayer(black) {
            MaterialLayer(materialColors) {
                // Previews and screenshot tests have no activity to supply one.
                val dispatcherOwner = LocalNavigationEventDispatcherOwner.current
                    ?: rememberNavigationEventDispatcherOwner(parent = null)
                val colors = resolveColors(style, dark)
                val typography = resolveTypography(style)
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides dispatcherOwner,
                    LocalUiStyle provides style,
                    LocalIsDark provides dark,
                    LocalReducedMotion provides reduced,
                    LocalDensity provides density,
                    LocalZeroColors provides colors,
                    LocalZeroTypography provides typography,
                    LocalHapticFeedback provides if (settings.haptics) systemHaptics else SilentHapticFeedback,
                    LocalSquircleEnabled provides settings.smoothCorners,
                    LocalOverscrollFactory provides
                        if (settings.elasticScroll) LocalOverscrollFactory.current else null,
                    LocalGlassMode provides settings.glassMode,
                    LocalBlurEnabled provides (settings.glassMode == GlassMode.Frosted),
                    LocalLiquidIntensity provides settings.glassIntensity / 100f,
                    LocalListDensity provides settings.listDensity,
                    LocalTopBarStyle provides settings.topBar,
                    LocalTopBarBlur provides settings.topBarBlur,
                    LocalDialogPosition provides settings.dialogPosition,
                    LocalSettingIconStyle provides settings.iconStyle,
                    LocalPredictiveBackMode provides settings.predictiveBack,
                ) {
                    ProvideContentColor(colors.content, content)
                }
            }
        }
    }
}

@Composable
private fun PureBlackLayer(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val base = MiuixTheme.colorScheme
    MiuixTheme(colors = remember(base) { base.pureBlack() }, content = content)
}

@Composable
private fun MaterialLayer(colors: ColorScheme?, content: @Composable () -> Unit) {
    if (colors == null) {
        content()
    } else {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

/** Keeps the system bar icons in sync with the resolved dark state. */
@Composable
private fun SystemBarAppearance(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        var current = view.context
        while (current is ContextWrapper && current !is Activity) current = current.baseContext
        val window = (current as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

/**
 * Publishes the dark selection to the system on Android 12 and above.
 * The splash screen is drawn by the system before any of our code runs; without this
 * it follows the system theme and flashes the wrong color on a forced light or dark app.
 */
private fun syncApplicationNightMode(context: Context, mode: DarkMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val manager = context.getSystemService(UiModeManager::class.java) ?: return
    runCatching {
        manager.setApplicationNightMode(
            when (mode) {
                DarkMode.System -> UiModeManager.MODE_NIGHT_AUTO
                DarkMode.Light -> UiModeManager.MODE_NIGHT_NO
                DarkMode.Dark -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }
}
