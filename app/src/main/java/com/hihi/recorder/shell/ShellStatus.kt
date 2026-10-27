package com.hihi.recorder.shell

import com.hihi.recorder.data.ShellBackend

/** Why a shell backend cannot record right now. */
enum class ShellFailure {
    ADB_NOT_RUNNING,
    SHIZUKU_NOT_RUNNING,
    SHIZUKU_NO_PERMISSION,
    NO_SU,
    PROMPT_TIMEOUT,
    NO_SCREENRECORD,
    BACKEND_ERROR,
}

/** Result of probing one of the three shell activation methods. */
sealed interface ShellStatus {
    data object Unknown : ShellStatus
    data object Checking : ShellStatus
    data class Available(val backend: ShellBackend) : ShellStatus
    data class Unavailable(
        val backend: ShellBackend,
        val reason: ShellFailure,
        val detail: String? = null,
    ) : ShellStatus
}

/** Thrown by a shell session when it cannot run a command or start screenrecord. */
class ShellException(
    val failure: ShellFailure,
    val detail: String? = null,
) : Exception(detail ?: failure.name)
