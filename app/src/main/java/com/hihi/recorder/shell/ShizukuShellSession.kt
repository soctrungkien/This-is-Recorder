package com.hihi.recorder.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.SystemClock
import com.hihi.recorder.BuildConfig
import com.hihi.recorder.data.ShellBackend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.File

/**
 * Shell identity provided by Shizuku's UserService API.
 *
 * Shizuku 13 no longer exposes a public process-spawning helper, so the app
 * binds a small AIDL service. Shizuku starts it with uid 2000 (shell), which
 * is exactly the identity `screenrecord` and `appops` expect.
 */
internal class ShizukuShellSession(
    private val context: Context,
) : ShellSession {

    override val backend: ShellBackend = ShellBackend.SHIZUKU

    private var service: IShellUserService? = null
    private var args: Shizuku.UserServiceArgs? = null
    private var connection: ServiceConnection? = null

    override suspend fun open() = withContext(Dispatchers.IO) {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            throw ShellException(ShellFailure.SHIZUKU_NOT_RUNNING)
        }
        val permission = runCatching { Shizuku.checkSelfPermission() }
            .getOrDefault(PackageManager.PERMISSION_DENIED)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            throw ShellException(ShellFailure.SHIZUKU_NO_PERMISSION)
        }

        val deferred = CompletableDeferred<IShellUserService>()
        val userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuShellService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("shell")
            .tag("ThisIsRecorderShell")
            .debuggable(BuildConfig.DEBUG)
            .version(SERVICE_VERSION)

        val binderConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder == null) {
                    deferred.completeExceptionally(
                        IllegalStateException("Shizuku returned an empty binder"),
                    )
                } else {
                    deferred.complete(IShellUserService.Stub.asInterface(binder))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                deferred.completeExceptionally(
                    IllegalStateException("Shizuku user service disconnected"),
                )
            }

            override fun onBindingDied(name: ComponentName?) {
                deferred.completeExceptionally(
                    IllegalStateException("Shizuku user service binding died"),
                )
            }
        }
        args = userServiceArgs
        connection = binderConnection

        runCatching {
            withContext(Dispatchers.Main.immediate) {
                Shizuku.bindUserService(userServiceArgs, binderConnection)
            }
        }.onFailure {
            close()
            throw ShellException(ShellFailure.BACKEND_ERROR, it.message)
        }

        val bound = withTimeoutOrNull(20_000L) { deferred.await() }
            ?: run {
                close()
                throw ShellException(ShellFailure.BACKEND_ERROR, "Shizuku bind timed out")
            }
        service = bound

        if (!exec("echo ready", timeoutMs = 5_000L).let { result ->
                result.isSuccess && result.output.contains("ready")
            }
        ) {
            close()
            throw ShellException(ShellFailure.BACKEND_ERROR, "Shizuku user service did not answer")
        }

        val binary = exec(
            "test -x /system/bin/screenrecord || command -v screenrecord",
            timeoutMs = 5_000L,
        )
        // `test -x` succeeds without printing, so the exit status is the only
        // reliable signal here.
        if (!binary.isSuccess) {
            close()
            throw ShellException(ShellFailure.NO_SCREENRECORD, binary.output)
        }
    }

    override suspend fun exec(command: String, timeoutMs: Long): ShellCommandResult {
        val active = service ?: throw ShellException(ShellFailure.BACKEND_ERROR, "Not bound")
        val raw = runCatching {
            active.runCommand(command, timeoutMs.coerceIn(1_000L, 120_000L).toInt())
        }.getOrElse {
            throw ShellException(ShellFailure.BACKEND_ERROR, it.message)
        }
        val newline = raw.indexOf('\n')
        if (newline < 0) return ShellCommandResult(-1, raw)
        val exitCode = raw.substring(0, newline).trim().toIntOrNull() ?: -1
        val output = raw.substring(newline + 1).trimEnd('\n')
        return ShellCommandResult(
            exitCode = exitCode,
            output = AdbDaemon.trimOutput(output),
            timedOut = exitCode == -1 && output.contains("timed out", ignoreCase = true),
        )
    }

    override suspend fun startVideo(
        file: File,
        size: String,
        bitRate: Int,
        timeLimitSeconds: Int,
    ): Long = withContext(Dispatchers.IO) {
        val active = service ?: throw ShellException(ShellFailure.BACKEND_ERROR, "Not bound")
        file.parentFile?.mkdirs()
        file.delete()
        val started = runCatching {
            active.startSegment(file.absolutePath, size, bitRate, timeLimitSeconds)
        }.getOrElse {
            throw ShellException(ShellFailure.BACKEND_ERROR, it.message)
        }
        if (started <= 0L) {
            val detail = runCatching { active.segmentError() }.getOrDefault("")
            throw ShellException(ShellFailure.BACKEND_ERROR, detail)
        }
        started
    }

    override suspend fun stopVideo() {
        withContext(Dispatchers.IO) {
            val active = service ?: return@withContext
            runCatching { active.stopSegment() }
        }
    }

    override suspend fun awaitVideoEnd(): ShellVideoEnd = withContext(Dispatchers.IO) {
        val deadline = SystemClock.elapsedRealtime() + 240_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val active = service ?: throw ShellException(ShellFailure.BACKEND_ERROR, "Unbound")
            val running = runCatching { active.segmentRunning() }.getOrElse {
                throw ShellException(ShellFailure.BACKEND_ERROR, it.message)
            }
            if (!running) {
                val endedAt = runCatching { active.segmentEndedAt() }.getOrDefault(0L)
                if (endedAt > 0L) {
                    return@withContext ShellVideoEnd(
                        endedAtElapsedMs = endedAt,
                        exitCode = runCatching { active.segmentExitCode() }.getOrDefault(-1),
                        error = runCatching { active.segmentError() }
                            .getOrDefault("")
                            .take(8_000),
                    )
                }
            }
            delay(150L)
        }
        throw ShellException(ShellFailure.BACKEND_ERROR, "screenrecord did not stop")
    }

    override fun close() {
        val active = service
        service = null
        runCatching { active?.stopSegment() }
        val boundArgs = args
        val boundConnection = connection
        args = null
        connection = null
        if (boundArgs != null && boundConnection != null) {
            runCatching { Shizuku.unbindUserService(boundArgs, boundConnection, true) }
        }
    }

    private companion object {
        const val SERVICE_VERSION = 1
    }
}
