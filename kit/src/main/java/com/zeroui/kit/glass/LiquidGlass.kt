package com.zeroui.kit.glass

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.zeroui.kit.theme.GlassMode
import com.zeroui.kit.theme.LocalGlassMode
import com.zeroui.kit.theme.isMaterial

/** True on Android 13 and above. */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Liquid Glass strength multiplier. */
val LocalLiquidIntensity = compositionLocalOf { 1f }

/** True when Liquid Glass is selected, supported and the Miuix style is active. */
@Composable
fun isLiquidEnabled(): Boolean =
    LocalGlassMode.current == GlassMode.Liquid && isLiquidGlassSupported() && !isMaterial()

/**
 * Returns a refraction source; null when Liquid Glass is unavailable.
 * With [always] false the source also requires Liquid Glass to be selected in the theme.
 */
@Composable
fun rememberLiquidBackdrop(always: Boolean = false): LiquidBackdrop? {
    val usable = if (always) isLiquidGlassSupported() else isLiquidEnabled()
    if (!usable) return null
    val layer = rememberLayerBackdrop()
    return remember(layer) { LiquidBackdrop(layer) }
}

/** Records this layer into [backdrop]; returns the receiver when it is null. */
fun Modifier.liquidSource(backdrop: LiquidBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop.layer)

/**
 * Draws a refracting glass surface over [backdrop]; returns the receiver when it is null.
 *
 * @param shape Corner-based shape used by the lens effect.
 * @param tint Color drawn on the glass surface.
 * @param blurRadius Blur of the sampled content.
 * @param refractionHeight Width of the refracting edge.
 * @param refractionAmount Displacement of the refracting edge.
 * @param intensity Multiplier of blur and refraction.
 */
fun Modifier.liquidGlass(
    backdrop: LiquidBackdrop?,
    shape: () -> Shape,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 24.dp,
    intensity: Float = 1f,
): Modifier = liquidGlassOver(
    backdrop = backdrop?.layer,
    shape = shape,
    tint = tint,
    blurRadius = blurRadius,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    intensity = intensity,
)

/** [liquidGlass] over any backdrop, with the highlight and shadow exposed. */
internal fun Modifier.liquidGlassOver(
    backdrop: Backdrop?,
    shape: () -> Shape,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 24.dp,
    intensity: Float = 1f,
    highlight: Highlight? = Highlight.Default,
    shadow: Shadow? = Shadow.Default,
): Modifier {
    if (backdrop == null) return this
    return drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            vibrancy()
            blur(blurRadius.toPx() * intensity)
            lens(refractionHeight.toPx() * intensity, refractionAmount.toPx() * intensity)
        },
        highlight = highlight?.let { { it } },
        shadow = shadow?.let { { it } },
        onDrawSurface = if (tint.isSpecified) {
            { drawRect(tint) }
        } else {
            null
        },
    )
}
