package com.hihi.recorder.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Small wrapper around `su -c`. Root is optional: every caller must be able to
 * fall back to the normal MediaProjection recorder.
 */
object RootShell {

    data class Result(
        val exitCode: Int,
        val output: String,
        val timedOut: Boolean,
    ) {
        val isSuccess: Boolean get() = !timedOut && exitCode == 0
    }

    suspend fun run(command: String, timeoutMs: Long = 15_000L): Result =
        withContext(Dispatchers.IO) {
            val process = runCatching {
                ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start()
            }.getOrElse {
                return@withContext Result(
                    exitCode = -1,
                    output = it.message.orEmpty(),
                    timedOut = false,
                )
            }

            val finished = runCatching {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            }.getOrDefault(false)

            if (!finished) {
                process.destroyForcibly()
                process.waitFor(1, TimeUnit.SECONDS)
                return@withContext Result(
                    exitCode = -1,
                    output = "",
                    timedOut = true,
                )
            }

            val output = runCatching {
                process.inputStream.bufferedReader().use { it.readText() }
            }.getOrDefault("")

            Result(
                exitCode = process.exitValue(),
                output = output.trim(),
                timedOut = false,
            )
        }

    fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
