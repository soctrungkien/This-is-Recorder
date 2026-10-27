package com.hihi.recorder.shell

import com.hihi.recorder.data.ShellBackend
import java.io.Closeable
import java.io.File

/** Outcome of a one-shot command executed with the shell identity. */
data class ShellCommandResult(
    val exitCode: Int,
    val output: String,
    val timedOut: Boolean = false,
) {
    val isSuccess: Boolean get() = !timedOut && exitCode == 0
}

/** End of one screenrecord segment. */
data class ShellVideoEnd(
    val endedAtElapsedMs: Long,
    val exitCode: Int,
    val error: String,
)

/**
 * A privileged command runner plus the screenrecord process it owns.
 *
 * Implementations are deliberately single-session: [startVideo] must not be
 * called again until [awaitVideoEnd] has returned.
 */
interface ShellSession : Closeable {
    val backend: ShellBackend

    /** Verifies the backend and the screenrecord binary. */
    suspend fun open()

    suspend fun exec(command: String, timeoutMs: Long = 15_000L): ShellCommandResult

    /**
     * Starts one screenrecord segment and returns the elapsed-realtime time at
     * which the process was spawned. The muxer later corrects the first-frame
     * delay from the encoded duration.
     */
    suspend fun startVideo(
        file: File,
        size: String,
        bitRate: Int,
        timeLimitSeconds: Int,
    ): Long

    /** Asks the active screenrecord process to finalize its file. */
    suspend fun stopVideo()

    /** Waits until the active segment process exits. */
    suspend fun awaitVideoEnd(): ShellVideoEnd
}
