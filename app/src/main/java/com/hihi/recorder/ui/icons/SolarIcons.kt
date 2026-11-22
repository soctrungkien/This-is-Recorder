package com.hihi.recorder.ui.icons

// Solar-style outline icons (https://github.com/480design/solar-icons, MIT).
// Hand-traced to ImageVector for the bottom navigation only.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun outline(
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
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = draw,
    )
}.build()

object SolarIcons {
    val Home: ImageVector = outline("SolarHome") {
        moveTo(3.5f, 10.2f)
        lineTo(12f, 3.5f)
        lineTo(20.5f, 10.2f)
        lineTo(20.5f, 20f)
        lineTo(3.5f, 20f)
        close()
        moveTo(9.5f, 20f)
        lineTo(9.5f, 14.5f)
        lineTo(14.5f, 14.5f)
        lineTo(14.5f, 20f)
    }

    val Videos: ImageVector = outline("SolarVideos") {
        moveTo(3.5f, 6f)
        lineTo(20.5f, 6f)
        lineTo(20.5f, 18f)
        lineTo(3.5f, 18f)
        close()
        moveTo(3.5f, 10f)
        lineTo(20.5f, 10f)
        moveTo(7.5f, 6f)
        lineTo(7.5f, 18f)
        moveTo(16.5f, 6f)
        lineTo(16.5f, 18f)
        moveTo(10.5f, 12.8f)
        lineTo(13.8f, 14.6f)
        lineTo(10.5f, 16.4f)
        close()
    }

    val Settings: ImageVector = outline("SolarSettings") {
        moveTo(12f, 8.2f)
        curveTo(9.9f, 8.2f, 8.2f, 9.9f, 8.2f, 12f)
        curveTo(8.2f, 14.1f, 9.9f, 15.8f, 12f, 15.8f)
        curveTo(14.1f, 15.8f, 15.8f, 14.1f, 15.8f, 12f)
        curveTo(15.8f, 9.9f, 14.1f, 8.2f, 12f, 8.2f)
        close()
        moveTo(19.2f, 13.6f)
        lineTo(17.6f, 12.9f)
        lineTo(17.6f, 11.1f)
        lineTo(19.2f, 10.4f)
        lineTo(18.2f, 8.2f)
        lineTo(16.6f, 8.9f)
        lineTo(15.3f, 7.9f)
        lineTo(15.6f, 6.2f)
        lineTo(13.2f, 5.6f)
        lineTo(12.3f, 7.1f)
        lineTo(10.5f, 7.1f)
        lineTo(9.6f, 5.6f)
        lineTo(7.2f, 6.2f)
        lineTo(7.5f, 7.9f)
        lineTo(6.2f, 8.9f)
        lineTo(4.6f, 8.2f)
        lineTo(3.6f, 10.4f)
        lineTo(5.2f, 11.1f)
        lineTo(5.2f, 12.9f)
        lineTo(3.6f, 13.6f)
        lineTo(4.6f, 15.8f)
        lineTo(6.2f, 15.1f)
        lineTo(7.5f, 16.1f)
        lineTo(7.2f, 17.8f)
        lineTo(9.6f, 18.4f)
        lineTo(10.5f, 16.9f)
        lineTo(12.3f, 16.9f)
        lineTo(13.2f, 18.4f)
        lineTo(15.6f, 17.8f)
        lineTo(15.3f, 16.1f)
        lineTo(16.6f, 15.1f)
        lineTo(18.2f, 15.8f)
        close()
    }
}
