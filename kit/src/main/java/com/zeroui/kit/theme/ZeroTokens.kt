package com.zeroui.kit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Semantic colors shared by both UI styles. */
@Immutable
data class ZeroColors(
    val page: Color,
    val card: Color,
    val content: Color,
    val contentMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val outline: Color,
    val danger: Color,
    val onDanger: Color,
    val success: Color,
)

/** Semantic text styles shared by both UI styles. */
@Immutable
data class ZeroTypography(
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

internal val LocalZeroColors = staticCompositionLocalOf<ZeroColors> {
    error("ZeroTheme is not applied")
}

internal val LocalZeroTypography = staticCompositionLocalOf<ZeroTypography> {
    error("ZeroTheme is not applied")
}

private val SuccessLight = Color(0xFF2FB457)
private val SuccessDark = Color(0xFF5BD17F)

/** Maps the active style's color scheme to [ZeroColors]. */
@Composable
internal fun resolveColors(style: UiStyle, dark: Boolean): ZeroColors {
    val success = if (dark) SuccessDark else SuccessLight
    if (style == UiStyle.Material) {
        val scheme = MaterialTheme.colorScheme
        return ZeroColors(
            page = scheme.surfaceContainer,
            card = scheme.surfaceBright,
            content = scheme.onSurface,
            contentMuted = scheme.onSurfaceVariant,
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            accentContainer = scheme.primaryContainer,
            onAccentContainer = scheme.onPrimaryContainer,
            outline = scheme.outlineVariant,
            danger = scheme.error,
            onDanger = scheme.onError,
            success = success,
        )
    }
    val scheme = MiuixTheme.colorScheme
    return ZeroColors(
        page = scheme.surface,
        card = scheme.surfaceContainer,
        content = scheme.onSurface,
        contentMuted = scheme.onSurfaceVariantSummary,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentContainer = scheme.primaryContainer,
        onAccentContainer = scheme.onPrimaryContainer,
        outline = scheme.dividerLine,
        danger = scheme.error,
        onDanger = scheme.onError,
        success = success,
    )
}

/** Maps the active style's text styles to [ZeroTypography]. */
@Composable
internal fun resolveTypography(style: UiStyle): ZeroTypography {
    if (style == UiStyle.Material) {
        val type = MaterialTheme.typography
        return ZeroTypography(
            headline = type.headlineMedium,
            title = type.titleMedium,
            body = type.bodyLarge,
            label = type.labelLarge,
            caption = type.bodySmall,
        )
    }
    val type = MiuixTheme.textStyles
    return ZeroTypography(
        headline = type.title2,
        title = type.title4,
        body = type.main,
        label = type.body2,
        caption = type.footnote1,
    )
}
