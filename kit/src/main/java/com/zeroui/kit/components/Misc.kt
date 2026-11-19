package com.zeroui.kit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeroui.kit.theme.ZeroTheme
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.basic.SmallTitleDefaults
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import androidx.compose.material3.Text as MaterialText
import top.yukonga.miuix.kmp.basic.SmallTitle as MiuixSmallTitle
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow

/** Group heading above a card. */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = SmallTitleDefaults.InsideMargin,
) {
    if (isMaterial()) {
        MaterialText(
            text = text,
            modifier = modifier
                .semantics { heading() }
                .padding(top = 8.dp)
                .padding(insideMargin),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    } else {
        MiuixSmallTitle(text = text, modifier = modifier.semantics { heading() }, insideMargin = insideMargin)
    }
}

/** Segmented tab selector. */
@Composable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isMaterial()) {
        MiuixTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
            colors = TabRowDefaults.tabRowColors(backgroundColor = Color.Transparent),
        )
        return
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedTabIndex,
                onClick = { onTabSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                label = { MaterialText(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** Small rounded label. */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = true,
    fontSize: TextUnit = 10.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    cornerRadius: Dp = 6.dp,
    horizontalPadding: Dp = 6.dp,
    verticalPadding: Dp = 2.dp,
) {
    val foreground = if (accent) ZeroTheme.colors.accent else ZeroTheme.colors.contentMuted
    Text(
        text = text,
        modifier = modifier
            .background(
                color = foreground.copy(alpha = if (accent) 0.15f else 0.10f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        color = foreground,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
    )
}

/** Accent message block; [danger] switches to the error color. */
@Composable
fun AccentBanner(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    textAlign: TextAlign? = null,
    danger: Boolean = false,
) {
    val tone = if (danger) ZeroTheme.colors.danger else ZeroTheme.colors.accent
    Box(
        modifier = modifier
            .background(
                color = tone.copy(alpha = 0.12f),
                shape = RoundedCornerShape(if (compact) 12.dp else 20.dp),
            )
            .padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 6.dp else 14.dp,
            ),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            color = tone,
        )
    }
}
