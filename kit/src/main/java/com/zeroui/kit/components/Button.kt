package com.zeroui.kit.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.Text as MaterialText
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/** Visual emphasis of a button. */
enum class ButtonVariant { Primary, Secondary }

/**
 * Button with slot content.
 *
 * @param containerColor Overrides the variant's container color when specified.
 * @param contentColor Overrides the variant's content color when specified.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Secondary,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    content: @Composable RowScope.() -> Unit,
) {
    if (!isMaterial()) {
        val base = if (variant == ButtonVariant.Primary) {
            ButtonDefaults.buttonColorsPrimary()
        } else {
            ButtonDefaults.buttonColors()
        }
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            cornerRadius = cornerRadius,
            minWidth = minWidth,
            minHeight = minHeight,
            colors = base.copy(
                color = containerColor.takeOrElse { base.color },
                contentColor = contentColor.takeOrElse { base.contentColor },
            ),
            insideMargin = insideMargin,
            content = content,
        )
        return
    }
    when {
        containerColor.isSpecified || contentColor.isSpecified -> MaterialButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = MaterialButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            content = content,
        )
        variant == ButtonVariant.Primary -> MaterialButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
        else -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
    }
}

/** Button with a single text label. */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Secondary,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    textStyle: TextStyle? = null,
) {
    if (!isMaterial()) {
        val base = if (variant == ButtonVariant.Primary) {
            ButtonDefaults.textButtonColorsPrimary()
        } else {
            ButtonDefaults.textButtonColors()
        }
        MiuixTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            cornerRadius = cornerRadius,
            minWidth = minWidth,
            minHeight = minHeight,
            colors = base.copy(
                color = containerColor.takeOrElse { base.color },
                textColor = contentColor.takeOrElse { base.textColor },
            ),
            insideMargin = insideMargin,
            textStyle = textStyle,
        )
        return
    }
    val sized = modifier.heightIn(min = minHeight)
    val label: @Composable RowScope.() -> Unit = {
        MaterialText(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = textStyle ?: androidx.compose.material3.LocalTextStyle.current,
        )
    }
    when {
        containerColor.isSpecified || contentColor.isSpecified -> MaterialButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            colors = MaterialButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            contentPadding = insideMargin,
            content = label,
        )
        variant == ButtonVariant.Primary -> MaterialButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            contentPadding = insideMargin,
            content = label,
        )
        else -> FilledTonalButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            contentPadding = insideMargin,
            content = label,
        )
    }
}

/** Icon button; [backgroundColor] selects the filled form. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    holdDownState: Boolean = false,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    content: @Composable () -> Unit,
) {
    if (!isMaterial()) {
        MiuixIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            holdDownState = holdDownState,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            minHeight = minHeight,
            minWidth = minWidth,
            content = content,
        )
        return
    }
    if (backgroundColor.isSpecified && backgroundColor != Color.Transparent) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    } else {
        MaterialIconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}
