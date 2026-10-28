package com.hihi.recorder.shell

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import com.hihi.recorder.data.ShellBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.ConcurrentHashMap

/** Probes the selected activation method without starting a recording. */
object ShellChecker {

    private const val SUCCESS_CACHE_MS = 20_000L
    private val successAt = ConcurrentHashMap<ShellBackend, Long>()

    fun invalidate(backend: ShellBackend? = null) {
        if (backend == null) {
            successAt.clear()
        } else {
            successAt.remove(backend)
        }
    }

    suspend fun check(
        context: Context,
        backend: ShellBackend,
        force: Boolean = false,
    ): ShellStatus = withContext(Dispatchers.IO) {
        if (backend == ShellBackend.ADB) {
            // The daemon writes a heartbeat once per second, so there is
            // nothing to cache and the status can recover by itself as soon as
            // the user runs the activation command.
            return@withContext if (AdbDaemon.isRunning(context)) {
                ShellStatus.Available(backend)
            } else {
                ShellStatus.Unavailable(backend, ShellFailure.ADB_NOT_RUNNING)
            }
        }

        if (backend == ShellBackend.ROOT) {
            // A running root daemon needs no `su` prompt. When it is missing,
            // checkRoot() starts it through the same one-time activation path
            // used by recording, so later checks only read the heartbeat.
            return@withContext if (RootDaemon.isRunning(context)) {
                ShellStatus.Available(backend)
            } else {
                checkRoot(context)
            }
        }

        if (!force) {
            val cached = successAt[backend] ?: 0L
            if (SystemClock.elapsedRealtime() - cached < SUCCESS_CACHE_MS) {
                return@withContext ShellStatus.Available(backend)
            }
        }

        val status = when (backend) {
            ShellBackend.ADB, ShellBackend.ROOT -> error("handled above")
            ShellBackend.SHIZUKU -> checkShizuku()
        }
        if (status is ShellStatus.Available) {
            successAt[backend] = SystemClock.elapsedRealtime()
        } else {
            successAt.remove(backend)
        }
        status
    }

    private suspend fun checkShizuku(): ShellStatus {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            return ShellStatus.Unavailable(
                ShellBackend.SHIZUKU,
                ShellFailure.SHIZUKU_NOT_RUNNING,
            )
        }
        val permission = runCatching { Shizuku.checkSelfPermission() }
            .getOrDefault(PackageManager.PERMISSION_DENIED)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            return ShellStatus.Unavailable(
                ShellBackend.SHIZUKU,
                ShellFailure.SHIZUKU_NO_PERMISSION,
            )
        }
        return ShellStatus.Available(ShellBackend.SHIZUKU)
    }

    private suspend fun checkRoot(context: Context): ShellStatus = try {
        // Opening a root session starts the daemon through su when needed and
        // verifies screenrecord through the same file protocol used later.
        ShellSessions.withSession(context, ShellBackend.ROOT) { }
        ShellStatus.Available(ShellBackend.ROOT)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: ShellException) {
        ShellStatus.Unavailable(ShellBackend.ROOT, error.failure, error.detail)
    } catch (error: Throwable) {
        ShellStatus.Unavailable(ShellBackend.ROOT, ShellFailure.BACKEND_ERROR, error.message)
    }
}
