package com.hihi.recorder.shell

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.root.RootShell
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

        if (!force) {
            val cached = successAt[backend] ?: 0L
            if (SystemClock.elapsedRealtime() - cached < SUCCESS_CACHE_MS) {
                return@withContext ShellStatus.Available(backend)
            }
        }

        val status = when (backend) {
            ShellBackend.ADB -> error("handled above")
            ShellBackend.SHIZUKU -> checkShizuku()
            ShellBackend.ROOT -> checkRoot()
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

    private suspend fun checkRoot(): ShellStatus {
        val id = RootShell.run("id", timeoutMs = 20_000L)
        if (id.timedOut) {
            return ShellStatus.Unavailable(ShellBackend.ROOT, ShellFailure.PROMPT_TIMEOUT)
        }
        if (!id.isSuccess || !id.output.contains("uid=0")) {
            return ShellStatus.Unavailable(ShellBackend.ROOT, ShellFailure.NO_SU, id.output)
        }
        val binary = RootShell.run(
            "test -x /system/bin/screenrecord || command -v screenrecord",
            timeoutMs = 10_000L,
        )
        // `test -x` succeeds without printing, so the exit status is the only
        // reliable signal here.
        if (!binary.isSuccess) {
            return ShellStatus.Unavailable(
                ShellBackend.ROOT,
                ShellFailure.NO_SCREENRECORD,
                binary.output,
            )
        }
        return ShellStatus.Available(ShellBackend.ROOT)
    }
}
