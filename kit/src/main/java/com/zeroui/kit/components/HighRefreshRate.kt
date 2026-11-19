package com.zeroui.kit.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.abs

/** A display mode reduced to the fields used for selection. */
data class RefreshMode(val id: Int, val rate: Float)

/** Window refresh rate selection. */
object HighRefreshRate {
    /** Preference value that selects the highest supported rate. */
    const val MAX = -1

    /** Preference value that leaves the choice to the system. */
    const val SYSTEM = 0

    /** Mode id to request for [preference]; 0 leaves the choice to the system. */
    fun pickModeId(modes: List<RefreshMode>, preference: Int): Int {
        if (modes.size < 2 || preference == SYSTEM) return 0
        val target = if (preference == MAX) modes.maxBy { it.rate }
        else modes.minBy { abs(it.rate - preference) }
        return target.id
    }

    /** Display mode request and frame rate power savings of a window at one point in time. */
    class Snapshot internal constructor(internal val modeId: Int, internal val powerSavings: Boolean?)

    /** Reads the current request of [window]; power savings is null below Android 15. */
    fun capture(window: Window): Snapshot = Snapshot(window.attributes.preferredDisplayModeId, powerSavings(window))

    /** Requests the display mode for [preference] on [window]; leaves fixed refresh rate displays untouched. */
    fun apply(window: Window, preference: Int) {
        val modes = sameSizeModes(window)
        if (modes.size < 2) return
        val modeId = pickModeId(modes.map { RefreshMode(it.modeId, it.refreshRate) }, preference)
        if (window.attributes.preferredDisplayModeId != modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = modeId }
        }
        // Android 15 lowers the rate on its own to save power, which undoes an
        // explicit choice. Only the "system" preference keeps that behavior.
        setPowerSavings(window, preference == SYSTEM)
    }

    /** Puts [window] back to the request and power savings recorded in [snapshot]. */
    fun restore(window: Window, snapshot: Snapshot) {
        if (window.attributes.preferredDisplayModeId != snapshot.modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = snapshot.modeId }
        }
        snapshot.powerSavings?.let { setPowerSavings(window, it) }
    }

    /** Distinct refresh rates of the display in descending order; empty when fixed. */
    fun supportedRates(window: Window): List<Int> {
        val rates = sameSizeModes(window).map { kotlin.math.round(it.refreshRate).toInt() }.distinct().sortedDescending()
        return if (rates.size < 2) emptyList() else rates
    }

    private fun powerSavings(window: Window): Boolean? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching { window.isFrameRatePowerSavingsBalanced }.getOrNull()
        } else {
            null
        }

    private fun setPowerSavings(window: Window, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching { window.setFrameRatePowerSavingsBalanced(enabled) }
        }
    }

    @Suppress("DEPRECATION")
    private fun displayOf(window: Window): Display? =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.context.display else null)
            ?: window.windowManager.defaultDisplay

    // Modes at another resolution are left out: requesting one makes the panel switch
    // resolution, not just refresh rate.
    private fun sameSizeModes(window: Window): List<Display.Mode> {
        val display = displayOf(window) ?: return emptyList()
        val current = display.mode ?: return emptyList()
        return display.supportedModes.filter {
            it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
        }
    }

    /** Calls [onChange] on display changes; the returned function unregisters the listener. */
    fun watchDisplay(context: Context, onChange: () -> Unit): () -> Unit {
        val dm = context.getSystemService(DisplayManager::class.java) ?: return {}
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) = onChange()
        }
        dm.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        return { dm.unregisterDisplayListener(listener) }
    }
}

/** Refresh rate preference read by [HighRefreshRateEffect]. */
val LocalRefreshRatePreference = compositionLocalOf { HighRefreshRate.MAX }

/**
 * Applies [preference] to the window hosting this composition and restores its previous
 * display mode on leaving. Re-applies on display changes.
 */
@Composable
fun HighRefreshRateEffect(preference: Int = LocalRefreshRatePreference.current) {
    val view = LocalView.current
    DisposableEffect(view, preference) {
        // A dialog lives in its own window with its own mode request, so the request
        // made on the activity window does not cover it.
        val window = (view.parent as? DialogWindowProvider)?.window ?: view.context.findActivity()?.window
        if (window == null) return@DisposableEffect onDispose { }
        val previous = HighRefreshRate.capture(window)
        HighRefreshRate.apply(window, preference)
        // Mode ids belong to one display configuration; after a fold or a resolution
        // change the old id may point at nothing.
        val stopWatching = HighRefreshRate.watchDisplay(view.context) { HighRefreshRate.apply(window, preference) }
        onDispose {
            stopWatching()
            HighRefreshRate.restore(window, previous)
        }
    }
}

/** Refresh rates offered by the display of the current activity. */
@Composable
fun rememberSupportedRefreshRates(): List<Int> {
    val view = LocalView.current
    return remember(view) {
        view.context.findActivity()?.window?.let(HighRefreshRate::supportedRates).orEmpty()
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
