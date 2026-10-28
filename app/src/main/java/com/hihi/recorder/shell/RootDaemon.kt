package com.hihi.recorder.shell

import android.content.Context
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.root.RootShell
import kotlinx.coroutines.CancellationException

/**
 * Root activation starts the same file daemon as ADB, but does it through `su`
 * once instead of asking the user to run an adb command.
 *
 * The daemon keeps running as root after the app process dies, so later
 * commands and recording segments do not prompt for root again. It exits after
 * a reboot (the app starts it again) or when its control directory disappears.
 */
object RootDaemon {

    private const val FOLDER = "shell-root"
    private const val START_TIMEOUT_MS = 30_000L

    internal fun daemon(context: Context): ShellFileDaemon =
        ShellFileDaemon(
            context = context,
            backend = ShellBackend.ROOT,
            folderName = FOLDER,
            ownerOnly = true,
        )

    fun isRunning(context: Context): Boolean = daemon(context).isRunning()

    /**
     * Starts the daemon if needed. Throws a [ShellException] when `su` is
     * denied, times out, or the daemon never reports a heartbeat.
     */
    suspend fun ensureRunning(context: Context) {
        val daemon = daemon(context)
        daemon.ensureScripts()
        if (daemon.isRunning()) return

        val result = try {
            RootShell.run(
                "sh ${RootShell.quote(daemon.startScript().absolutePath)}",
                timeoutMs = START_TIMEOUT_MS,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            throw ShellException(ShellFailure.BACKEND_ERROR, error.message)
        }

        when {
            result.timedOut -> throw ShellException(ShellFailure.PROMPT_TIMEOUT)
            !result.isSuccess -> throw ShellException(
                ShellFailure.NO_SU,
                result.output.ifBlank { null },
            )
        }

        if (!daemon.awaitRunning()) {
            throw ShellException(
                ShellFailure.BACKEND_ERROR,
                result.output.ifBlank { "root daemon did not start" },
            )
        }
    }

    /** Stops the background daemon and any screenrecord it owns. */
    fun shutdown(context: Context) {
        val daemon = daemon(context)
        if (daemon.isRunning()) daemon.requestStop()
    }
}
