package com.hihi.recorder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge as MaterialBadge
import androidx.compose.material3.BadgedBox as MaterialBadgedBox
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults as MaterialCardDefaults
import androidx.compose.material3.CircularProgressIndicator as MaterialProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider as MaterialHorizontalDivider
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as MaterialNavigationBar
import androidx.compose.material3.NavigationBarItem as MaterialNavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults as MaterialNavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton as MaterialRadioButton
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hihi.recorder.R
import com.hihi.recorder.data.UiStyle
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixProgress
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults as MiuixNavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults as MiuixTabRowDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults as MiuixTextFieldDefaults
import top.yukonga.miuix.kmp.basic.TooltipBox as MiuixTooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Immutable
data class AppPalette(
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val error: Color,
    val onError: Color,
    val outline: Color,
    val divider: Color,
    val isDark: Boolean,
) {
    val heroContainer: Color get() = primaryContainer
    val onHeroContainer: Color get() = onPrimaryContainer
}

@Composable
fun appPalette(): AppPalette {
    return if (LocalUiStyle.current == UiStyle.MIUIX) {
        val colors = MiuixTheme.colorScheme
        AppPalette(
            background = colors.background,
            surface = colors.surface,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            surfaceContainerHighest = colors.surfaceContainerHighest,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariantSummary,
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            error = colors.error,
            onError = colors.onError,
            outline = colors.outline,
            divider = colors.dividerLine,
            isDark = colors.background.luminance() < 0.5f,
        )
    } else {
        val colors = MaterialTheme.colorScheme
        AppPalette(
            background = colors.background,
            surface = colors.surface,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            surfaceContainerHighest = colors.surfaceContainerHighest,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant,
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            error = colors.error,
            onError = colors.onError,
            outline = colors.outline,
            divider = colors.outlineVariant,
            isDark = colors.background.luminance() < 0.5f,
        )
    }
}

/**
 * Extra space reserved above the navigation bar while the floating recording
 * toolbar is visible. Kept as a composition local so every scrolling screen
 * clears the toolbar without threading a parameter through each one.
 */
val LocalExtraBottomPadding = staticCompositionLocalOf { 0.dp }

/** Bottom content padding that clears the floating bars and system navigation area. */
@Composable
fun appBottomContentPadding(): Dp {
    val navigationBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    return navigationBarInset + 118.dp + LocalExtraBottomPadding.current
}

