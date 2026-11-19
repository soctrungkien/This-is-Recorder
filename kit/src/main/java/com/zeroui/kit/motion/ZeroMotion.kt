package com.zeroui.kit.motion

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import kotlin.math.roundToInt

/** True when decorative motion should stay still. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Shared transition specs. */
object ZeroMotion {
    /** Page fade in place. */
    internal val navFade: NavTransition = navGraphicsTransition(opaqueDepth = 1f) { scope ->
        val depth = scope.relativeDepth
        if (depth < 0f) alpha = 1f - (-depth).coerceIn(0f, 1f)
    }

    /** Short slide with fade for pushed pages. */
    internal val navSlideFade: NavTransition = navGraphicsTransition(opaqueDepth = 1f) { scope ->
        val width = scope.layoutSize.width.toFloat()
        val depth = scope.relativeDepth
        val direction = if (scope.layoutDirection == LayoutDirection.Rtl) -1f else 1f
        if (depth <= 0f) {
            val progress = (-depth).coerceIn(0f, 1f)
            // Whole pixels only: text on a page parked at a fractional offset is blurry.
            translationX = (direction * progress * width * 0.18f).roundToInt().toFloat()
            alpha = ((1f - progress) / 0.7f).coerceIn(0f, 1f)
        } else {
            translationX = -direction * depth.coerceIn(0f, 1f) * width * 0.06f
        }
    }

    /**
     * Fades the old content out, then the new content in.
     * For content that changes shape; overlapping the two during a resize looks muddy.
     */
    fun fadeThrough(clip: Boolean = true): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(220, delayMillis = 90)),
        initialContentExit = fadeOut(tween(90)),
        sizeTransform = SizeTransform(clip = clip),
    )

    /** Cross-fades old and new content. */
    fun crossFade(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(200)),
        initialContentExit = fadeOut(tween(200)),
        sizeTransform = SizeTransform(clip = false),
    )

    /** Rolls old text up and new text in from below. */
    fun textTicker(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(220, delayMillis = 60)) +
            slideInVertically(tween(260, delayMillis = 60)) { it / 2 },
        initialContentExit = fadeOut(tween(140)) +
            slideOutVertically(tween(260)) { -it / 2 },
        sizeTransform = SizeTransform(clip = false),
    )
}

/** Observes the system animator duration scale and reports whether it is zero. */
@Composable
fun rememberSystemAnimationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val read = { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    var off by remember { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                off = read()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return off
}
