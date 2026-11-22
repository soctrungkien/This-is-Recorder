package com.hihi.recorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Item
import com.itos.coui.radiobutton.RadioButton as CouiRadioButton
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.shape.CouiShape
import com.itos.coui.switch.CouiSwitchDefaults
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme

/**
 * Native COUI building blocks used by [Design] when [UiStyle.COUI] is active.
 *
 * The palette and typography come from the surrounding [CouiTheme]; the app
 * only supplies its own labels and state. Keeping these wrappers in one file
 * avoids duplicating the COUI colour plumbing in every screen.
 */

@Composable
internal fun CouiCardSurface(
    modifier: Modifier,
    containerColor: Color,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CouiPageDefaults.shape)
            .background(containerColor)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
internal fun CouiSectionTitle(text: String, modifier: Modifier = Modifier) {
    CouiText(
        modifier = modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp),
        text = text,
        kind = CouiTextKind.GroupTitle,
        color = CouiTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
}

@Composable
internal fun CouiDivider(modifier: Modifier = Modifier, horizontalPadding: Dp = 16.dp) {
    val thickness = with(LocalDensity.current) {
        0.33.dp.roundToPx().coerceAtLeast(1).toDp()
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(thickness)
            .background(CouiTheme.colorScheme.outlineVariant),
    )
}

@Composable
internal fun CouiRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val dark = CouiTheme.isDark
    val colors = CouiPageDefaults.themeColors(dark)
    Item(
        modifier = modifier,
        icon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(CouiPageDefaults.iconSize),
                    tint = if (enabled) colors.summary else colors.disabled,
                )
            }
        },
        onClick = if (enabled) onClick else null,
        enabled = enabled,
        title = {
            CouiText(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) colors.title else colors.disabled,
                textAlign = TextAlign.Start,
            )
        },
        summary = summary?.takeIf { it.isNotBlank() }?.let { value ->
            {
                CouiText(
                    text = value,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) colors.summary else colors.disabled,
                    textAlign = TextAlign.Start,
                )
            }
        },
        trailing = {
            if (trailing != null) {
                trailing()
            } else if (enabled && onClick != null) {
                CouiChevron(dark)
            }
        },
    )
}

@Composable
internal fun CouiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val dark = CouiTheme.isDark
    val colors = CouiPageDefaults.themeColors(dark)
    Item(
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        enabled = enabled,
        title = {
            CouiText(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) colors.title else colors.disabled,
                textAlign = TextAlign.Start,
            )
        },
        summary = summary?.takeIf { it.isNotBlank() }?.let { value ->
            {
                CouiText(
                    text = value,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) colors.summary else colors.disabled,
                    textAlign = TextAlign.Start,
                )
            }
        },
        trailing = {
            CouiSwitch(
                modifier = Modifier.padding(start = 16.dp),
                checked = checked,
                onCheckedChange = null,
                dark = dark,
                colors = CouiSwitchDefaults.colors(colors.accent, dark),
                enabled = enabled,
                onDragCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
internal fun CouiSelectRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val dark = CouiTheme.isDark
    val colors = CouiPageDefaults.themeColors(dark)
    Item(
        modifier = modifier,
        onClick = if (enabled) onClick else null,
        enabled = enabled,
        title = {
            CouiText(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) colors.title else colors.disabled,
                textAlign = TextAlign.Start,
            )
        },
        summary = summary?.takeIf { it.isNotBlank() }?.let { text ->
            {
                CouiText(
                    text = text,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) colors.summary else colors.disabled,
                    textAlign = TextAlign.Start,
                )
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CouiText(
                    modifier = Modifier.widthIn(max = 180.dp),
                    text = value,
                    kind = CouiTextKind.ItemValue,
                    color = if (enabled) colors.summary else colors.disabled,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CouiChevron(dark)
            }
        },
    )
}

@Composable
private fun CouiChevron(dark: Boolean) {
    Icon(
        imageVector = CouiIcons.Filled.Next,
        contentDescription = null,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(width = 12.dp, height = 24.dp),
        tint = if (dark) Color(0x4DFFFFFF) else Color(0x42000000),
    )
}

@Composable
internal fun CouiSwitchControl(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = CouiTheme.isDark
    CouiSwitch(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        dark = dark,
        colors = CouiSwitchDefaults.colors(CouiTheme.colorScheme.accent, dark),
        enabled = enabled,
    )
}

@Composable
internal fun CouiRadioControl(
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CouiRadioButton(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
    )
}

