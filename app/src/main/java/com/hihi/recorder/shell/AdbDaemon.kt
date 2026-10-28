package com.hihi.recorder.shell

import android.content.Context
import com.hihi.recorder.data.ShellBackend

/**
 * ADB activation is a one-time command the user runs from a computer. That
 * command starts [ShellFileDaemon]'s script with the shell identity; the daemon
 * keeps running in the background and the app talks to it through files.
 *
 * The root backend uses the same daemon protocol with a separate directory, so
 * both transports share the command and screenrecord implementation.
 */
object AdbDaemon {

    private const val FOLDER = "shell"

    internal fun daemon(context: Context): ShellFileDaemon =
        ShellFileDaemon(context, ShellBackend.ADB, FOLDER)

    fun ensureScripts(context: Context) {
        daemon(context).ensureScripts()
    }

    fun isRunning(context: Context): Boolean = daemon(context).isRunning()

    /** Command shown to the user; run it once from a computer with adb. */
    fun activationCommand(context: Context): String {
        val daemon = daemon(context)
        daemon.ensureScripts()
        return "adb shell sh \"${daemon.startScript().absolutePath}\""
    }

    /** Optional command shown in the ADB note. */
    fun shutdownCommand(context: Context): String {
        val daemon = daemon(context)
        daemon.ensureScripts()
        return "adb shell sh \"${daemon.stopScript().absolutePath}\""
    }

    fun trimOutput(output: String): String = ShellFileDaemon.trimOutput(output)
}
