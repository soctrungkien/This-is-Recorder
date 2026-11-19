package com.zeroui.kit.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zeroui.kit.glass.CardGlass
import com.zeroui.kit.glass.LocalCardGlass
import com.zeroui.kit.glass.LocalLiquidIntensity
import com.zeroui.kit.glass.frostedGlass
import com.zeroui.kit.glass.liquidGlassOver
import com.zeroui.kit.glass.rememberSquircleShape
import com.zeroui.kit.glass.shaderUsable
import com.zeroui.kit.theme.ProvideContentColor
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider

private val SegmentOuterRadius = 16.dp
private val SegmentInnerRadius = 4.dp
private val SegmentGap = 2.dp
private val MaterialCardRadius = 16.dp

private const val BlurTintAlpha = 0.55f
private const val LiquidTintAlpha = 0.35f
private const val TranslucentAlpha = 0.78f

/** True inside a Material segmented list. */
private val LocalInSegmentedList = staticCompositionLocalOf { false }

/**
 * Card.
 *
 * @param color Container color; the theme's card color when unspecified.
 * @param contentColor Content color; the theme's card content color when unspecified.
 * @param contentPadding Inner padding; zero renders a segmented list in the Material style.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    cornerRadius: Dp = CardDefaults.CornerRadius,
    contentPadding: PaddingValues = CardDefaults.InsideMargin,
    pressFeedbackType: PressFeedback = PressFeedback.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val defaultColor = color == Color.Unspecified
    val glass = LocalCardGlass.current?.takeIf { defaultColor }
    if (glass != null) {
        val inner: @Composable ColumnScope.() -> Unit = {
            CompositionLocalProvider(LocalCardGlass provides null) { content() }
        }
        GlassCard(
            glass,
            modifier,
            cornerRadius,
            contentPadding,
            contentColor,
            pressFeedbackType,
            showIndication,
            holdDownState,
            onClick,
            onLongPress,
            inner,
        )
        return
    }
    if (isMaterial()) {
        MaterialCard(modifier, color, contentColor, cornerRadius, contentPadding, onClick, onLongPress, content)
        return
    }
    val defaults = CardDefaults.defaultColors()
    val colors = CardDefaults.defaultColors(
        color = if (defaultColor) defaults.color else color,
        contentColor = if (contentColor == Color.Unspecified) defaults.contentColor else contentColor,
    )
    val interactive = onClick != null || onLongPress != null || showIndication || holdDownState ||
        pressFeedbackType != PressFeedback.None
    if (interactive) {
        MiuixCard(
            modifier = modifier,
            cornerRadius = cornerRadius,
            insideMargin = contentPadding,
            colors = colors,
            pressFeedbackType = pressFeedbackType.toMiuix(),
            showIndication = showIndication,
            holdDownState = holdDownState,
            onClick = onClick,
            onLongPress = onLongPress,
            content = content,
        )
    } else {
        MiuixCard(
            modifier = modifier,
            cornerRadius = cornerRadius,
            insideMargin = contentPadding,
            colors = colors,
            content = content,
        )
    }
}

/** Group of list rows with zero padding. */
@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier, contentPadding = PaddingValues(0.dp), content = content)
}

/** Titled group of list rows with the standard page margins. */
@Composable
fun ListSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title)
        ListCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), content = content)
    }
}

/** Card on a page background that samples the background with its selected material. */
@Composable
private fun GlassCard(
    glass: CardGlass,
    modifier: Modifier,
    cornerRadius: Dp,
    contentPadding: PaddingValues,
    contentColor: Color,
    pressFeedbackType: PressFeedback,
    showIndication: Boolean,
    holdDownState: Boolean,
    onClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isMaterial()) {
        MaterialCard(
            modifier,
            MaterialTheme.colorScheme.surfaceBright.copy(alpha = TranslucentAlpha),
            MaterialTheme.colorScheme.onSurface,
            cornerRadius,
            contentPadding,
            onClick,
            onLongPress,
            content,
        )
        return
    }
    val defaults = CardDefaults.defaultColors()
    val base = defaults.color
    val blur = glass.blur
    val liquid = glass.liquid
    val squircle = rememberSquircleShape(cornerRadius)
    val intensity = LocalLiquidIntensity.current
    val glassModifier = when {
        liquid != null -> modifier.liquidGlassOver(
            backdrop = liquid.layer,
            shape = { RoundedCornerShape(cornerRadius) },
            tint = base.copy(alpha = LiquidTintAlpha),
            intensity = intensity,
            shadow = null,
        )
        blur != null -> modifier.frostedGlass(
            backdrop = blur,
            shape = squircle,
            tint = base.copy(alpha = BlurTintAlpha),
        )
        else -> modifier
    }
    MiuixCard(
        modifier = glassModifier,
        cornerRadius = cornerRadius,
        insideMargin = contentPadding,
        colors = CardDefaults.defaultColors(
            color = if (blur != null || liquid != null) Color.Transparent else base.copy(alpha = TranslucentAlpha),
            contentColor = if (contentColor == Color.Unspecified) defaults.contentColor else contentColor,
        ),
        pressFeedbackType = pressFeedbackType.toMiuix(),
        showIndication = showIndication,
        holdDownState = holdDownState,
        onClick = onClick,
        onLongPress = onLongPress,
        content = content,
    )
}

