package com.zeroui.kit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zeroui.kit.glass.LiquidToggle
import com.zeroui.kit.glass.LocalLiquidIntensity
import com.zeroui.kit.glass.isLiquidEnabled
import com.zeroui.kit.theme.ListDensity
import com.zeroui.kit.theme.LocalListDensity
import com.zeroui.kit.theme.ProvideContentColor
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.Text as MaterialText
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.preference.ArrowPreference as MiuixArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference as MiuixDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference as MiuixSwitchPreference

private const val DisabledAlpha = 0.38f

private val CompactRowVertical = 11.dp
private val CompactRowMargin = PaddingValues(horizontal = 16.dp, vertical = CompactRowVertical)

/** Default padding of list rows for the active density. */
@Composable
fun rowMargin(): PaddingValues =
    if (LocalListDensity.current == ListDensity.Compact) CompactRowMargin else BasicComponentDefaults.InsideMargin

@Composable
private fun rowVerticalPadding(): Dp =
    if (LocalListDensity.current == ListDensity.Compact) CompactRowVertical else 16.dp

/** Material list row with leading, trailing and bottom slots. */
@Composable
private fun MaterialRow(
    title: String?,
    summary: String?,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    enabled: Boolean = true,
    titleTrailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    ProvideContentColor(colors.onSurface) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, role = role, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp, vertical = rowVerticalPadding()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .alpha(if (enabled) 1f else DisabledAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (startAction != null) {
                    Box(modifier = Modifier.padding(end = 16.dp), contentAlignment = Alignment.Center) {
                        ProvideContentColor(colors.onSurfaceVariant, startAction)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (content != null) {
                        content()
                    } else {
                        if (title != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MaterialText(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurface,
                                )
                                if (titleTrailing != null) {
                                    Box(modifier = Modifier.padding(start = 8.dp)) { titleTrailing() }
                                }
                            }
                        }
                        if (!summary.isNullOrEmpty()) {
                            MaterialText(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (endActions != null) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = endActions,
                    )
                }
            }
            if (bottomAction != null) {
                Box(modifier = Modifier.padding(top = 12.dp)) { bottomAction() }
            }
        }
    }
}

/** Row with a title, a summary and optional slots. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = rowMargin(),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (isMaterial()) {
        MaterialRow(
            title = title,
            summary = summary,
            modifier = modifier,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            onClick = onClick,
            enabled = enabled,
        )
    } else {
        MiuixBasicComponent(
            modifier = modifier,
            title = title,
            summary = summary,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            insideMargin = insideMargin,
            onClick = onClick,
            enabled = enabled,
        )
    }
}

/** Row with custom content in the title slot. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = rowMargin(),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isMaterial()) {
        MaterialRow(
            title = null,
            summary = null,
            modifier = modifier,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            onClick = onClick,
            enabled = enabled,
            content = content,
        )
    } else {
        MiuixBasicComponent(
            modifier = modifier,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            insideMargin = insideMargin,
            onClick = onClick,
            enabled = enabled,
            content = content,
        )
    }
}

/** Navigation row. */
@Composable
fun ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = rowMargin(),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (isMaterial()) {
        MaterialRow(
            title = title,
            summary = summary,
            modifier = modifier,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            onClick = onClick,
            enabled = enabled,
        )
    } else {
        MiuixArrowPreference(
            title = title,
            modifier = modifier,
            summary = summary,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            insideMargin = insideMargin,
            onClick = onClick,
            enabled = enabled,
        )
    }
}

/** Row that toggles a switch. */
@Composable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    insideMargin: PaddingValues = rowMargin(),
    enabled: Boolean = true,
) {
    if (!isMaterial()) {
        if (!isLiquidEnabled()) {
            MiuixSwitchPreference(
                checked = checked,
                onCheckedChange = onCheckedChange,
                title = title,
                modifier = modifier,
                summary = summary,
                startAction = startAction,
                insideMargin = insideMargin,
                enabled = enabled,
            )
            return
        }
        val currentChecked by rememberUpdatedState(checked)
        val selectedProvider = remember { { currentChecked } }
        MiuixBasicComponent(
            modifier = modifier,
            title = title,
            summary = summary,
            startAction = startAction,
            insideMargin = insideMargin,
            enabled = enabled,
            onClick = if (enabled) {
                { onCheckedChange(!currentChecked) }
            } else {
                null
            },
            endActions = {
                LiquidToggle(
                    selected = selectedProvider,
                    onSelect = onCheckedChange,
                    enabled = enabled,
                    intensity = LocalLiquidIntensity.current,
                )
            },
        )
        return
    }
    val haptic = LocalHapticFeedback.current
    MaterialRow(
        title = title,
        summary = summary,
        modifier = modifier,
        startAction = startAction,
        endActions = { MaterialToggle(checked = checked, onCheckedChange = null, enabled = enabled) },
        onClick = {
            onCheckedChange(!checked)
            haptic.performHapticFeedback(
                if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
            )
        },
        role = Role.Switch,
        enabled = enabled,
    )
}

/** Row that selects one value from a dropdown. */
@Composable
fun DropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    insideMargin: PaddingValues = rowMargin(),
    enabled: Boolean = true,
) {
    if (!isMaterial()) {
        MiuixDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            startAction = startAction,
            insideMargin = insideMargin,
            enabled = enabled,
            onSelectedIndexChange = onSelectedIndexChange,
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    Box(modifier = modifier) {
        MaterialRow(
            title = title,
            summary = summary,
            startAction = startAction,
            enabled = enabled,
            onClick = { expanded = true },
            endActions = if (items.isNotEmpty()) {
                {
                    MaterialText(
                        text = items[safeIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )
                }
            } else {
                null
            },
        )
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                items.forEachIndexed { index, text ->
                    val selected = index == safeIndex
                    DropdownMenuItem(
                        text = {
                            MaterialText(
                                text = text,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelectedIndexChange(index)
                        },
                        trailingIcon = if (selected) {
                            {
                                MaterialIcon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
