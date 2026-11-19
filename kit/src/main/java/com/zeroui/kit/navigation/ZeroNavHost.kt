package com.zeroui.kit.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.zeroui.kit.R
import com.zeroui.kit.motion.LocalReducedMotion
import com.zeroui.kit.motion.ZeroMotion
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import kotlin.math.abs

/** Behavior of the back gesture. */
enum class PredictiveBackMode(@StringRes val labelRes: Int) {
    Off(R.string.predictive_back_off),
    Official(R.string.predictive_back_official),
    Custom(R.string.predictive_back_custom),
}

/** Active back gesture behavior. */
val LocalPredictiveBackMode = staticCompositionLocalOf { PredictiveBackMode.Custom }

/** Animation used for pushed pages. */
enum class PageTransition(@StringRes val labelRes: Int) {
    Slide(R.string.page_transition_slide),
    SlideFade(R.string.page_transition_slide_fade),
}

/**
 * Pushes [key] when it is not already on the stack.
 * A quick double tap on a row would otherwise open the same page twice.
 */
fun NavController.pushUnique(key: NavKey) {
    if (key !in backStack) push(key)
}

/** Navigation container with the shared transitions and back gesture handling. */
@Composable
fun ZeroNavHost(
    controller: NavController,
    modifier: Modifier = Modifier,
    transition: PageTransition = PageTransition.Slide,
    content: NavEntryBuilder.() -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val backGesture = LocalPredictiveBackMode.current
    val effects = if (reduced) {
        NavDisplayEffects(enableCornerClip = false, dimAmount = 0f)
    } else {
        NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius())
    }
    FingerTrackingBackScope(modifier, remap = backGesture == PredictiveBackMode.Custom) {
        NavDisplay(
            navController = controller,
            modifier = Modifier.fillMaxSize(),
            transition = when {
                reduced -> ZeroMotion.navFade
                transition == PageTransition.SlideFade -> ZeroMotion.navSlideFade
                else -> NavTransitions.MiuixDefault
            },
            effects = effects,
            content = content,
        )
    }
}

/** Handles back with a plain pop when predictive back is off. */
@Composable
fun PlainBackHandler(controller: NavController, key: NavKey) {
    val off = LocalPredictiveBackMode.current == PredictiveBackMode.Off
    // The root page stays out of it so back there still reaches the activity and leaves the app.
    val isTop = controller.backStack.lastOrNull() == key && controller.backStack.size > 1
    BackHandler(enabled = off && isTop) { controller.pop() }
}

/** Registers a page that honors the selected back gesture behavior. */
inline fun <reified T : NavKey> NavEntryBuilder.page(
    controller: NavController,
    noinline content: @Composable (T) -> Unit,
) {
    entry<T> { key ->
        PlainBackHandler(controller, key)
        content(key)
    }
}

/**
 * Shows [placeholder] until the page's enter transition settles, then keeps [content].
 * Composing a heavy page while it is still sliding in costs frames exactly when they show.
 */
@Composable
fun DeferUntilSettled(
    placeholder: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val transition = LocalNavTransitionScope.current
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(transition) {
        snapshotFlow { transition.isRunning }.first { running -> !running }
        settled = true
    }
    if (settled) content() else placeholder()
}

/**
 * Forwards host back events to an inner dispatcher.
 *
 * The progress the system reports is eased and does not follow the finger one to one,
 * so a page driven by it lags behind the drag. NavDisplay listens on whichever dispatcher
 * the composition local holds; handing it our own is the only place to rewrite the
 * events before it sees them.
 *
 * @param remap Rewrites progress to the finger displacement divided by the container width.
 */
@Composable
private fun FingerTrackingBackScope(
    modifier: Modifier,
    remap: Boolean,
    content: @Composable () -> Unit,
) {
    val outer = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    if (outer == null) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }
    // A plain array, not state: the width is only read inside gesture callbacks and
    // must not recompose anything when it changes.
    val widthPx = remember { floatArrayOf(0f) }
    val remapState = rememberUpdatedState(remap)
    val relay = remember { BackGestureRelay() }
    val inner = remember(outer) { NavigationEventDispatcher() }
    val forwarder = remember(relay) { BackGestureForwarder(relay, { widthPx[0] }, { remapState.value }) }
    val innerOwner = remember(inner) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = inner
        }
    }

    DisposableEffect(inner, relay) {
        // Claim the host's back gesture only while something inside handles it.
        // On the root page nothing does, and the gesture goes to the system.
        relay.onEnabledChanged = { forwarder.isBackEnabled = it }
        inner.addInput(relay)
        onDispose {
            relay.onEnabledChanged = {}
            inner.dispose()
        }
    }
    DisposableEffect(outer, forwarder) {
        outer.addHandler(forwarder)
        onDispose { forwarder.remove() }
    }

    Box(modifier.fillMaxSize().onSizeChanged { widthPx[0] = it.width.toFloat() }) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides innerOwner) {
            content()
        }
    }
}

/** Event source of the inner dispatcher. */
private class BackGestureRelay : NavigationEventInput() {
    var onEnabledChanged: (Boolean) -> Unit = {}

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        onEnabledChanged(hasEnabledHandlers)
    }

    fun started(event: NavigationEvent) = dispatchOnBackStarted(event)
    fun progressed(event: NavigationEvent) = dispatchOnBackProgressed(event)
    fun cancelled() = dispatchOnBackCancelled()
    fun completed() = dispatchOnBackCompleted()
}

/** Host handler that sends rewritten events to [relay]. */
private class BackGestureForwarder(
    private val relay: BackGestureRelay,
    private val widthPx: () -> Float,
    private val remapEnabled: () -> Boolean,
) : NavigationEventHandler<NavigationEventInfo>(
    initialInfo = NavigationEventInfo.None,
    isBackEnabled = false,
    isForwardEnabled = false,
) {
    private var startX = 0f

    override fun onBackStarted(event: NavigationEvent) {
        startX = event.touchX
        relay.started(remap(event))
    }

    override fun onBackProgressed(event: NavigationEvent) {
        relay.progressed(remap(event))
    }

    override fun onBackCancelled() {
        relay.cancelled()
    }

    override fun onBackCompleted() {
        relay.completed()
    }

    private fun remap(event: NavigationEvent): NavigationEvent {
        val width = widthPx()
        if (!remapEnabled() || width <= 0f) return event
        val progress = (abs(event.touchX - startX) / width).coerceIn(0f, 1f)
        return NavigationEvent(
            swipeEdge = event.swipeEdge,
            progress = progress,
            touchX = event.touchX,
            touchY = event.touchY,
            frameTimeMillis = event.frameTimeMillis,
        )
    }
}
