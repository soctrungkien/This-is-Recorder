package com.zeroui.kit.glass

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zeroui.kit.theme.LocalBlurEnabled
import com.zeroui.kit.theme.LocalTopBarBlur
import com.zeroui.kit.theme.isMaterial
import com.zeroui.kit.components.TopBarScroll
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TopBarBlurFadeDistance = 48.dp

/** True when AGSL runtime shaders are available. */
// RuntimeShader only exists from API 33, whatever the library check says.
fun shaderUsable(): Boolean =
    isRuntimeShaderSupported() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Sampling layer for floating bars; null when blur is off or not available. */
@Composable
fun rememberBlurBackdrop(): BlurBackdrop? {
    if (!LocalBlurEnabled.current || !shaderUsable() || isMaterial()) return null
    val base = MiuixTheme.colorScheme.surface
    val layer = rememberLayerBackdrop {
        // Content does not paint every pixel. Without a solid base the blur would
        // sample transparency wherever it left a gap.
        drawRect(base)
        drawContent()
    }
    return remember(layer) { BlurBackdrop(layer) }
}

/** Sampling layer for the top bar blur; null when it is off or not available. */
@Composable
fun rememberTopBarBackdrop(): BlurBackdrop? {
    if (!LocalTopBarBlur.current || !shaderUsable() || isMaterial()) return null
    val base = MiuixTheme.colorScheme.surface
    val page = LocalPageBackground.current
    val layer = rememberLayerBackdrop {
        drawRect(base)
        // The page image is drawn by the scaffold, outside this layer, so it has to be
        // repainted here for the bar to blur it.
        page?.let { drawPageBackground(it, base) }
        drawContent()
    }
    return remember(layer) { BlurBackdrop(layer) }
}

/** Transparent with a sampling layer, unspecified without one. */
fun topBarColor(backdrop: BlurBackdrop?): Color =
    if (backdrop != null) Color.Transparent else Color.Unspecified

/** Records this content into [backdrop]; returns the receiver when it is null. */
fun Modifier.topBarBlurSource(backdrop: BlurBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop.layer)

/** Records this content into [backdrop]; returns the receiver when it is null. */
fun Modifier.blurSource(backdrop: BlurBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop.layer)

/** Draws a progressive blur behind [content] that fades in with the scroll offset. */
@Composable
fun BlurredTopBar(
    backdrop: BlurBackdrop?,
    scrollBehavior: TopBarScroll?,
    content: @Composable () -> Unit,
) {
    if (backdrop == null) {
        content()
        return
    }
    val colors = BlurDefaults.blurColors(
        blendColors = listOf(BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f))),
    )
    val ready = rememberAfterFirstFrame()
    Box {
        if (ready) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // Nothing sits under the bar until the list moves, so the blur
                        // fades in over TopBarBlurFadeDistance instead of popping.
                        alpha = scrollBehavior?.behavior?.state
                            ?.let { (-it.contentOffset / TopBarBlurFadeDistance.toPx()).coerceIn(0f, 1f) }
                            ?: 1f
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop.layer,
                        shape = RectangleShape,
                        blurRadius = 10f,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        colors = colors,
                    ),
            )
        }
        content()
    }
}

/** False until the first frame of this composition has been drawn. */
@Composable
private fun rememberAfterFirstFrame(): Boolean {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        ready = true
    }
    return ready
}

/** Applies blur from [backdrop] with a [tint]; returns the receiver when it is null. */
fun Modifier.frostedGlass(
    backdrop: BlurBackdrop?,
    shape: Shape,
    tint: Color,
    blurRadius: Float = 25f,
): Modifier = if (backdrop == null) {
    this
} else {
    textureBlur(
        backdrop = backdrop.layer,
        shape = shape,
        blurRadius = blurRadius,
        colors = BlurColors(blendColors = listOf(BlendColorEntry(color = tint))),
    )
}

/** Continuous-curvature corner shape. */
@Composable
fun rememberSquircleShape(cornerRadius: Dp): Shape {
    val squircle = isSquircleEnabled() && shaderUsable()
    return remember(cornerRadius, squircle) { SquircleShape(cornerRadius, squircle) }
}

/** Squircle shape that keeps the outline of the last requested size and density. */
private class SquircleShape(private val cornerRadius: Dp, private val squircle: Boolean) : Shape {
    private var cachedSize = Size.Unspecified
    private var cachedDensity = Float.NaN
    private var cachedOutline: Outline? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // Building the squircle path is the costly part, and the outline gets asked
        // for again at the same size far more often than the size changes.
        cachedOutline?.let { if (size == cachedSize && density.density == cachedDensity) return it }
        val outline = Outline.Generic(
            Path().apply {
                addSquircleRect(
                    width = size.width,
                    height = size.height,
                    cornerRadius = with(density) { cornerRadius.toPx() },
                    squircleEnabled = squircle,
                )
            },
        )
        cachedSize = size
        cachedDensity = density.density
        cachedOutline = outline
        return outline
    }
}
