package com.zeroui.kit.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.zeroui.kit.glass.LocalPageBackground
import com.zeroui.kit.glass.PageGlassHost
import com.zeroui.kit.glass.drawPageBackground
import com.zeroui.kit.theme.CardMaterial
import com.zeroui.kit.theme.LocalTopBarStyle
import com.zeroui.kit.theme.TopBarStyle
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults as MiuixTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior as MiuixScrollBehaviorFactory
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarState as MiuixTopAppBarState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState

/** Page scaffold that hosts popups and draws the current page background. */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingToolbar: @Composable () -> Unit = {},
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    val color = when {
        containerColor != Color.Unspecified -> containerColor
        isMaterial() -> MaterialTheme.colorScheme.surfaceContainer
        else -> MiuixTheme.colorScheme.surface
    }
    val page = LocalPageBackground.current?.takeIf { containerColor == Color.Unspecified }
    val glassPage = page?.takeIf { it.cards != CardMaterial.Solid }
    val wrappedContent: @Composable (PaddingValues) -> Unit = if (glassPage == null) {
        content
    } else {
        { padding -> PageGlassHost(glassPage, color) { content(padding) } }
    }
    MiuixScaffold(
        modifier = if (page != null) {
            modifier.drawBehind {
                drawRect(color)
                drawPageBackground(page, color)
            }
        } else {
            modifier
        },
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        floatingToolbar = floatingToolbar,
        floatingToolbarPosition = floatingToolbarPosition,
        snackbarHost = snackbarHost,
        containerColor = if (page != null) Color.Transparent else color,
        contentWindowInsets = contentWindowInsets,
        content = wrappedContent,
    )
}

/** Miuix-typed scroll behavior backed by a Material one. */
@OptIn(ExperimentalMaterial3Api::class)
private class MaterialScrollBehavior(
    val material: TopAppBarScrollBehavior,
    override val state: MiuixTopAppBarState,
) : ScrollBehavior {
    override val isPinned: Boolean get() = false
    override val snapAnimationSpec: AnimationSpec<Float>? get() = null
    override val flingAnimationSpec: DecayAnimationSpec<Float>? get() = null
    override val nestedScrollConnection: NestedScrollConnection get() = material.nestedScrollConnection
}

/** Scroll behavior of a [TopAppBar]; connect [nestedScrollConnection] to the scrolling content. */
@Stable
class TopBarScroll internal constructor(
    internal val behavior: ScrollBehavior,
    internal val material: TopAppBarScrollBehavior? = null,
) {
    val nestedScrollConnection: NestedScrollConnection get() = behavior.nestedScrollConnection
}

/** Scroll behavior that collapses [TopAppBar]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberTopBarScrollBehavior(): TopBarScroll {
    if (!isMaterial()) {
        val miuix = MiuixScrollBehaviorFactory()
        return remember(miuix) { TopBarScroll(miuix) }
    }
    val material = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val placeholder = rememberMiuixTopAppBarState()
    return remember(material, placeholder) { TopBarScroll(MaterialScrollBehavior(material, placeholder), material) }
}

/**
 * Top app bar; uses a single-line layout for the compact style or an empty title.
 * [color] overrides the container color; unspecified uses the surface of the active style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    largeTitle: String = title,
    subtitle: String = "",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopBarScroll? = null,
    titlePadding: Dp = MiuixTopAppBarDefaults.TitlePadding,
    bottomContent: @Composable () -> Unit = {},
) {
    val containerColor = color.takeIf { it != Color.Unspecified }
    val compact = LocalTopBarStyle.current == TopBarStyle.Compact || (title.isEmpty() && largeTitle.isEmpty())
    if (!isMaterial()) {
        if (compact) {
            MiuixSmallTopAppBar(
                title = title,
                modifier = modifier,
                color = containerColor ?: MiuixTheme.colorScheme.surface,
                subtitle = subtitle,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior?.behavior,
                titlePadding = titlePadding,
                bottomContent = bottomContent,
            )
        } else {
            MiuixTopAppBar(
                title = title,
                modifier = modifier,
                color = containerColor ?: MiuixTheme.colorScheme.surface,
                largeTitle = largeTitle,
                subtitle = subtitle,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior?.behavior,
                titlePadding = titlePadding,
                bottomContent = bottomContent,
            )
        }
        return
    }
    val container = containerColor ?: MaterialTheme.colorScheme.surfaceContainer
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = container,
        scrolledContainerColor = container,
    )
    val material = scrollBehavior?.material
    if (compact) {
        MaterialTopAppBar(
            title = { MaterialText(text = title.ifEmpty { largeTitle }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = material,
        )
    } else {
        LargeTopAppBar(
            title = { MaterialText(text = largeTitle.ifEmpty { title }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = material,
        )
    }
}
