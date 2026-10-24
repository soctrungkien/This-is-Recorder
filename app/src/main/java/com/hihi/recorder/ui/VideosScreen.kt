package com.hihi.recorder.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hihi.recorder.R
import com.hihi.recorder.data.SavedRecording
import com.hihi.recorder.data.UiStyle
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture
import top.yukonga.miuix.kmp.icon.extended.Share

@Composable
fun VideosScreen(
    recordings: List<SavedRecording>,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
    onRename: (SavedRecording, String) -> Unit,
    onDelete: (SavedRecording) -> Unit,
) {
    var renameTarget by remember { mutableStateOf<SavedRecording?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedRecording?>(null) }
    val palette = appPalette()
    val bottomContentPadding = appBottomContentPadding()

    LaunchedEffect(Unit) { onRefresh() }

    AppScaffold(
        title = stringResource(R.string.videos_title),
        subtitle = stringResource(R.string.videos_count, recordings.size),
        snackbarHostState = snackbarHostState,
        actions = {
            AppIconButton(
                icon = MiuixIcons.Refresh,
                contentDescription = stringResource(R.string.action_refresh),
                onClick = onRefresh,
                enabled = !isLoading,
            )
        },
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        val listContent: @Composable (PaddingValues) -> Unit = { listPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = recordings,
                    key = { it.uri.toString() },
                ) { recording ->
                    VideoCard(
                        recording = recording,
                        onOpen = { onOpen(recording.uri) },
                        onShare = { onShare(recording.uri) },
                        onRename = { renameTarget = recording },
                        onDelete = { deleteTarget = recording },
                    )
                }
            }
        }

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding() + 24.dp,
                        bottom = bottomContentPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppCard(contentPadding = PaddingValues(24.dp)) {
                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppSpinner(size = 20.dp)
                            Spacer(Modifier.width(10.dp))
                            AppText(
                                text = stringResource(R.string.videos_loading),
                                style = AppTextStyle.SmallBody,
                                color = palette.onSurfaceVariant,
                            )
                        }
                    } else {
                        AppIcon(
                            icon = MiuixIcons.ScreenCapture,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(38.dp),
                            tint = palette.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        AppText(
                            text = stringResource(R.string.videos_empty_title),
                            modifier = Modifier.fillMaxWidth(),
                            style = AppTextStyle.Title,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        AppText(
                            text = stringResource(R.string.videos_empty_message),
                            modifier = Modifier.fillMaxWidth(),
                            style = AppTextStyle.SmallBody,
                            color = palette.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        AppButton(
                            text = stringResource(R.string.action_refresh),
                            onClick = onRefresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        } else if (LocalUiStyle.current == UiStyle.MIUIX) {
            PullToRefresh(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding),
                color = palette.primary,
                refreshTexts = listOf(
                    stringResource(R.string.pull_to_refresh),
                    stringResource(R.string.release_to_refresh),
                    stringResource(R.string.refreshing),
                    stringResource(R.string.refresh_complete),
                ),
                refreshTextStyle = MiuixTheme.textStyles.footnote2.copy(
                    color = palette.onSurfaceVariant,
                ),
            ) {
                listContent(
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = bottomContentPadding,
                    ),
                )
            }
        } else {
            listContent(
                PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = topPadding + 8.dp,
                    bottom = bottomContentPadding,
                ),
            )
        }
    }

    renameTarget?.let { recording ->
        AppInputDialog(
            title = stringResource(R.string.rename_title),
            message = stringResource(R.string.rename_message),
            initialValue = recording.displayName.substringBeforeLast('.'),
            placeholder = stringResource(R.string.rename_placeholder),
            confirmLabel = stringResource(R.string.action_rename),
            onConfirm = { newName ->
                onRename(recording, newName)
                renameTarget = null
            },
            onDismissRequest = { renameTarget = null },
            dismissLabel = stringResource(R.string.action_cancel),
        )
    }

    deleteTarget?.let { recording ->
        AppConfirmDialog(
            title = stringResource(R.string.delete_title),
            message = stringResource(R.string.delete_message, recording.displayName),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onDelete(recording)
                deleteTarget = null
            },
            onDismissRequest = { deleteTarget = null },
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
        )
    }
}

@Composable
private fun VideoCard(
    recording: SavedRecording,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = appPalette()
    AppCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingThumbnail(
                uri = recording.uri,
                modifier = Modifier
                    .width(116.dp)
                    .height(68.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = recording.displayName,
                    style = AppTextStyle.Subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                AppText(
                    text = stringResource(
                        R.string.last_meta,
                        formatDuration(recording.durationMs),
                        formatBytes(recording.sizeBytes),
                    ),
                    style = AppTextStyle.Footnote,
                    color = palette.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                AppText(
                    text = recording.relativePath.trimEnd('/')
                        .ifBlank { stringResource(R.string.videos_unknown_folder) },
                    style = AppTextStyle.Footnote,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        AppDivider()
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            VideoAction(
                icon = MiuixIcons.Play,
                label = stringResource(R.string.action_open),
                onClick = onOpen,
                modifier = Modifier.weight(1f),
            )
            VideoAction(
                icon = MiuixIcons.Share,
                label = stringResource(R.string.action_share),
                onClick = onShare,
                modifier = Modifier.weight(1f),
            )
            VideoAction(
                icon = MiuixIcons.Rename,
                label = stringResource(R.string.action_rename),
                onClick = onRename,
                modifier = Modifier.weight(1f),
            )
            VideoAction(
                icon = MiuixIcons.Delete,
                label = stringResource(R.string.action_delete),
                onClick = onDelete,
                tint = palette.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VideoAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = appPalette().onSurfaceVariant,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            icon = icon,
            contentDescription = label,
            modifier = Modifier.size(19.dp),
            tint = tint,
        )
        Spacer(Modifier.height(2.dp))
        AppText(
            text = label,
            style = AppTextStyle.Footnote,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
