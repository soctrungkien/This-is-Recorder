package com.hihi.recorder.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.rememberMarkdownState

/**
 * Renders GitHub release notes as markdown using multiplatform-markdown-renderer.
 *
 * Links are routed through [onOpenUrl] so they use the app's normal browser
 * handling instead of the default [UriHandler].
 */
@Composable
fun MarkdownText(
    text: String,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    val uriHandler = remember(onOpenUrl) {
        object : UriHandler {
            override fun openUri(uri: String) = onOpenUrl(uri)
        }
    }

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        Markdown(
            rememberMarkdownState(text),
            modifier = modifier.fillMaxWidth(),
            colors = markdownColor(text = palette.onSurfaceVariant),
        )
    }
}
