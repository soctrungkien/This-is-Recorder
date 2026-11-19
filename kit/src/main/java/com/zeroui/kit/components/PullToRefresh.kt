package com.zeroui.kit.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.PullToRefreshDefaults
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.basic.PullToRefresh as MiuixPullToRefresh

/** State of a [PullToRefresh] container. */
@Stable
class PullRefreshState internal constructor(internal val state: PullToRefreshState)

/** Creates and remembers a [PullRefreshState]. */
@Composable
fun rememberPullRefreshState(): PullRefreshState {
    val state = rememberPullToRefreshState()
    return remember(state) { PullRefreshState(state) }
}

/**
 * Pull to refresh container.
 *
 * @param topBarScroll Scroll behavior of the page's top bar, so the indicator follows its collapse.
 * @param refreshTexts Indicator texts in the order idle, release, refreshing, done; the defaults when null.
 */
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullRefreshState = rememberPullRefreshState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    topBarScroll: TopBarScroll? = null,
    refreshTexts: List<String>? = null,
    content: @Composable () -> Unit,
) {
    MiuixPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        pullToRefreshState = state.state,
        contentPadding = contentPadding,
        topAppBarScrollBehavior = topBarScroll?.behavior,
        refreshTexts = refreshTexts ?: PullToRefreshDefaults.refreshTexts,
        content = content,
    )
}
