package com.zeroui.kit.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberLiquidLayerBackdrop
import com.zeroui.kit.theme.CardMaterial
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.max
import kotlin.math.roundToInt
import com.kyant.backdrop.backdrops.LayerBackdrop as LiquidLayerBackdrop

/**
 * Image drawn behind a page.
 *
 * @property dim Opacity of the veil over the image in percent.
 * @property cards Material of default cards on the image.
 */
@Immutable
class PageBackground(val bitmap: ImageBitmap, val dim: Int, val cards: CardMaterial)

/** Background of the current page; null draws none. */
val LocalPageBackground = staticCompositionLocalOf<PageBackground?> { null }

/** Sampling layers for cards on a page background; both null means translucent cards. */
class CardGlass(val blur: BlurBackdrop? = null, val liquid: LiquidBackdrop? = null)

/** Card material of the current page; null draws opaque cards. */
val LocalCardGlass = staticCompositionLocalOf<CardGlass?> { null }

/** Draws [bg] with crop scaling and a [veil] overlay. */
fun DrawScope.drawPageBackground(bg: PageBackground, veil: Color) {
    val image = bg.bitmap
    if (image.width <= 0 || image.height <= 0 || size.width <= 0f || size.height <= 0f) return
    val scale = max(size.width / image.width, size.height / image.height)
    val w = (image.width * scale).roundToInt()
    val h = (image.height * scale).roundToInt()
    drawImage(
        image = image,
        dstOffset = IntOffset(((size.width - w) / 2f).roundToInt(), ((size.height - h) / 2f).roundToInt()),
        dstSize = IntSize(w, h),
        filterQuality = FilterQuality.Low,
    )
    if (bg.dim > 0) drawRect(veil.copy(alpha = bg.dim / 100f))
}

/** Provides the card sampling layer for [content]. */
@Composable
internal fun PageGlassHost(bg: PageBackground, base: Color, content: @Composable () -> Unit) {
    val usable = shaderUsable() && !isMaterial()
    when {
        usable && bg.cards == CardMaterial.Liquid && isLiquidGlassSupported() -> {
            val layer = rememberLiquidLayerBackdrop()
            val backdrop = remember(layer) { LiquidBackdrop(layer) }
            Box(Modifier.fillMaxSize()) {
                key(bg, base) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer()
                            .liquidSource(backdrop)
                            .drawBehind {
                                drawRect(base)
                                drawPageBackground(bg, base)
                            },
                    )
                }
                CompositionLocalProvider(LocalCardGlass provides CardGlass(liquid = backdrop), content = content)
            }
        }
        usable -> {
            val layer = rememberLayerBackdrop {
                drawRect(base)
                drawPageBackground(bg, base)
            }
            val backdrop = remember(layer) { BlurBackdrop(layer) }
            Box(Modifier.fillMaxSize()) {
                key(bg, base) {
                    Box(Modifier.matchParentSize().graphicsLayer().layerBackdrop(layer))
                }
                CompositionLocalProvider(LocalCardGlass provides CardGlass(blur = backdrop), content = content)
            }
        }
        else -> CompositionLocalProvider(LocalCardGlass provides CardGlass(), content = content)
    }
}
