package com.hihi.recorder.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

    private val suCandidates = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/debug_ramdisk/su",
    )

    private fun resolveSu(): String =
        suCandidates.firstOrNull { File(it).exists() } ?: "su"

    /** The `su` binary used by [run]; sessions should spawn the same one. */
    fun suBinary(): String = resolveSu()

    suspend fun run(command: String, timeoutMs: Long = 15_000L): Result =
        withContext(Dispatchers.IO) {
            val su = resolveSu()
            val process = runCatching {
                ProcessBuilder(su, "-c", command)
                    .redirectErrorStream(true)
                    .start()
            }.getOrElse { firstError ->
                // Some root managers only expose `su` through $PATH rather than
                // one of the well-known absolute locations. Try the plain name
                // once before giving up.
                if (su != "su") {
                    runCatching {
                        ProcessBuilder("su", "-c", command)
                            .redirectErrorStream(true)
                            .start()
                    }.getOrElse {
                        return@withContext Result(
                            exitCode = -1,
                            output = firstError.message.orEmpty(),
                            timedOut = false,
                        )
                    }
                } else {
                    return@withContext Result(
                        exitCode = -1,
                        output = firstError.message.orEmpty(),
                        timedOut = false,
                    )
                }
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
