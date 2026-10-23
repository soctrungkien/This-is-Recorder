package com.hihi.recorder.root

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RootFailure {
    NO_SU,
    PROMPT_TIMEOUT,
    NO_SCREENRECORD,
}

sealed interface RootStatus {
    data object Unknown : RootStatus
    data object Checking : RootStatus
    data object Available : RootStatus
    data class Unavailable(val reason: RootFailure) : RootStatus
}

/** Detects su + the platform screenrecord binary without requiring root to exist. */
object RootChecker {

    @Volatile
    private var cachedSuccessAt: Long = 0L
    private const val CACHE_MS = 20_000L

    fun invalidate() {
        cachedSuccessAt = 0L
    }

    suspend fun check(context: Context, force: Boolean = false): RootStatus =
        withContext(Dispatchers.IO) {
            if (!force && System.currentTimeMillis() - cachedSuccessAt < CACHE_MS) {
                return@withContext RootStatus.Available
            }

            val id = RootShell.run("id", timeoutMs = 20_000L)
            if (id.timedOut) return@withContext RootStatus.Unavailable(RootFailure.PROMPT_TIMEOUT)
            if (!id.isSuccess || !id.output.contains("uid=0")) {
                return@withContext RootStatus.Unavailable(RootFailure.NO_SU)
            }

            val binary = RootShell.run(
                "command -v screenrecord || which screenrecord",
                timeoutMs = 10_000L,
            )
            if (!binary.isSuccess || !binary.output.contains("screenrecord")) {
                return@withContext RootStatus.Unavailable(RootFailure.NO_SCREENRECORD)
            }

            cachedSuccessAt = System.currentTimeMillis()
            RootStatus.Available
        }
}
