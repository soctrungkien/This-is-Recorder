package com.zeroui.kit.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.zeroui.kit.R
import com.zeroui.kit.components.SettingIconStyle
import com.zeroui.kit.navigation.PredictiveBackMode
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/** Component family used to render the kit. */
enum class UiStyle(@StringRes val labelRes: Int) {
    Miuix(R.string.ui_style_miuix),
    Material(R.string.ui_style_material),
}

/** Palette generation style for custom theme colors. */
enum class PaletteStyle { TonalSpot, Neutral, Vibrant, Expressive, Rainbow, FruitSalad, Monochrome, Fidelity, Content }

internal fun PaletteStyle.toMiuix(): ThemePaletteStyle = ThemePaletteStyle.valueOf(name)

/** Light or dark selection. */
enum class DarkMode { System, Light, Dark }

/** Decorative motion selection. */
enum class MotionMode(@StringRes val labelRes: Int) {
    System(R.string.follow_system),
    Reduced(R.string.reduce_motion_on),
    Full(R.string.reduce_motion_off),
}

/** Material of floating bars. */
enum class GlassMode(@StringRes val labelRes: Int) {
    Off(R.string.glass_off),
    Frosted(R.string.glass_frosted),
    Liquid(R.string.glass_liquid),
}

/** Material of cards shown on a page background. */
enum class CardMaterial(@StringRes val labelRes: Int) {
    Solid(R.string.home_bg_cards_solid),
    Blur(R.string.glass_frosted),
    Liquid(R.string.glass_liquid),
}

/** Vertical padding of list rows. */
enum class ListDensity(@StringRes val labelRes: Int) {
    Standard(R.string.list_density_standard),
    Compact(R.string.list_density_compact),
}

/** Top bar layout. */
enum class TopBarStyle(@StringRes val labelRes: Int) {
    Large(R.string.top_bar_large),
    Compact(R.string.top_bar_compact),
}

/** Dialog placement. */
enum class DialogPosition(@StringRes val labelRes: Int) {
    Bottom(R.string.dialog_position_bottom),
    Center(R.string.dialog_position_center),
}

/** Active floating bar material. */
val LocalGlassMode = staticCompositionLocalOf { GlassMode.Off }

/** True when frosted blur is selected. */
val LocalBlurEnabled = staticCompositionLocalOf { false }

/** Active list row density. */
val LocalListDensity = staticCompositionLocalOf { ListDensity.Standard }

/** Active top bar layout. */
val LocalTopBarStyle = staticCompositionLocalOf { TopBarStyle.Large }

/** True when the top bar blurs the content scrolled under it. */
val LocalTopBarBlur = staticCompositionLocalOf { false }

/** Active dialog placement. */
val LocalDialogPosition = staticCompositionLocalOf { DialogPosition.Bottom }

/**
 * Complete input of [ZeroTheme].
 *
 * @property seedColor ARGB seed for generated palettes; 0 reads the wallpaper colors.
 * @property uiScale Scale of dp and sp in percent.
 * @property glassIntensity Strength of Liquid Glass in percent.
 */
@Immutable
data class ThemeSettings(
    val style: UiStyle = UiStyle.Miuix,
    val darkMode: DarkMode = DarkMode.System,
    val dynamicColor: Boolean = false,
    val seedColor: Int = 0,
    val palette: PaletteStyle = PaletteStyle.TonalSpot,
    val pureBlack: Boolean = false,
    val motion: MotionMode = MotionMode.System,
    val uiScale: Int = 100,
    val glassMode: GlassMode = GlassMode.Off,
    val glassIntensity: Int = 100,
    val listDensity: ListDensity = ListDensity.Standard,
    val topBar: TopBarStyle = TopBarStyle.Large,
    val topBarBlur: Boolean = false,
    val smoothCorners: Boolean = true,
    val elasticScroll: Boolean = true,
    val haptics: Boolean = true,
    val dialogPosition: DialogPosition = DialogPosition.Bottom,
    val iconStyle: SettingIconStyle = SettingIconStyle.Filled,
    val predictiveBack: PredictiveBackMode = PredictiveBackMode.Custom,
)
