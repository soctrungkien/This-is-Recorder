package com.hihi.recorder.shell

import android.content.Context
import androidx.annotation.StringRes
import com.hihi.recorder.R

/** Shared user-facing reason for every backend. */
@StringRes
fun ShellFailure.messageRes(): Int = when (this) {
    ShellFailure.ADB_NOT_RUNNING -> R.string.error_shell_adb_not_running
    ShellFailure.SHIZUKU_NOT_RUNNING -> R.string.error_shell_shizuku_not_running
    ShellFailure.SHIZUKU_NO_PERMISSION -> R.string.error_shell_shizuku_no_permission
    ShellFailure.NO_SU -> R.string.error_shell_no_root
    ShellFailure.PROMPT_TIMEOUT -> R.string.error_shell_root_timeout
    ShellFailure.NO_SCREENRECORD -> R.string.error_shell_no_screenrecord
    ShellFailure.BACKEND_ERROR -> R.string.error_shell_backend
}

fun ShellFailure.message(context: Context): String = context.getString(messageRes())

/** Quotes one argument for /system/bin/sh. */
fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
