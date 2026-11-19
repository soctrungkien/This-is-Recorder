package com.zeroui.kit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.zeroui.kit.theme.ZeroTheme
import com.zeroui.kit.theme.isMaterial
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.Text as MaterialText
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor

/** Text styled by the active component family. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = ZeroTheme.typography.body,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    if (isMaterial()) {
        MaterialText(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            style = style,
        )
    } else {
        MiuixText(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            style = style,
        )
    }
}

/** Painter icon tinted by the active content color when [tint] is unspecified. */
@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (isMaterial()) {
        MaterialIcon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (tint == Color.Unspecified) MaterialLocalContentColor.current else tint,
        )
    } else {
        MiuixIcon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (tint == Color.Unspecified) MiuixLocalContentColor.current else tint,
        )
    }
}

/** Vector icon tinted by the active content color when [tint] is unspecified. */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (isMaterial()) {
        MaterialIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (tint == Color.Unspecified) MaterialLocalContentColor.current else tint,
        )
    } else {
        MiuixIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (tint == Color.Unspecified) MiuixLocalContentColor.current else tint,
        )
    }
}
