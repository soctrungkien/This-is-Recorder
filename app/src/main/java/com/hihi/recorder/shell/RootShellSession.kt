package com.hihi.recorder.shell

import android.os.SystemClock
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.root.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/** screenrecord through `su -c`, using the same segment script as the old root mode. */
internal class RootShellSession : ShellSession {

    override val backend: ShellBackend = ShellBackend.ROOT

    @Volatile
    private var process: Process? = null

    @Volatile
    private var currentFile: File? = null

    @Volatile
    private var childPid: Int = 0

    private val processError = StringBuilder()

    override suspend fun open() = withContext(Dispatchers.IO) {
        val id = RootShell.run("id", timeoutMs = 20_000L)
        when {
            id.timedOut -> throw ShellException(ShellFailure.PROMPT_TIMEOUT)
            !id.isSuccess || !id.output.contains("uid=0") ->
                throw ShellException(ShellFailure.NO_SU, id.output)

            else -> Unit
        }
        val binary = RootShell.run(
            "test -x /system/bin/screenrecord || command -v screenrecord",
            timeoutMs = 10_000L,
        )
        // `test -x` succeeds without printing, so the exit status is the only
        // reliable signal here.
        if (!binary.isSuccess) {
            throw ShellException(ShellFailure.NO_SCREENRECORD, binary.output)
        }
    }

    override suspend fun exec(command: String, timeoutMs: Long): ShellCommandResult {
        val result = RootShell.run(command, timeoutMs)
        return ShellCommandResult(
            exitCode = result.exitCode,
            output = AdbDaemon.trimOutput(result.output),
            timedOut = result.timedOut,
        )
    }

    override suspend fun startVideo(
        file: File,
        size: String,
        bitRate: Int,
        timeLimitSeconds: Int,
    ): Long = withContext(Dispatchers.IO) {
        val directory = file.parentFile ?: throw ShellException(ShellFailure.BACKEND_ERROR)
        directory.mkdirs()
        file.delete()
        val script = File(directory, "record_segment.sh").apply {
            writeText(SEGMENT_SCRIPT)
        }
        synchronized(processError) { processError.setLength(0) }

        val command = "sh ${RootShell.quote(script.absolutePath)} " +
            "${RootShell.quote(file.absolutePath)} " +
            "${RootShell.quote(size)} " +
            RootShell.quote(bitRate.toString()) + " " +
            RootShell.quote(timeLimitSeconds.toString())
        val su = RootShell.suBinary()
        val started = runCatching {
            ProcessBuilder(su, "-c", command).start()
        }.getOrElse { first ->
            // Mirror RootShell.run(): a known absolute path can be visible but
            // not executable in the app's context; try plain `su` once.
            if (su != "su") {
                ProcessBuilder("su", "-c", command).start()
            } else {
                throw first
            }
        }
        process = started
        currentFile = file
        childPid = 0

        val pidSignal = ArrayBlockingQueue<Int>(1)
        Thread {
            runCatching {
                started.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (line.startsWith(PID_PREFIX)) {
                            val pid = line.removePrefix(PID_PREFIX).trim().toIntOrNull()
                            if (pid != null) pidSignal.offer(pid)
                        }
                    }
                }
            }
        }.apply { isDaemon = true }.start()
        Thread {
            runCatching {
                started.errorStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        synchronized(processError) {
                            if (processError.length < 8_000) {
                                processError.append(line).append('\n')
                            }
                        }
                    }
                }
            }
        }.apply { isDaemon = true }.start()

        val pid = withTimeoutOrNull(15_000L) {
            while (true) {
                val value = pidSignal.poll(100L, TimeUnit.MILLISECONDS)
                if (value != null && value > 0) return@withTimeoutOrNull value
                if (!started.isAlive) return@withTimeoutOrNull null
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
        if (pid == null) {
            runCatching { started.destroyForcibly() }
            process = null
            val detail = synchronized(processError) { processError.toString().trim() }
            throw ShellException(ShellFailure.BACKEND_ERROR, detail.take(1_500))
        }
        childPid = pid
        SystemClock.elapsedRealtime()
    }

    override suspend fun stopVideo() {
        withContext(Dispatchers.IO) {
            val pid = childPid
            if (pid > 0) {
                RootShell.run("kill -2 $pid", timeoutMs = 5_000L)
            } else {
                process?.let { runCatching { it.destroy() } }
            }
        }
    }

    override suspend fun awaitVideoEnd(): ShellVideoEnd = withContext(Dispatchers.IO) {
        val active = process ?: throw ShellException(ShellFailure.BACKEND_ERROR)
        val exitCode = runCatching { active.waitFor() }.getOrDefault(-1)
        val endedAt = SystemClock.elapsedRealtime()
        process = null
        childPid = 0
        val file = currentFile
        currentFile = null
        val processText = synchronized(processError) { processError.toString().trim() }
        val fileText = runCatching {
            file?.let { File("${it.absolutePath}.err") }
                ?.takeIf { it.isFile }
                ?.readText()
                ?.trim()
                .orEmpty()
        }.getOrDefault("")
        val error = listOf(processText, fileText)
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        ShellVideoEnd(endedAt, exitCode, error.take(8_000))
    }

    override fun close() {
        val pid = childPid
        if (pid > 0) {
            // The app cannot signal a root-owned screenrecord directly. Kill it
            // through su on a background thread so aborting never blocks the
            // main thread.
            Thread {
                kotlinx.coroutines.runBlocking {
                    runCatching { RootShell.run("kill -2 $pid", timeoutMs = 3_000L) }
                }
            }.apply { isDaemon = true }.start()
        }
        process?.let { runCatching { it.destroyForcibly() } }
        process = null
        currentFile = null
        childPid = 0
    }

    private companion object {
        const val PID_PREFIX = "PID:"

        val SEGMENT_SCRIPT = """
            #!/system/bin/sh
            out="${'$'}1"
            size="${'$'}2"
            bitrate="${'$'}3"
            limit="${'$'}4"
            err="${'$'}out.err"
            screenrecord --size "${'$'}size" --bit-rate "${'$'}bitrate" --time-limit "${'$'}limit" "${'$'}out" 2>"${'$'}err" &
            child=${'$'}!
            echo "PID:${'$'}child"
            wait "${'$'}child"
            rc=${'$'}?
            chmod 666 "${'$'}out" "${'$'}err" 2>/dev/null
            exit ${'$'}rc
        """.trimIndent() + "\n"
    }
}