enum class AppTextStyle {
    Display,
    Headline,
    Title,
    Subtitle,
    Body,
    SmallBody,
    Caption,
    Label,
    Footnote,
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.Body,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        val miuixStyle = when (style) {
            AppTextStyle.Display -> MiuixTheme.textStyles.title1
            AppTextStyle.Headline -> MiuixTheme.textStyles.title2
            AppTextStyle.Title -> MiuixTheme.textStyles.title3
            AppTextStyle.Subtitle -> MiuixTheme.textStyles.title4
            AppTextStyle.Body -> MiuixTheme.textStyles.body1
            AppTextStyle.SmallBody -> MiuixTheme.textStyles.body2
            AppTextStyle.Caption -> MiuixTheme.textStyles.footnote1
            AppTextStyle.Label -> MiuixTheme.textStyles.footnote1
            AppTextStyle.Footnote -> MiuixTheme.textStyles.footnote2
        }
        MiuixText(
            text = text,
            modifier = modifier,
            color = color,
            style = miuixStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
        )
    } else {
        val materialStyle = when (style) {
            AppTextStyle.Display -> MaterialTheme.typography.displaySmall
            AppTextStyle.Headline -> MaterialTheme.typography.headlineSmall
            AppTextStyle.Title -> MaterialTheme.typography.titleLarge
            AppTextStyle.Subtitle -> MaterialTheme.typography.titleMedium
            AppTextStyle.Body -> MaterialTheme.typography.bodyLarge
            AppTextStyle.SmallBody -> MaterialTheme.typography.bodyMedium
            AppTextStyle.Caption -> MaterialTheme.typography.bodySmall
            AppTextStyle.Label -> MaterialTheme.typography.labelLarge
            AppTextStyle.Footnote -> MaterialTheme.typography.labelMedium
        }
        MaterialText(
            text = text,
            modifier = modifier,
            color = color,
            style = materialStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

@Composable
fun AppIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = appPalette().onSurface,
) {
    MiuixIcon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color = appPalette().surfaceContainer,
    contentColor: Color = appPalette().onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixCard(
            modifier = modifier.fillMaxWidth(),
            insideMargin = PaddingValues(0.dp),
            colors = MiuixCardDefaults.defaultColors(
                color = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    } else {
        MaterialCard(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = MaterialCardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}

@Composable
fun AppSectionTitle(text: String, modifier: Modifier = Modifier) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        SmallTitle(
            text = text,
            modifier = modifier.padding(start = 6.dp, top = 6.dp, bottom = 1.dp),
        )
    } else {
        AppText(
            text = text,
            modifier = modifier.padding(start = 8.dp, top = 12.dp, bottom = 3.dp),
            style = AppTextStyle.Label,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    val palette = appPalette()
    val dividerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixHorizontalDivider(
            modifier = dividerModifier,
            thickness = 0.75.dp,
            color = palette.divider,
        )
    } else {
        MaterialHorizontalDivider(
            modifier = dividerModifier,
            thickness = 1.dp,
            color = palette.divider,
        )
    }
}

@Composable
fun AppRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixBasicComponent(
            modifier = modifier.fillMaxWidth(),
            title = title,
            summary = summary?.takeIf { it.isNotBlank() },
            startAction = leadingIcon?.let { icon ->
                {
                    AppIcon(
                        icon = icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = if (enabled) palette.onSurfaceVariant else palette.outline,
                    )
                }
            },
            endActions = when {
                trailing != null -> {
                    { trailing() }
                }

                enabled && onClick != null -> {
                    {
                        AppIcon(
                            icon = MiuixIcons.ChevronForward,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = palette.onSurfaceVariant,
                        )
                    }
                }

                else -> null
            },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            onClick = if (enabled && onClick != null) onClick else null,
            holdDownState = false,
            enabled = enabled,
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                AppIcon(
                    icon = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = if (enabled) palette.onSurfaceVariant else palette.outline,
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = title,
                    style = AppTextStyle.Subtitle,
                    color = if (enabled) palette.onSurface else palette.outline,
                )
                if (!summary.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    AppText(
                        text = summary,
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            } else if (enabled && onClick != null) {
                Spacer(Modifier.width(8.dp))
                AppIcon(
                    icon = MiuixIcons.ChevronForward,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = palette.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        MaterialSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

@Composable
fun AppRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixRadioButton(
            selected = selected,
            onClick = null,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        MaterialRadioButton(
            selected = selected,
            onClick = null,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

@Composable
fun AppSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        SwitchPreference(
            checked = checked,
            onCheckedChange = onCheckedChange,
            title = title,
            summary = summary?.takeIf { it.isNotBlank() },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            enabled = enabled,
        )
    } else {
        AppRow(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) },
            trailing = {
                AppSwitch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled,
                )
            },
        )
    }
}

@Composable
fun AppSelectRow(
    title: String,
    value: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        ArrowPreference(
            title = title,
            summary = summary?.takeIf { it.isNotBlank() },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            onClick = onClick,
            enabled = enabled,
            endActions = {
                AppText(
                    text = value,
                    modifier = Modifier.widthIn(max = 180.dp),
                    style = AppTextStyle.Footnote,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    } else {
        AppRow(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = onClick,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = value,
                        modifier = Modifier.widthIn(max = 180.dp),
                        style = AppTextStyle.Footnote,
                        color = palette.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(4.dp))
                    AppIcon(
                        icon = MiuixIcons.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = palette.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
) {
    val palette = appPalette()
    val container = if (destructive) palette.error else palette.primary
    val content = if (destructive) palette.onError else palette.onPrimary
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading,
            minHeight = 48.dp,
            cornerRadius = 16.dp,
            colors = MiuixButtonDefaults.buttonColors(
                color = container,
                contentColor = content,
            ),
        ) {
            if (loading) {
                MiuixProgress(
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
                        .progressIndicatorColors(
                            foregroundColor = content,
                            backgroundColor = content.copy(alpha = 0.25f),
                        ),
                )
                Spacer(Modifier.width(9.dp))
            }
            MiuixText(text = text, fontWeight = FontWeight.SemiBold)
        }
    } else {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.large,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
            ),
        ) {
            if (loading) {
                MaterialProgress(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
                Spacer(Modifier.width(9.dp))
            }
            MaterialText(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    destructive: Boolean = false,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            minWidth = 0.dp,
            minHeight = 38.dp,
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = when {
                destructive -> MiuixButtonDefaults.textButtonColors(
                    textColor = palette.error,
                )

                primary -> MiuixButtonDefaults.textButtonColorsPrimary()
                else -> MiuixButtonDefaults.textButtonColors()
            },
        )
    } else {
        MaterialTextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            MaterialText(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    !enabled -> palette.onSurfaceVariant.copy(alpha = 0.38f)
                    destructive -> palette.error
                    primary -> palette.primary
                    else -> palette.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = appPalette().onSurfaceVariant,
) {
    val palette = appPalette()
    val iconContent: @Composable () -> Unit = {
        AppIcon(
            icon = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp),
            tint = tint,
        )
    }
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        val button: @Composable () -> Unit = {
            MiuixIconButton(
                onClick = onClick,
                modifier = modifier.size(40.dp),
                enabled = enabled,
                backgroundColor = Color.Transparent,
                cornerRadius = 20.dp,
                minHeight = 40.dp,
                minWidth = 40.dp,
                content = iconContent,
            )
        }
        if (!contentDescription.isNullOrBlank()) {
            MiuixTooltipBox(
                text = contentDescription,
                enabled = enabled,
                containerColor = palette.surfaceContainerHigh,
                contentColor = palette.onSurface,
                content = button,
            )
        } else {
            button()
        }
    } else {
        MaterialIconButton(
            onClick = onClick,
            modifier = modifier.size(40.dp),
            enabled = enabled,
            content = iconContent,
        )
    }
}

@Composable
fun AppSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = appPalette().primary,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixProgress(
            modifier = modifier,
            size = size,
            strokeWidth = 2.dp,
            colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
                .progressIndicatorColors(
                    foregroundColor = color,
                    backgroundColor = color.copy(alpha = 0.22f),
                ),
        )
    } else {
        MaterialProgress(
            modifier = modifier.size(size),
            strokeWidth = 2.dp,
            color = color,
        )
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            insideMargin = DpSize(width = 15.dp, height = 13.dp),
            colors = MiuixTextFieldDefaults.textFieldColors(
                backgroundColor = palette.surfaceContainerHigh,
                labelColor = palette.onSurfaceVariant,
                borderColor = palette.primary,
            ),
            cornerRadius = 14.dp,
            label = placeholder,
            useLabelAsPlaceholder = true,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.onSurface),
            placeholder = if (placeholder.isNotEmpty()) {
                {
                    MaterialText(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surfaceContainerHigh,
                unfocusedContainerColor = palette.surfaceContainerHigh,
                focusedBorderColor = palette.primary,
                unfocusedBorderColor = palette.outline.copy(alpha = 0.45f),
                cursorColor = palette.primary,
                focusedTextColor = palette.onSurface,
                unfocusedTextColor = palette.onSurface,
            ),
        )
    }
}

@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    message: String? = null,
    buttons: @Composable RowScope.() -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        WindowDialog(
            show = true,
            title = title,
            summary = message?.takeIf { it.isNotBlank() },
            backgroundColor = palette.surfaceContainer,
            titleColor = palette.onSurface,
            summaryColor = palette.onSurfaceVariant,
            onDismissRequest = onDismissRequest,
            maxWidth = 480.dp,
            cornerRadius = 28.dp,
        ) {
            Column {
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
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(palette.surfaceContainer)
                        .border(
                            1.dp,
                            palette.outline.copy(alpha = 0.35f),
                            MaterialTheme.shapes.extraLarge,
                        )
                        .padding(22.dp),
                ) {
                    AppText(
                        text = title,
                        style = AppTextStyle.Title,
                        color = palette.onSurface,
                    )
                    if (!message.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        AppText(
                            text = message,
                            style = AppTextStyle.SmallBody,
                            color = palette.onSurfaceVariant,
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
}

@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String,
    destructive: Boolean = false,
) {
    AppDialog(
        title = title,
        message = message,
        onDismissRequest = onDismissRequest,
        buttons = {
            AppTextButton(
                text = dismissLabel,
                onClick = onDismissRequest,
            )
            Spacer(Modifier.width(6.dp))
            AppTextButton(
                text = confirmLabel,
                onClick = onConfirm,
                primary = !destructive,
                destructive = destructive,
            )
        },
    )
}

data class SelectOption<T>(
    val value: T,
    val label: String,
    val summary: String? = null,
)

@Composable
fun <T> AppSelectDialog(
    title: String,
    options: List<SelectOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val palette = appPalette()
    AppDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        buttons = {
            AppTextButton(
                text = stringResource(R.string.action_close),
                onClick = onDismissRequest,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                palette.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable {
                            onSelect(option.value)
                            onDismissRequest()
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = option.label,
                            style = AppTextStyle.Subtitle,
                            color = if (isSelected) palette.primary else palette.onSurface,
                        )
                        if (!option.summary.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            AppText(
                                text = option.summary,
                                style = AppTextStyle.Footnote,
                                color = palette.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    AppRadioButton(selected = isSelected)
                }
            }
        }
    }
}

@Composable
fun AppInputDialog(
    title: String,
    message: String,
    initialValue: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AppDialog(
        title = title,
        message = message,
        onDismissRequest = onDismissRequest,
        buttons = {
            AppTextButton(text = dismissLabel, onClick = onDismissRequest)
            Spacer(Modifier.width(6.dp))
            AppTextButton(
                text = confirmLabel,
                onClick = { onConfirm(value) },
                primary = true,
                enabled = value.isNotBlank(),
            )
        },
    ) {
        AppTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = placeholder,
            keyboardType = keyboardType,
        )
    }
}

@Composable
fun <T> AppSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX && options.isNotEmpty()) {
        val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
        MiuixTabRow(
            tabs = options.map { it.second },
            selectedTabIndex = selectedIndex,
            onTabSelected = { index ->
                if (enabled && index != selectedIndex) {
                    onSelect(options[index].first)
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .alpha(if (enabled) 1f else 0.45f),
            colors = MiuixTabRowDefaults.tabRowColors(
                backgroundColor = palette.surfaceContainerHigh,
                contentColor = palette.onSurfaceVariant,
                selectedBackgroundColor = palette.primary,
                selectedContentColor = palette.onPrimary,
            ),
            minWidth = 0.dp,
            maxWidth = 10_000.dp,
            height = 42.dp,
            cornerRadius = 13.dp,
            itemSpacing = 4.dp,
        )
    } else {
        val outerShape = RoundedCornerShape(16.dp)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(outerShape)
                .background(palette.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val fill by animateColorAsState(
                    targetValue = if (isSelected) palette.primary else Color.Transparent,
                    label = "segmentFill",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(fill)
                        .clickable(enabled = enabled && !isSelected) { onSelect(value) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = label,
                        style = AppTextStyle.Label,
                        color = when {
                            isSelected -> palette.onPrimary
                            !enabled -> palette.outline
                            else -> palette.onSurfaceVariant
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else null,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    subtitle: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val palette = appPalette()
    val navigationBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val extraBottomPadding = LocalExtraBottomPadding.current
    val snackbarHost: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 108.dp + navigationBarInset + extraBottomPadding),
        ) {
            SnackbarHost(state = snackbarHostState)
        }
    }

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixScaffold(
            modifier = modifier,
            topBar = {
                top.yukonga.miuix.kmp.basic.SmallTopAppBar(
                    title = title,
                    subtitle = subtitle,
                    color = palette.surface,
                    titleColor = palette.onSurface,
                    subtitleColor = palette.onSurfaceVariant,
                    actions = actions,
                )
            },
            snackbarHost = snackbarHost,
            containerColor = palette.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            content = content,
        )
    } else {
        MaterialScaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            AppText(
                                text = title,
                                style = AppTextStyle.Title,
                                color = palette.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            AppText(
                                text = subtitle,
                                style = AppTextStyle.Footnote,
                                color = palette.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.surface,
                        scrolledContainerColor = palette.surfaceContainer,
                        titleContentColor = palette.onSurface,
                        actionIconContentColor = palette.onSurface,
                    ),
                )
            },
            snackbarHost = snackbarHost,
            containerColor = palette.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            content = content,
        )
    }
}

data class AppNavItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

internal fun navBadgeText(count: Int): String = if (count > 99) "99+" else count.toString()

/**
 * Bottom navigation assembled from the official Miuix and Material 3 components.
 *
 * Miuix owns the selected-label behaviour through [NavigationBarDisplayMode], so
 * there is no custom sliding indicator, blur layer, or hand-built tab layout.
 */
@Composable
fun AppNavigationBar(
    items: List<AppNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    selectedLabelsOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        // Documented Miuix colour recipe: on-surface content for unselected
        // items (the component applies its own secondary opacity) and the theme
        // primary for the selected item. Both resolve correctly in dark mode.
        val itemColors = MiuixNavigationBarDefaults.navigationBarItemColors(
            unselectedContentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            selectedContentColor = MiuixTheme.colorScheme.primary,
        )
        MiuixNavigationBar(
            modifier = modifier.fillMaxWidth(),
            mode = if (selectedLabelsOnly) {
                NavigationBarDisplayMode.IconWithSelectedLabel
            } else {
                NavigationBarDisplayMode.IconAndText
            },
        ) {
            items.forEachIndexed { index, item ->
                MiuixNavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                    colors = itemColors,
                    badge = if (item.badgeCount > 0) {
                        {
                            MiuixBadge {
                                MiuixText(text = navBadgeText(item.badgeCount))
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    } else {
        // Material 3's defaults map selected content onto secondaryContainer,
        // which is too low-contrast after the Miuix-to-Material colour mapping.
        // Use the high-contrast on-surface token plus a light primary tint so
        // both themes stay legible.
        val itemColors = MaterialNavigationBarItemDefaults.colors(
            selectedIconColor = palette.onSurface,
            selectedTextColor = palette.onSurface,
            indicatorColor = palette.primary.copy(alpha = if (palette.isDark) 0.24f else 0.12f),
            unselectedIconColor = palette.onSurfaceVariant,
            unselectedTextColor = palette.onSurfaceVariant,
        )
        MaterialNavigationBar(
            modifier = modifier.fillMaxWidth(),
            containerColor = palette.surface,
            contentColor = palette.onSurface,
            tonalElevation = 0.dp,
        ) {
            items.forEachIndexed { index, item ->
                MaterialNavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = {
                        if (item.badgeCount > 0) {
                            MaterialBadgedBox(
                                badge = {
                                    MaterialBadge {
                                        MaterialText(text = navBadgeText(item.badgeCount))
                                    }
                                },
                            ) {
                                MaterialIcon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            MaterialIcon(
                                imageVector = item.icon,
                                contentDescription = null,
                            )
                        }
                    },
                    label = {
                        MaterialText(
                            text = item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    alwaysShowLabel = !selectedLabelsOnly,
                    colors = itemColors,
                )
            }
        }
    }
}

/** Keeps a select dialog's value across rotations. */
@Composable
fun rememberSelectDialogState(): Pair<Boolean, (Boolean) -> Unit> {
    var open by rememberSaveable { mutableStateOf(false) }
    return open to { open = it }
}