/** Material card: a segmented list for zero padding; one rounded surface for other padding. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MaterialCard(
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    cornerRadius: Dp,
    contentPadding: PaddingValues,
    onClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = if (color == Color.Unspecified) MaterialTheme.colorScheme.surfaceBright else color
    val foreground = if (contentColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else contentColor
    if (contentPadding.isZero()) {
        ProvideContentColor(foreground) {
            SegmentedRowsLayout(modifier, container, onClick, onLongPress, content)
        }
        return
    }
    val radius = if (cornerRadius == CardDefaults.CornerRadius) MaterialCardRadius else cornerRadius
    val shape = RoundedCornerShape(radius)
    CompositionLocalProvider(LocalInSegmentedList provides false) {
        ProvideContentColor(foreground) {
            Column(
                modifier = modifier
                    .clip(shape)
                    .background(container)
                    .then(
                        if (onClick != null || onLongPress != null) {
                            Modifier.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongPress)
                        } else {
                            Modifier
                        },
                    )
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}

/** Horizontal divider; hidden inside Material segmented lists. */
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.DividerColor,
) {
    if (isMaterial() && LocalInSegmentedList.current) return
    MiuixHorizontalDivider(modifier = modifier, thickness = thickness, color = color)
}

@Composable
private fun PaddingValues.isZero(): Boolean {
    val direction = LocalLayoutDirection.current
    return calculateTopPadding() == 0.dp && calculateBottomPadding() == 0.dp &&
        calculateStartPadding(direction) == 0.dp && calculateEndPadding(direction) == 0.dp
}

private fun segmentShape(first: Boolean, last: Boolean): Shape {
    val top = if (first) SegmentOuterRadius else SegmentInnerRadius
    val bottom = if (last) SegmentOuterRadius else SegmentInnerRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

private data class Segment(val top: Int, val bottom: Int, val first: Boolean, val last: Boolean)

/** Segment bounds written during layout and read during draw. */
private class SegmentHolder {
    var segments by mutableStateOf(emptyList<Segment>())
    var latest: List<Segment> = emptyList()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SegmentedRowsLayout(
    modifier: Modifier,
    color: Color,
    onClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val holder = remember { SegmentHolder() }
    CompositionLocalProvider(LocalInSegmentedList provides true) {
        Layout(
            content = { SegmentColumnScope.content() },
            modifier = modifier
                .then(
                    if (onClick != null || onLongPress != null) {
                        Modifier
                            .clip(RoundedCornerShape(SegmentOuterRadius))
                            .combinedClickable(onClick = onClick ?: {}, onLongClick = onLongPress)
                    } else {
                        Modifier
                    },
                )
                .drawBehind {
                    holder.segments.forEach { segment ->
                        val outline = segmentShape(segment.first, segment.last).createOutline(
                            Size(size.width, (segment.bottom - segment.top).toFloat()),
                            layoutDirection,
                            this,
                        )
                        translate(top = segment.top.toFloat()) { drawOutline(outline, color) }
                    }
                },
        ) { measurables, constraints ->
            val gap = SegmentGap.roundToPx()
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val visible = placeables.indices.filter { placeables[it].height > 0 }
            val tops = IntArray(placeables.size)
            var y = 0
            visible.forEachIndexed { order, index ->
                if (order > 0) y += gap
                tops[index] = y
                y += placeables[index].height
            }
            val width = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                placeables.maxOfOrNull { it.width } ?: 0
            }
            val segments = visible.mapIndexed { order, index ->
                Segment(
                    top = tops[index],
                    bottom = tops[index] + placeables[index].height,
                    first = order == 0,
                    last = order == visible.lastIndex,
                )
            }
            if (segments != holder.latest) {
                holder.latest = segments
                holder.segments = segments
            }
            layout(width, y.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                placeables.forEachIndexed { index, placeable ->
                    val order = visible.indexOf(index)
                    if (order < 0) {
                        placeable.place(0, 0)
                    } else {
                        val shape = segmentShape(first = order == 0, last = order == visible.lastIndex)
                        placeable.placeWithLayer(0, tops[index]) {
                            this.shape = shape
                            clip = true
                        }
                    }
                }
            }
        }
    }
}

/** Column scope of a segmented list; weight has no effect. */
private object SegmentColumnScope : ColumnScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Horizontal): Modifier =
        fillMaxWidth().wrapContentWidth(alignment)
    override fun Modifier.alignBy(alignmentLine: VerticalAlignmentLine): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}

