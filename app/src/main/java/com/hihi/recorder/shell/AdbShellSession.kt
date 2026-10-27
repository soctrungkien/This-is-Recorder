package com.hihi.recorder.shell

import android.content.Context
import android.os.SystemClock
import com.hihi.recorder.data.ShellBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Talks to [AdbDaemon] through files. The app never forks `adb`; the user
 * starts the daemon once from a computer.
 */
internal class AdbShellSession(
    private val context: Context,
) : ShellSession {

    override val backend: ShellBackend = ShellBackend.ADB

    private val requestId = AtomicInteger(0)
    private val segmentCounter = AtomicInteger(0)

    @Volatile
    private var videoActive = false

    @Volatile
    private var activeToken: String? = null

    override suspend fun open() = withContext(Dispatchers.IO) {
        AdbDaemon.ensureScripts(context)
        if (!AdbDaemon.isRunning(context)) {
            throw ShellException(ShellFailure.ADB_NOT_RUNNING)
        }
        val ping = exec("echo ready", timeoutMs = 5_000L)
        if (!ping.isSuccess || !ping.output.contains("ready")) {
            throw ShellException(ShellFailure.ADB_NOT_RUNNING, ping.output)
        }
        val binary = exec(
            "test -x /system/bin/screenrecord || command -v screenrecord",
            timeoutMs = 5_000L,
        )
        // `test -x` succeeds without printing, so the exit status is the only
        // reliable signal here.
        if (!binary.isSuccess) {
            throw ShellException(ShellFailure.NO_SCREENRECORD, binary.output)
        }
    }

    override suspend fun exec(command: String, timeoutMs: Long): ShellCommandResult =
        withContext(Dispatchers.IO) {
            commandMutex.withLock {
                val id = requestId.incrementAndGet().toString()
                AdbDaemon.writeAtomically(
                    AdbDaemon.commandFile(context),
                    "$id\n$command\n",
                )

                val deadline = SystemClock.elapsedRealtime() + timeoutMs
                while (SystemClock.elapsedRealtime() < deadline) {
                    if (!AdbDaemon.isRunning(context)) {
                        throw ShellException(ShellFailure.ADB_NOT_RUNNING)
                    }
                    val result = readResult(id)
                    if (result != null) return@withLock result
                    delay(80L)
                }
                ShellCommandResult(exitCode = -1, output = "", timedOut = true)
            }
        }

    override suspend fun startVideo(
        file: File,
        size: String,
        bitRate: Int,
        timeLimitSeconds: Int,
    ): Long = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.delete()

        val token = "${System.currentTimeMillis()}_${segmentCounter.incrementAndGet()}"
        val startMarker = AdbDaemon.videoStartMarker(context, token)
        val endMarker = AdbDaemon.videoEndMarker(context, token)
        val errorFile = AdbDaemon.videoErrorFile(context, token)
        listOf(startMarker, endMarker, errorFile).forEach { it.delete() }
        // A stop left over from an abandoned segment must not kill this one.
        AdbDaemon.videoStopRequest(context).delete()
        activeToken = token

        AdbDaemon.writeAtomically(
            AdbDaemon.videoStartRequest(context),
            buildString {
                append(file.absolutePath).append('\n')
                append(size).append('\n')
                append(bitRate).append('\n')
                append(timeLimitSeconds).append('\n')
                append(token).append('\n')
            },
        )

        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (startMarker.isFile) {
                val elapsed = parseUptimeMs(startMarker.readText())
                if (elapsed > 0L) {
                    videoActive = true
                    return@withContext elapsed
                }
            }
            if (!AdbDaemon.isRunning(context)) {
                throw ShellException(ShellFailure.ADB_NOT_RUNNING)
            }
            delay(80L)
        }
        activeToken = null
        // The request may still be consumed after this timeout. Ask the daemon
        // to stop this token so no unattended screenrecord survives.
        runCatching { requestStop(token) }
        throw ShellException(ShellFailure.BACKEND_ERROR, "screenrecord did not start")
    }

    override suspend fun stopVideo() {
        withContext(Dispatchers.IO) {
            if (!videoActive) return@withContext
            val token = activeToken ?: return@withContext
            requestStop(token)
        }
    }

    override suspend fun awaitVideoEnd(): ShellVideoEnd = withContext(Dispatchers.IO) {
        val token = activeToken
            ?: throw ShellException(ShellFailure.BACKEND_ERROR, "No active segment")
        val endMarker = AdbDaemon.videoEndMarker(context, token)
        val errorFile = AdbDaemon.videoErrorFile(context, token)
        val deadline = SystemClock.elapsedRealtime() + 240_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (endMarker.isFile) {
                val text = runCatching { endMarker.readText() }.getOrDefault("")
                val parts = text.trim().split(Regex("\\s+"), limit = 2)
                val endedAt = parseUptimeMs(parts.getOrNull(0).orEmpty())
                val exitCode = parts.getOrNull(1)?.toIntOrNull() ?: -1
                if (endedAt > 0L) {
                    videoActive = false
                    activeToken = null
                    val error = runCatching {
                        errorFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
                    }.getOrDefault("")
                    runCatching { errorFile.delete() }
                    return@withContext ShellVideoEnd(endedAt, exitCode, error.take(8_000))
                }
            }
            if (!AdbDaemon.isRunning(context)) {
                throw ShellException(ShellFailure.ADB_NOT_RUNNING)
            }
            delay(120L)
        }
        throw ShellException(ShellFailure.BACKEND_ERROR, "screenrecord did not stop")
    }

    override fun close() {
        if (videoActive) {
            activeToken?.let { token ->
                runCatching { requestStop(token) }
            }
            videoActive = false
        }
        activeToken = null
    }

    private fun requestStop(token: String) {
        AdbDaemon.writeAtomically(AdbDaemon.videoStopRequest(context), "$token\n")
    }

    private fun readResult(expectedId: String): ShellCommandResult? {
        val file = AdbDaemon.resultFile(context)
        if (!file.isFile) return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        val first = text.indexOf('\n')
        if (first <= 0) return null
        val id = text.substring(0, first).trim()
        if (id != expectedId) return null
        val second = text.indexOf('\n', first + 1)
        if (second < 0) return null
        val exitCode = text.substring(first + 1, second).trim().toIntOrNull() ?: -1
        val output = text.substring(second + 1).trimEnd('\n')
        runCatching { file.delete() }
        return ShellCommandResult(exitCode, AdbDaemon.trimOutput(output))
    }

    private fun parseUptimeMs(raw: String): Long {
        val seconds = raw.trim().split(Regex("\\s+")).firstOrNull()?.toDoubleOrNull()
            ?: return 0L
        return (seconds * 1_000.0).toLong()
    }

    private companion object {
        /**
         * Every session shares the daemon's single command/result file pair.
         * Serialize process-wide, not per instance, or concurrent app-op and
         * recording sessions can overwrite each other's pending command.
         */
        val commandMutex = Mutex()
    }
}
