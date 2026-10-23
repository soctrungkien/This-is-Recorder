package com.hihi.recorder.media

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import com.hihi.recorder.data.VideoBitrate
import com.hihi.recorder.data.VideoResolution
import kotlin.math.min
import kotlin.math.roundToInt

data class CaptureSize(val width: Int, val height: Int) {
    val spec: String get() = "${width}x$height"
}

/** Resolves the output size while keeping the current display orientation. */
fun resolveCaptureSize(context: Context, resolution: VideoResolution): CaptureSize {
    val display = context.getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)
    val point = Point()
    if (display != null) {
        @Suppress("DEPRECATION")
        display.getRealSize(point)
    }
    if (point.x <= 0 || point.y <= 0) {
        point.x = 1080
        point.y = 1920
    }

    val nativeShortEdge = min(point.x, point.y)
    val requested = resolution.shortEdge ?: nativeShortEdge
    val scale = min(1f, requested.toFloat() / nativeShortEdge.toFloat())

    val width = (point.x * scale).roundToInt().coerceAtLeast(2)
    val height = (point.y * scale).roundToInt().coerceAtLeast(2)
    return CaptureSize(alignEven(width), alignEven(height))
}

fun resolveDisplayDensity(context: Context): Int =
    context.resources.displayMetrics.densityDpi

/**
 * Auto mode targets roughly 0.12 bits per pixel per frame, which lands around
 * 9 Mbps for 1080p30 and 18 Mbps for 1080p60.
 */
fun VideoBitrate.bitsPerSecond(width: Int, height: Int, frameRate: Int): Int {
    megabitsPerSecond?.let { return it * 1_000_000 }
    val estimate = (width.toLong() * height.toLong() * frameRate.toLong() * 0.12).toLong()
    return estimate.coerceIn(3_000_000L, 45_000_000L).toInt()
}

private fun alignEven(value: Int): Int {
    val aligned = if (value % 2 == 0) value else value - 1
    return aligned.coerceAtLeast(2)
}

fun isAtLeastApi(api: Int): Boolean = Build.VERSION.SDK_INT >= api