/** Position of a row inside a grouped card. */
enum class SlicePosition {
    Single, First, Middle, Last;

    val roundTop: Boolean get() = this == Single || this == First
    val roundBottom: Boolean get() = this == Single || this == Last

    companion object {
        /** Returns the position of row [index] in a group of [count] rows. */
        fun of(index: Int, count: Int): SlicePosition = when {
            count <= 1 -> Single
            index == 0 -> First
            index == count - 1 -> Last
            else -> Middle
        }
    }
}

/** One row of a grouped card for lazy lists. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardSlice(
    position: SlicePosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val click = if (onClick != null || onLongPress != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = indication,
            onClick = onClick ?: {},
            onLongClick = onLongPress,
        )
    } else {
        Modifier
    }
    val glass = LocalCardGlass.current

    if (isMaterial()) {
        val top = if (position.roundTop) SegmentOuterRadius else SegmentInnerRadius
        val bottom = if (position.roundBottom) SegmentOuterRadius else SegmentInnerRadius
        val shape = RoundedCornerShape(top, top, bottom, bottom)
        val color = MaterialTheme.colorScheme.surfaceBright.let {
            if (glass != null) it.copy(alpha = TranslucentAlpha) else it
        }
        CompositionLocalProvider(LocalInSegmentedList provides true) {
            ProvideContentColor(MaterialTheme.colorScheme.onSurface) {
                Box(
                    modifier = modifier
                        .padding(bottom = if (position.roundBottom) 0.dp else SegmentGap)
                        .clip(shape)
                        .background(color)
                        .then(click),
                    content = content,
                )
            }
        }
        return
    }

    val radius = CardDefaults.CornerRadius
    val top = if (position.roundTop) radius else 0.dp
    val bottom = if (position.roundBottom) radius else 0.dp
    val base = MiuixTheme.colorScheme.surfaceContainer
    val surface = when {
        glass?.blur != null -> {
            val shape = rememberSliceShape(position, radius)
            Modifier
                .frostedGlass(backdrop = glass.blur, shape = shape, tint = base.copy(alpha = BlurTintAlpha))
                .clip(shape)
        }
        glass != null -> Modifier.squircleSurface(base.copy(alpha = TranslucentAlpha), top, top, bottom, bottom)
        else -> Modifier.squircleSurface(base, top, top, bottom, bottom)
    }
    Box(modifier = modifier.then(surface).then(click), content = content)
}

/** Continuous-curvature outline of one row. */
@Composable
private fun rememberSliceShape(position: SlicePosition, radius: Dp): Shape {
    val squircle = isSquircleEnabled() && shaderUsable()
    return remember(position, radius, squircle) {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                if (!position.roundTop && !position.roundBottom) {
                    return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
                }
                val r = with(density) { radius.toPx() }
                val extendTop = if (position.roundTop) 0f else r * 2
                val extendBottom = if (position.roundBottom) 0f else r * 2
                val full = Path().apply {
                    addSquircleRect(
                        width = size.width,
                        height = size.height + extendTop + extendBottom,
                        cornerRadius = r,
                        squircleEnabled = squircle,
                    )
                    translate(Offset(0f, -extendTop))
                }
                val bounds = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
                return Outline.Generic(Path.combine(PathOperation.Intersect, full, bounds))
            }
        }
    }
}

/** Visual feedback of a pressed card. */
enum class PressFeedback { None, Sink, Tilt }

internal fun PressFeedback.toMiuix(): PressFeedbackType = PressFeedbackType.valueOf(name)
