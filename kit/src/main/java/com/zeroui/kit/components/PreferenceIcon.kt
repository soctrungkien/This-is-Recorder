package com.zeroui.kit.components

import com.zeroui.kit.glass.rememberSquircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zeroui.kit.R
import com.zeroui.kit.components.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.MaterialTheme
import com.zeroui.kit.theme.isMaterial

enum class SettingIconStyle(@androidx.annotation.StringRes val labelRes: Int) {
    Line(R.string.icon_style_line), Filled(R.string.icon_style_filled), Tinted(R.string.icon_style_tinted),
    Colorful(R.string.icon_style_colorful),
}

val LocalSettingIconStyle = staticCompositionLocalOf { SettingIconStyle.Filled }

enum class IconHue(val color: Color) {
    Blue(Color(0xFF3482FF)),
    Indigo(Color(0xFF5B6CF0)),
    Purple(Color(0xFF8A5CF5)),
    Pink(Color(0xFFF2508C)),
    Red(Color(0xFFF5473B)),
    Orange(Color(0xFFFF8A00)),
    Amber(Color(0xFFF2A100)),
    Green(Color(0xFF2DBE60)),
    Teal(Color(0xFF14B0BE)),
    Gray(Color(0xFF8E9AAF)),
}

class SettingIcon(
    private val line: () -> ImageVector,
    private val filled: () -> ImageVector,
    val hue: IconHue = IconHue.Gray,
) {
    @Composable
    fun current(): ImageVector =
        if (LocalSettingIconStyle.current == SettingIconStyle.Line) line() else filled()
}

private val ColorfulTile = 30.dp
private val ColorfulGlyph = 19.dp

@Composable
fun PreferenceIcon(icon: SettingIcon) {
    val style = LocalSettingIconStyle.current
    val material = isMaterial()
    if (style == SettingIconStyle.Colorful) {
        Box(
            modifier = Modifier
                .then(if (material) Modifier else Modifier.padding(end = 6.dp))
                .size(ColorfulTile)
                .clip(rememberSquircleShape(9.dp))
                .background(icon.hue.color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon.current(),
                contentDescription = null,
                modifier = Modifier.size(ColorfulGlyph),
                tint = Color.White,
            )
        }
        return
    }
    val tinted = style == SettingIconStyle.Tinted

    if (material) {
        Icon(
            imageVector = icon.current(),
            contentDescription = null,
            tint = if (tinted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Icon(
        imageVector = icon.current(),
        contentDescription = null,
        modifier = Modifier.padding(end = 6.dp),
        tint = if (tinted) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground,
    )
}
