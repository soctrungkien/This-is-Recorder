package com.zeroui.kit.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.zeroui.kit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    canDrag: (index: Int) -> Boolean = { true },
    onMove: (from: Int, to: Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val state = remember(lazyListState) { DragDropState(lazyListState, canDrag, onMove, scope) }
    state.haptic = haptic
    androidx.compose.runtime.LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

/** Drag state of one lazy list; observable through snapshot state. */
@Stable
class DragDropState internal constructor(
    private val state: LazyListState,
    private val canDrag: (Int) -> Boolean,
    private val onMove: (Int, Int) -> Unit,
    private val scope: CoroutineScope
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    internal var haptic: HapticFeedback? = null

    internal var longPressExcluded = false

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    /** True when the item at [from] and the position [delta] away are both draggable. */
    internal fun canMoveBy(from: Int, delta: Int): Boolean {
        val to = from + delta
        return to != from && to in 0 until state.layoutInfo.totalItemsCount && canDrag(from) && canDrag(to)
    }

    internal fun moveBy(from: Int, delta: Int): Boolean {
        if (!canMoveBy(from, delta)) return false
        onMove(from, from + delta)
        haptic?.performHapticFeedback(HapticFeedbackType.SegmentTick)
        return true
    }

    internal fun onDragStart(offset: Offset): Boolean {
        val y = offset.y.toInt() + state.layoutInfo.viewportStartOffset
        val hit = state.layoutInfo.visibleItemsInfo
            .firstOrNull { y in it.offset..(it.offset + it.size) }
            ?.takeIf { canDrag(it.index) }
            ?: return false
        draggingItemIndex = hit.index
        draggingItemInitialOffset = hit.offset
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        return true
    }

    var settlingItemIndex by mutableStateOf<Int?>(null)
        private set
    internal val settlingOffset = Animatable(0f)

    internal fun onDragInterrupted() {
        val released = draggingItemIndex
        if (released != null) {
            val from = draggingItemOffset
            settlingItemIndex = released
            scope.launch {
                settlingOffset.snapTo(from)
                settlingOffset.animateTo(0f, SettleSpec)

                if (settlingItemIndex == released && draggingItemIndex != released) settlingItemIndex = null
            }
        }
        draggingItemIndex = null
        draggingItemDraggedDelta = 0f
        draggingItemInitialOffset = 0
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..(item.offset + item.size) &&
                draggingItem.index != item.index &&
                canDrag(item.index)
        }

        if (targetItem != null) {
            val from = draggingItem.index
            val to = targetItem.index
            onMove(from, to)
            draggingItemIndex = to
            haptic?.performHapticFeedback(HapticFeedbackType.SegmentTick)
        } else {
            val overscroll = when {
                draggingItemDraggedDelta > 0 ->
                    (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 ->
                    (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) scope.launch { scrollChannel.send(overscroll) }
        }
    }
}

fun Modifier.dragContainer(
    dragDropState: DragDropState,
    onLongPress: () -> Unit = {},
): Modifier = pointerInput(dragDropState, onLongPress) {
    var ignored = false
    detectDragGesturesAfterLongPress(
        onDrag = { change, offset ->
            if (!ignored) {
                change.consume()
                dragDropState.onDrag(offset)
            }
        },
        onDragStart = { offset ->
            ignored = dragDropState.longPressExcluded
            if (!ignored && dragDropState.onDragStart(offset)) onLongPress()
        },
        onDragEnd = { ignored = false; dragDropState.onDragInterrupted() },
        onDragCancel = { ignored = false; dragDropState.onDragInterrupted() }
    )
}

fun Modifier.excludeFromDragContainer(dragDropState: DragDropState): Modifier =
    pointerInput(dragDropState) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            dragDropState.longPressExcluded = true
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.fastAny { it.pressed })
            } finally {
                dragDropState.longPressExcluded = false
            }
        }
    }

private val ItemPlacementSpec = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

private val SettleSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.5f
)

@Composable
fun LazyItemScope.fastItemPlacement(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(140),
    placementSpec = ItemPlacementSpec,
    fadeOutSpec = tween(110)
)

@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val dragging = index == dragDropState.draggingItemIndex

    val itemModifier = when (index) {
        dragDropState.draggingItemIndex -> Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = dragDropState.draggingItemOffset }
        dragDropState.settlingItemIndex -> Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = dragDropState.settlingOffset.value }
        else -> fastItemPlacement()
    }
    val moveUp = stringResource(R.string.move_up)
    val moveDown = stringResource(R.string.move_down)
    val actions = buildList {
        if (dragDropState.canMoveBy(index, -1)) add(CustomAccessibilityAction(moveUp) { dragDropState.moveBy(index, -1) })
        if (dragDropState.canMoveBy(index, 1)) add(CustomAccessibilityAction(moveDown) { dragDropState.moveBy(index, 1) })
    }
    Box(modifier.then(itemModifier).semantics { customActions = actions }) { content(dragging) }
}
