package com.zeroui.kit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeroui.kit.R
import com.zeroui.kit.theme.DialogPosition
import com.zeroui.kit.theme.LocalDialogPosition
import com.zeroui.kit.theme.ProvideContentColor
import com.zeroui.kit.theme.isMaterial
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.material3.Text as MaterialText
import top.yukonga.miuix.kmp.window.WindowBottomSheet as MiuixWindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog

/** Dialog controlled by [show]; [onDismissFinished] runs after the exit animation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!isMaterial()) {
        MiuixWindowDialog(
            show = show,
            modifier = modifier,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            largeScreen = if (LocalDialogPosition.current == DialogPosition.Center) true else null,
            content = {
                HighRefreshRateEffect()
                content()
            },
        )
        return
    }
    // BasicAlertDialog has no exit animation to wait for, so "finished" is reported
    // as soon as show drops. wasShown keeps it from firing on the first composition.
    var wasShown by remember { mutableStateOf(false) }
    val finished by rememberUpdatedState(onDismissFinished)
    LaunchedEffect(show) {
        if (show) {
            wasShown = true
        } else if (wasShown) {
            wasShown = false
            finished?.invoke()
        }
    }
    if (!show) return
    BasicAlertDialog(onDismissRequest = { onDismissRequest?.invoke() }, modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            ProvideContentColor(MaterialTheme.colorScheme.onSurface) {
                HighRefreshRateEffect()
                Column(modifier = Modifier.padding(24.dp)) {
                    if (!title.isNullOrEmpty()) {
                        MaterialText(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    if (!summary.isNullOrEmpty()) {
                        MaterialText(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    content()
                }
            }
        }
    }
}

/** Bottom sheet controlled by [show]; [onDismissFinished] runs after the exit animation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    allowDismiss: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!isMaterial()) {
        MiuixWindowBottomSheet(
            show = show,
            modifier = modifier,
            title = title,
            startAction = startAction,
            endAction = endAction,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            allowDismiss = allowDismiss,
        ) {
            HighRefreshRateEffect()
            content()
        }
        return
    }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var visible by remember { mutableStateOf(false) }
    val finished by rememberUpdatedState(onDismissFinished)
    LaunchedEffect(show) {
        if (show) {
            visible = true
        } else if (visible) {
            // Leave the composition only after the slide down has played; dropping
            // the sheet at once would cut the animation.
            sheetState.hide()
            visible = false
            finished?.invoke()
        }
    }
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = { if (allowDismiss) onDismissRequest?.invoke() },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ProvideContentColor(MaterialTheme.colorScheme.onSurface) {
            HighRefreshRateEffect()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            ) {
                if (title != null || startAction != null || endAction != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { startAction?.invoke() }
                        if (title != null) {
                            MaterialText(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { endAction?.invoke() }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                content()
            }
        }
    }
}

/** Closes a managed window and runs an action after its exit animation. */
@Stable
class WindowCloser internal constructor(private val request: (() -> Unit) -> Unit) {
    /** Starts the exit animation and runs [then] when it ends. */
    fun close(then: () -> Unit = {}) = request(then)
}

/**
 * Visibility holder of a managed window.
 * An `if (visible) Dialog()` call site removes the dialog the instant its flag flips,
 * which skips the exit animation. This holds its own flag, plays the exit first and
 * only then tells the caller.
 */
@Stable
private class ManagedPresence(private val onDismiss: () -> Unit) {
    var show by mutableStateOf(true)
        private set
    private var pending: (() -> Unit)? = null

    val closer = WindowCloser { then ->
        // A second tap during the exit animation must not replace the pending action.
        if (show) {
            pending = then
            show = false
        }
    }

    fun dismiss() = closer.close(onDismiss)

    fun finished() {
        val action = pending ?: onDismiss
        pending = null
        action()
    }
}

@Composable
private fun rememberManagedPresence(onDismiss: () -> Unit): ManagedPresence {
    val current by rememberUpdatedState(onDismiss)
    return remember { ManagedPresence { current() } }
}

/** Dialog for `if (visible)` call sites; it owns its enter and exit animation. */
@Composable
fun ManagedDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    summary: String? = null,
    dismissible: Boolean = true,
    content: @Composable WindowCloser.() -> Unit,
) {
    val presence = rememberManagedPresence(onDismiss)
    WindowDialog(
        show = presence.show,
        title = title,
        summary = summary,
        onDismissRequest = if (dismissible) ({ presence.dismiss() }) else null,
        onDismissFinished = { presence.finished() },
    ) {
        presence.closer.content()
    }
}

/** Bottom sheet for `if (visible)` call sites; it owns its enter and exit animation. */
@Composable
fun ManagedBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    dismissible: Boolean = true,
    content: @Composable WindowCloser.() -> Unit,
) {
    val presence = rememberManagedPresence(onDismiss)
    WindowBottomSheet(
        show = presence.show,
        title = title,
        onDismissRequest = { presence.dismiss() },
        onDismissFinished = { presence.finished() },
        allowDismiss = dismissible,
    ) {
        presence.closer.content()
    }
}

/** Equal-width cancel and confirm buttons. */
@Composable
fun DialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    confirmEnabled: Boolean = true,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(text = cancelText, onClick = onCancel, modifier = Modifier.weight(1f))
        TextButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            variant = ButtonVariant.Primary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Confirmation dialog for `if (visible)` call sites. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.ok),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ManagedDialog(onDismiss = onDismiss, title = title, summary = message) {
        DialogButtons(
            confirmText = confirmText,
            onConfirm = { close(onConfirm) },
            onCancel = { close(onDismiss) },
        )
    }
}

/** Text of a confirmation request. */
data class ConfirmRequest(
    val title: String,
    val message: String? = null,
    val confirmText: String,
)

/** Suspending confirmation requests, rendered by [ConfirmDialogHost]. */
@Stable
class ConfirmDialogState {
    internal var active by mutableStateOf<ActiveConfirm?>(null)
        private set

    /** Shows [request] and returns true when the user confirms. */
    suspend fun confirm(request: ConfirmRequest): Boolean {
        val answer = CompletableDeferred<Boolean>()
        val entry = ActiveConfirm(request, answer)
        // Only one dialog fits on screen. A newer request answers the older one with
        // "no" so its caller is not left suspended forever.
        active?.answer?.complete(false)
        active = entry
        try {
            return answer.await()
        } finally {
            // Also reached on cancellation, e.g. the caller's scope left the screen.
            if (active === entry) active = null
        }
    }
}

internal class ActiveConfirm(
    val request: ConfirmRequest,
    val answer: CompletableDeferred<Boolean>,
)

/** Renders the active request of [state]. */
@Composable
fun ConfirmDialogHost(state: ConfirmDialogState) {
    val entry = state.active ?: return
    ManagedDialog(
        onDismiss = { entry.answer.complete(false) },
        title = entry.request.title,
        summary = entry.request.message,
    ) {
        DialogButtons(
            confirmText = entry.request.confirmText,
            onConfirm = { close { entry.answer.complete(true) } },
            onCancel = { close { entry.answer.complete(false) } },
        )
    }
}
