package com.hihi.recorder.shell

import android.content.Context
import androidx.core.content.edit
import com.hihi.recorder.data.ShellBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/**
 * Controls the `android:project_media` app-op while shell recording captures
 * phone audio.
 *
 * The shell identity grants the op before the MediaProjection consent screen
 * and restores the previous mode when the recording stops. The previous mode is
 * persisted first, so a crash or a system-killed service can still clean up on
 * the next launch.
 */
class ProjectMediaAppOp(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Enables the app-op. Returns false when the shell backend could not be
     * reached; the normal projection prompt is still allowed to run.
     */
    suspend fun grant(backend: ShellBackend): Boolean = mutex.withLock {
        val packageName = appContext.packageName
        if (preferences.getBoolean(KEY_PENDING, false)) {
            // A restore from an earlier attempt is still pending. Keep its
            // saved previous mode, but use the backend that works now.
            if (preferences.getString(KEY_BACKEND, null) != backend.name) {
                preferences.edit(commit = true) { putString(KEY_BACKEND, backend.name) }
            }
            return@withLock setMode(backend, packageName, MODE_ALLOW)
        }

        // An unreadable mode must not fall back to `default`: restoring that
        // later could silently undo a pre-existing deny/ignore/foreground mode.
        val previous = readMode(backend, packageName) ?: return@withLock false
        if (previous == MODE_ALLOW) {
            // The op was already allowed by the user or another tool. Do not
            // schedule a restore that would change it.
            return@withLock true
        }

        // Synchronous by design: the pending restore must survive a crash that
        // happens after the app-op changes below.
        preferences.edit(commit = true) {
            putBoolean(KEY_PENDING, true)
            putString(KEY_PREVIOUS, previous)
            putString(KEY_BACKEND, backend.name)
        }

        val allowed = setMode(backend, packageName, MODE_ALLOW)
        if (!allowed) {
            clearPending()
        }
        allowed
    }

    /** Restores the mode saved by [grant]. Returns false if it must be retried. */
    suspend fun restore(): Boolean = mutex.withLock {
        if (!preferences.getBoolean(KEY_PENDING, false)) return@withLock true
        val backend = preferences.getString(KEY_BACKEND, null)
            ?.let { name -> runCatching { ShellBackend.valueOf(name) }.getOrNull() }
            ?: ShellBackend.ROOT
        val previous = preferences.getString(KEY_PREVIOUS, MODE_DEFAULT) ?: MODE_DEFAULT
        val restored = setMode(backend, appContext.packageName, previous)
        if (restored) clearPending()
        restored
    }

    private suspend fun readMode(backend: ShellBackend, packageName: String): String? {
        val result = try {
            ShellSessions.withSession(appContext, backend) { session ->
                session.exec(
                    "appops get ${shellQuote(packageName)} android:project_media",
                    timeoutMs = 15_000L,
                )
            }
        } catch (cancelled: CancellationException) {
            // Never leave the op allowed without its pending restore record.
            throw cancelled
        } catch (_: Throwable) {
            return null
        }

        if (!result.isSuccess) return null
        val output = result.output
        if (output.isBlank() || output.contains("No operations", ignoreCase = true)) {
            return MODE_DEFAULT
        }
        val match = MODE_PATTERN.find(output) ?: return MODE_DEFAULT
        return normalizeMode(match.groupValues[1])
    }

    private suspend fun setMode(
        backend: ShellBackend,
        packageName: String,
        mode: String,
    ): Boolean = try {
        ShellSessions.withSession(appContext, backend) { session ->
            val result = session.exec(
                "appops set ${shellQuote(packageName)} android:project_media " +
                    shellQuote(mode),
                timeoutMs = 15_000L,
            )
            result.isSuccess &&
                !result.output.contains("error", ignoreCase = true) &&
                !result.output.contains("unknown", ignoreCase = true)
        }
    } catch (cancelled: CancellationException) {
        // If the caller is cancelled after `appops set` ran, leave the pending
        // record intact so the next launch restores the previous mode.
        throw cancelled
    } catch (_: Throwable) {
        false
    }

    private fun normalizeMode(raw: String): String = when (raw.lowercase(Locale.US)) {
        MODE_ALLOW, MODE_DENY, MODE_IGNORE, MODE_FOREGROUND -> raw.lowercase(Locale.US)
        else -> MODE_DEFAULT
    }

    private fun clearPending() {
        // Synchronous for the same reason as grant(): the recorded previous
        // mode must not be lost if the process dies right after restoration.
        preferences.edit(commit = true) {
            remove(KEY_PENDING)
            remove(KEY_PREVIOUS)
            remove(KEY_BACKEND)
        }
    }

    companion object {
        private const val PREFS_NAME = "miuix_recorder_shell"
        private const val KEY_PENDING = "project_media_pending"
        private const val KEY_PREVIOUS = "project_media_previous"
        private const val KEY_BACKEND = "project_media_backend"

        private const val MODE_ALLOW = "allow"
        private const val MODE_DENY = "deny"
        private const val MODE_IGNORE = "ignore"
        private const val MODE_DEFAULT = "default"
        private const val MODE_FOREGROUND = "foreground"

        private val MODE_PATTERN = Regex(
            // AOSP prints the public op name (PROJECT_MEDIA); some OEMs print
            // the android:project_media spelling accepted by `appops`.
            """(?:android:)?project_media:\s*([A-Za-z]+)""",
            RegexOption.IGNORE_CASE,
        )

        private val mutex = Mutex()
        private val restoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var restoreJob: Job? = null

        fun hasPending(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PENDING, false)

        /**
         * Waits for a fire-and-forget restore to finish. Shizuku user services
         * are shared per tag, so the next session must not bind before the
         * previous restore has unbound (and destroyed) its service.
         */
        suspend fun awaitPendingWork() {
            restoreJob?.join()
        }

        /**
         * Fire-and-forget cleanup for service teardown. The service may call
         * stopSelf immediately afterwards; this scope intentionally outlives
         * it, but the process and the app-op operation are small.
         */
        fun restoreDetached(context: Context) {
            val appContext = context.applicationContext
            if (!hasPending(appContext)) return
            restoreJob = restoreScope.launch {
                runCatching { ProjectMediaAppOp(appContext).restore() }
            }
        }
    }
}
