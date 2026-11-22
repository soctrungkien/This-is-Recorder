package com.hihi.recorder.ui.icons

// Remix Icon line icons (https://remixicon.com, Remix Icon License v1.0).
// Hand-traced to ImageVector for the bottom navigation only.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun line(
    name: String,
    draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = draw,
    )
}.build()

object RemixIcons {
    /** home-5-line */
    val Home: ImageVector = line("RemixHome") {
        moveTo(3f, 20f)
        lineTo(3f, 9.489f)
        lineTo(12f, 2.477f)
        lineTo(21f, 9.489f)
        lineTo(21f, 20f)
        close()
        moveTo(11f, 20f)
        lineTo(11f, 13f)
        lineTo(13f, 13f)
        lineTo(13f, 20f)
    }

    /** video-line */
    val Videos: ImageVector = line("RemixVideos") {
        moveTo(3f, 3.993f)
        lineTo(21f, 3.993f)
        lineTo(21f, 20.007f)
        lineTo(3f, 20.007f)
        close()
        moveTo(5f, 5f)
        lineTo(5f, 19f)
        lineTo(19f, 19f)
        lineTo(19f, 5f)
        close()
        moveTo(10.4f, 8.348f)
        lineTo(15.5f, 11.667f)
        lineTo(10.4f, 15.252f)
        close()
    }

    /** settings-3-line — simplified gear outline */
    val Settings: ImageVector = line("RemixSettings") {
        moveTo(19.4f, 15f)
        lineTo(17.6f, 14.1f)
        lineTo(17.6f, 11.9f)
        lineTo(19.4f, 11f)
        lineTo(18.4f, 9.1f)
        lineTo(16.6f, 9.9f)
        lineTo(14.8f, 8.9f)
        lineTo(14.8f, 6.9f)
        lineTo(12.8f, 6.9f)
        lineTo(12.8f, 8.9f)
        lineTo(11f, 9.9f)
        lineTo(9.2f, 9.1f)
        lineTo(8.2f, 11f)
        lineTo(10f, 11.9f)
        lineTo(10f, 14.1f)
        lineTo(8.2f, 15f)
        lineTo(9.2f, 16.9f)
        lineTo(11f, 16.1f)
        lineTo(12.8f, 17.1f)
        lineTo(12.8f, 19f)
        lineTo(14.8f, 19f)
        lineTo(14.8f, 17.1f)
        lineTo(16.6f, 16.1f)
        lineTo(18.4f, 16.9f)
        close()
        moveTo(13.8f, 14.6f)
        curveTo(12.8f, 15.2f, 11.5f, 14.9f, 10.9f, 13.9f)
        curveTo(10.3f, 12.9f, 10.6f, 11.6f, 11.6f, 11f)
        curveTo(12.6f, 10.4f, 13.9f, 10.7f, 14.5f, 11.7f)
        curveTo(15.1f, 12.7f, 14.8f, 14f, 13.8f, 14.6f)
        close()
    }
}
