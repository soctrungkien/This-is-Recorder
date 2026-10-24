package com.hihi.recorder.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture

@Composable
fun RecordingThumbnail(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val palette = appPalette()
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { loadThumbnail(context, uri) }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val image = thumbnail
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            AppIcon(
                icon = MiuixIcons.ScreenCapture,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = palette.onSurfaceVariant,
            )
        }
    }
}

private fun loadThumbnail(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val bitmap = retriever.getScaledFrameAtTime(
            0L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            320,
            180,
        )
        bitmap?.asImageBitmap()
    } finally {
        runCatching { retriever.release() }
    }
}.getOrNull()
