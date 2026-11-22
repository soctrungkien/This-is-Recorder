package com.hihi.recorder.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hihi.recorder.R
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Seed for "follow wallpaper" — no explicit key color. */
internal const val KeyColorFromWallpaper: Int = 0
/** Default seed used when the custom picker is first opened. */
private const val DefaultCustomKeyColor: Int = 0xFF2196F3.toInt()

private data class KeyPreset(@StringRes val labelRes: Int, val argb: Int)

/** 15 presets from ZeroUI, in the same order. */
private val KeyPresets = listOf(
    KeyPreset(R.string.monet_color_red, 0xFFE5484D.toInt()),
    KeyPreset(R.string.monet_color_pink, 0xFFD6409F.toInt()),
    KeyPreset(R.string.monet_color_purple, 0xFF8E4EC6.toInt()),
    KeyPreset(R.string.monet_color_deep_purple, 0xFF6E56CF.toInt()),
    KeyPreset(R.string.monet_color_indigo, 0xFF3E63DD.toInt()),
    KeyPreset(R.string.monet_color_blue, 0xFF1B6BFF.toInt()),
    KeyPreset(R.string.monet_color_cyan, 0xFF05A2C2.toInt()),
    KeyPreset(R.string.monet_color_teal, 0xFF12A594.toInt()),
    KeyPreset(R.string.monet_color_green, 0xFF30A46C.toInt()),
    KeyPreset(R.string.monet_color_yellow, 0xFFF5D90A.toInt()),
    KeyPreset(R.string.monet_color_amber, 0xFFFFB224.toInt()),
    KeyPreset(R.string.monet_color_orange, 0xFFF76808.toInt()),
    KeyPreset(R.string.monet_color_brown, 0xFFAD7F58.toInt()),
    KeyPreset(R.string.monet_color_blue_grey, 0xFF687D91.toInt()),
    KeyPreset(R.string.monet_color_sakura, 0xFFF5A3B7.toInt()),
)

internal fun isCustomKeyColor(value: Int): Boolean =
    value != KeyColorFromWallpaper && KeyPresets.none { it.argb == value }

/**
 * Wallpaper + preset + custom swatch row, ported from ZeroUI's ThemeSettings.
 * [selected] is an ARGB seed; [KeyColorFromWallpaper] means follow wallpaper.
 */
@Composable
internal fun MonetKeyColorRow(
    selected: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val presetIndex = KeyPresets.indexOfFirst { it.argb == selected }
    val custom = isCustomKeyColor(selected)
    val selectedSlot = when {
        selected == KeyColorFromWallpaper -> 0
        custom -> KeyPresets.size + 1
        else -> presetIndex + 1
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedSlot - 2).coerceAtLeast(0),
    )
    val wallpaperColor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.compose.ui.res.colorResource(android.R.color.system_accent1_500)
    } else {
        colors.surfaceContainerHighest
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            KeySwatch(
                color = wallpaperColor,
                selected = selected == KeyColorFromWallpaper,
                label = stringResource(R.string.monet_key_color_wallpaper),
                icon = MiuixIcons.Background,
                enabled = enabled,
                onClick = { onSelect(KeyColorFromWallpaper) },
            )
        }
        items(KeyPresets.size) { i ->
            val preset = KeyPresets[i]
            KeySwatch(
                color = Color(preset.argb),
                selected = i == presetIndex,
                label = stringResource(preset.labelRes),
                enabled = enabled,
                onClick = { onSelect(preset.argb) },
            )
        }
        item {
            KeySwatch(
                color = if (custom) Color(selected) else colors.surfaceContainerHighest,
                selected = custom,
                label = stringResource(R.string.monet_key_color_custom),
                icon = MiuixIcons.Edit,
                enabled = enabled,
                onClick = onCustom,
            )
        }
    }
}

@Composable
private fun KeySwatch(
    color: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val palette = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, palette.primary, CircleShape) else Modifier)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label; role = Role.RadioButton }
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (color.luminance() > 0.5f) {
                        Color.Black.copy(alpha = 0.7f)
                    } else {
                        Color.White
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Miuix ColorPalette inside the app's themed dialog, from ZeroUI. */
@Composable
internal fun CustomMonetColorDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(Color(initial)) }
    AppDialog(
        title = stringResource(R.string.monet_custom_color_title),
        onDismissRequest = onDismiss,
        buttons = {
            AppTextButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
            Spacer(Modifier.size(8.dp))
            AppTextButton(
                text = stringResource(R.string.action_save),
                onClick = { onConfirm(draft.copy(alpha = 1f).toArgb()) },
                primary = true,
            )
        },
    ) {
        ColorPalette(
            color = draft,
            onColorChanged = { draft = it.copy(alpha = 1f) },
            includeGrayColumn = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "#%06X".format(draft.toArgb() and 0xFFFFFF),
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun initialCustomColor(value: Int): Int =
    if (isCustomKeyColor(value)) value else DefaultCustomKeyColor

