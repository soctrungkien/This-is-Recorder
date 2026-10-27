package com.hihi.recorder.shell

import android.content.Context
import android.os.SystemClock
import androidx.annotation.Keep
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Runs with the shell identity inside the process created by Shizuku's
 * UserService mechanism. It owns one screenrecord child at a time and exposes
 * small command helpers for app-ops and diagnostics.
 *
 * `screenrecord` is started through a tiny script so the service learns the
 * child PID from a file. Android's [Process] has no portable `pid()`, and the
 * service must signal screenrecord (not the wrapper) to finalize the MP4.
 */
class ShizukuShellService : IShellUserService.Stub {

    private val lock = Any()

    private var process: Process? = null
    private var childPid = 0
    private var running = false
    private var endedAt = 0L
    private var exitCode = -1
    private var error = ""

    @Suppress("unused")
    @Keep
    constructor()

    @Suppress("unused", "UNUSED_PARAMETER")
    @Keep
    constructor(context: Context)

    override fun destroy() {
        synchronized(lock) { stopLocked(force = true) }
        exitProcess(0)
    }

    override fun exit() {
        destroy()
    }

    override fun runCommand(command: String, timeoutMs: Int): String {
        val child = try {
            ProcessBuilder("/system/bin/sh", "-c", command)
                .redirectErrorStream(true)
                .start()
        } catch (failure: Throwable) {
            return "-1\n${failure.message.orEmpty()}"
        }

        var output = ""
        val reader = Thread {
            output = runCatching {
                child.inputStream.bufferedReader().use { it.readText() }
            }.getOrDefault("")
        }.apply { isDaemon = true }
        reader.start()

        val finished = runCatching {
            child.waitFor(
                timeoutMs.coerceAtLeast(1_000).toLong(),
                TimeUnit.MILLISECONDS,
            )
        }.getOrDefault(false)
        if (!finished) {
            runCatching { child.destroyForcibly() }
        }
        runCatching { reader.join(1_000L) }
        val code = if (finished) runCatching { child.exitValue() }.getOrDefault(-1) else -1
        val suffix = if (finished) "" else "\nCommand timed out"
        return "$code\n${output.take(64_000)}$suffix"
    }

    override fun startSegment(
        path: String,
        size: String,
        bitRate: Int,
        timeLimitSeconds: Int,
    ): Long = synchronized(lock) {
        stopLocked(force = true)
        val file = File(path).apply {
            parentFile?.mkdirs()
            delete()
        }
        val script = File("${file.absolutePath}.sh")
        val errorFile = File("${file.absolutePath}.err")
        val pidFile = File("${file.absolutePath}.pid")
        listOf(errorFile, pidFile).forEach { it.delete() }

        try {
            script.writeText(SEGMENT_SCRIPT)
            val child = ProcessBuilder(
                "/system/bin/sh",
                script.absolutePath,
                file.absolutePath,
                size,
                bitRate.toString(),
                timeLimitSeconds.toString(),
            ).start()
            process = child
            childPid = 0
            running = true
            endedAt = 0L
            exitCode = -1
            error = ""
            val startedAt = SystemClock.elapsedRealtime()

            Thread {
                val output = runCatching {
                    child.inputStream.bufferedReader().use { it.readText() }
                }.getOrDefault("")
                val code = runCatching { child.waitFor() }.getOrDefault(-1)
                val fileError = runCatching {
                    errorFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
                }.getOrDefault("")
                synchronized(lock) {
                    if (process === child) {
                        process = null
                        childPid = 0
                        running = false
                        endedAt = SystemClock.elapsedRealtime()
                        exitCode = code
                        error = listOf(output.trim(), fileError)
                            .filter { it.isNotEmpty() }
                            .joinToString("\n")
                            .takeLast(8_000)
                    }
                }
            }.apply { isDaemon = true }.start()

            val pid = waitForPid(pidFile)
            if (pid <= 0) {
                stopLocked(force = true)
                running = false
                endedAt = SystemClock.elapsedRealtime()
                exitCode = -1
                error = "screenrecord did not start"
                return@synchronized -1L
            }
            childPid = pid
            startedAt
        } catch (failure: Throwable) {
            stopLocked(force = true)
            running = false
            endedAt = SystemClock.elapsedRealtime()
            exitCode = -1
            error = failure.message.orEmpty()
            runCatching { script.delete() }
            -1L
        }
    }

    override fun stopSegment() {
        synchronized(lock) {
            // Signal screenrecord only. The wrapper must stay alive to reap it,
            // chmod the output and record the real end time.
            stopLocked(force = false)
        }
    }

    override fun segmentEndedAt(): Long = synchronized(lock) { endedAt }

    override fun segmentExitCode(): Int = synchronized(lock) { exitCode }

    override fun segmentError(): String = synchronized(lock) { error }

    override fun segmentRunning(): Boolean = synchronized(lock) { running }

    private fun waitForPid(pidFile: File): Int {
        val deadline = SystemClock.elapsedRealtime() + 2_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val pid = runCatching {
                pidFile.takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull()
            }.getOrNull()
            if (pid != null && pid > 0) return pid
            Thread.sleep(40L)
        }
        return 0
    }

    private fun stopLocked(force: Boolean) {
        val pid = childPid
        if (pid > 0) {
            // The service and screenrecord share the shell UID, so a direct
            // signal through /system/bin/kill is allowed.
            childPid = 0
            runCatching {
                ProcessBuilder("/system/bin/kill", "-2", pid.toString()).start()
            }
        }
        if (force) {
            // Used only by destroy() and failed starts: finalization is not
            // needed because the segment is being discarded.
            process?.let { runCatching { it.destroyForcibly() } }
            process = null
        }
    }

    private companion object {
        val SEGMENT_SCRIPT = """
            #!/system/bin/sh
            out="${'$'}1"
            size="${'$'}2"
            bitrate="${'$'}3"
            limit="${'$'}4"
            err="${'$'}out.err"
            screenrecord --size "${'$'}size" --bit-rate "${'$'}bitrate" --time-limit "${'$'}limit" "${'$'}out" 2>"${'$'}err" &
            child=${'$'}!
            echo "${'$'}child" > "${'$'}out.pid.tmp"
            mv -f "${'$'}out.pid.tmp" "${'$'}out.pid"
            wait "${'$'}child"
            rc=${'$'}?
            chmod 666 "${'$'}out" "${'$'}err" 2>/dev/null
            exit ${'$'}rc
        """.trimIndent() + "\n"
    }
}