@Composable
internal fun CouiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
) {
    val palette = appPalette()
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorScheme
    val interactive = enabled && !loading
    val container = if (destructive) palette.error else colors.accent
    val content = if (destructive) palette.onError else colors.onAccent
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CouiCapsuleShape)
            .background(if (interactive) container else container.copy(alpha = 0.42f))
            .then(
                if (interactive) {
                    Modifier.couiButtonFeedback(dark = dark, interactionSource = source)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = interactive,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = content,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(9.dp))
            }
            CouiText(
                text = text,
                kind = CouiTextKind.ItemTitle,
                color = content,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun CouiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    destructive: Boolean = false,
) {
    val palette = appPalette()
    val dark = CouiTheme.isDark
    val color = when {
        !enabled -> palette.onSurfaceVariant.copy(alpha = 0.38f)
        destructive -> palette.error
        primary -> palette.primary
        else -> palette.onSurfaceVariant
    }
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(CouiCapsuleShape)
            .then(
                if (enabled) {
                    Modifier.couiButtonFeedback(
                        dark = dark,
                        interactionSource = source,
                        pressFeedback = false,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CouiText(
            text = text,
            kind = CouiTextKind.ItemValue,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun CouiIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = appPalette().onSurfaceVariant,
) {
    val dark = CouiTheme.isDark
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (enabled) {
                    Modifier.couiButtonFeedback(dark = dark, interactionSource = source)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp),
            tint = tint,
        )
    }
}

@Composable
internal fun CouiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = CouiTheme.colorScheme
    val body = CouiTheme.typography.body
    val shape = CouiShape(14.dp, 3f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = body.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.accent),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.onSurface.copy(alpha = 0.05f))
                    .border(0.75.dp, colors.outlineVariant, shape)
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = body.copy(color = colors.onSurfaceVariant.copy(alpha = 0.62f)),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
internal fun CouiDialog(
    title: String,
    onDismissRequest: () -> Unit,
    message: String? = null,
    buttons: @Composable RowScope.() -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorScheme
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (dark) 0.6f else 0.2f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .fillMaxWidth()
                    .widthIn(max = CouiDialogDefaults.maxWidth)
                    .clip(CouiDialogDefaults.shape)
                    .background(colors.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                CouiText(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    kind = CouiTextKind.SheetTitle,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (!message.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    CouiText(
                        modifier = Modifier.fillMaxWidth(),
                        text = message,
                        kind = CouiTextKind.ItemSummary,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (content != null) {
                    Spacer(Modifier.height(16.dp))
                    content()
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    content = buttons,
                )
            }
        }
    }
}

@Composable
internal fun CouiSegmented(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // minSdk is 34, so COUI's native SegmentedControl is always available.
    com.itos.coui.segmentbutton.SegmentedControl(
        modifier = modifier.fillMaxWidth(),
        labels = labels,
        selectedIndex = selectedIndex.coerceIn(0, labels.lastIndex.coerceAtLeast(0)),
        onSelected = onSelected,
        dark = CouiTheme.isDark,
        enabled = enabled,
    )
}

/** COUI-styled bottom bar; COUI's own NavigationBar needs API 34 + CouiBackdrop. */
@Composable
internal fun CouiNavigationBar(
    items: List<AppNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    selectedLabelsOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CouiTheme.colorScheme
    val dark = CouiTheme.isDark
    val shape = CouiShape(16.dp, 3f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .heightIn(min = 60.dp)
            .background(colors.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val source = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(
                        if (selected) {
                            Modifier.background(colors.accent.copy(alpha = if (dark) 0.24f else 0.12f))
                        } else {
                            Modifier
                        },
                    )
                    .couiButtonFeedback(dark = dark, interactionSource = source, pressFeedback = false)
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = true,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (selected) colors.accent else colors.onSurfaceVariant,
                    )
                    if (item.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            CouiText(
                                text = navBadgeText(item.badgeCount),
                                kind = CouiTextKind.GroupFooter,
                                color = colors.onAccent,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                if (!selectedLabelsOnly || selected) {
                    CouiText(
                        text = item.label,
                        kind = CouiTextKind.GroupFooter,
                        color = if (selected) colors.accent else colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Compact COUI header used by [AppScaffold] in COUI mode. */
@Composable
internal fun CouiTopBar(
    title: String,
    subtitle: String,
    navigationIcon: (@Composable () -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 52.dp)
            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon?.invoke()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            CouiText(
                text = title,
                kind = CouiTextKind.TopBar,
                color = colors.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CouiText(
                text = subtitle,
                kind = CouiTextKind.GroupFooter,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}
