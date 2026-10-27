package com.hihi.recorder.shell

import android.content.Context
import android.os.SystemClock
import java.io.File

/**
 * Files shared with the ADB daemon.
 *
 * A normal app cannot execute `adb`, so ADB activation is a one-time command
 * the user runs from a computer. That command starts [daemonScript] with the
 * shell identity; the daemon keeps running in the background and the app talks
 * to it through small command/result files in its external files directory.
 * No internal app directory is used because the shell UID cannot read it.
 */
object AdbDaemon {

    private const val FOLDER = "shell"
    private const val HEARTBEAT_FRESH_MS = 6_000L
    private const val MAX_COMMAND_OUTPUT = 64_000

    fun directory(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, FOLDER).apply { mkdirs() }
    }

    fun startScript(context: Context): File = File(directory(context), "start.sh")

    fun stopScript(context: Context): File = File(directory(context), "stop.sh")

    private fun daemonScript(context: Context): File = File(directory(context), "daemon.sh")

    fun commandFile(context: Context): File = File(directory(context), "command")

    fun resultFile(context: Context): File = File(directory(context), "result")

    private fun heartbeatFile(context: Context): File = File(directory(context), "heartbeat")

    fun videoStartRequest(context: Context): File = File(directory(context), "video.start.req")

    fun videoStopRequest(context: Context): File = File(directory(context), "video.stop.req")

    fun videoStartMarker(context: Context, token: String): File =
        File(directory(context), "video.start.$token")

    fun videoEndMarker(context: Context, token: String): File =
        File(directory(context), "video.end.$token")

    fun videoErrorFile(context: Context, token: String): File =
        File(directory(context), "video.err.$token")

    /** Command shown to the user; run it once from a computer with adb. */
    fun activationCommand(context: Context): String =
        "adb shell sh \"${startScript(context).absolutePath}\""

    /** Optional command shown in the ADB note. */
    fun shutdownCommand(context: Context): String =
        "adb shell sh \"${stopScript(context).absolutePath}\""

    fun ensureScripts(context: Context) {
        val dir = directory(context)
        install(daemonScript(context), DAEMON_SCRIPT)
        install(startScript(context), START_SCRIPT)
        install(stopScript(context), STOP_SCRIPT)
        File(dir, ".nomedia").writeText("")
    }

    /**
     * Replaces a helper script without touching the inode of a script that may
     * already be running. POSIX shells read script files incrementally, so
     * rewriting `daemon.sh` in place while it executes can corrupt the loop.
     */
    private fun install(file: File, content: String) {
        val existing = runCatching {
            if (file.isFile) file.readText() else null
        }.getOrNull()
        if (existing == content) return

        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(content)
        if (!temp.renameTo(file)) {
            file.writeText(content)
            temp.delete()
        }
    }

    fun isRunning(context: Context): Boolean {
        val heartbeat = readHeartbeatElapsedMs(context) ?: return false
        val age = SystemClock.elapsedRealtime() - heartbeat
        return age in -HEARTBEAT_FRESH_MS..HEARTBEAT_FRESH_MS
    }

    /**
     * `/proc/uptime` is the same clock as [SystemClock.elapsedRealtime], so a
     * heartbeat written by the shell can be compared without any timezone or
     * wall-clock adjustment.
     */
    fun readHeartbeatElapsedMs(context: Context): Long? {
        val file = heartbeatFile(context)
        if (!file.isFile) return null
        val seconds = runCatching { file.readText().trim().toDoubleOrNull() }.getOrNull()
            ?: return null
        if (seconds <= 0.0) return null
        return (seconds * 1_000.0).toLong()
    }

    fun writeAtomically(file: File, text: String) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            file.writeText(text)
            temp.delete()
        }
    }

    fun trimOutput(output: String): String =
        if (output.length <= MAX_COMMAND_OUTPUT) output else output.take(MAX_COMMAND_OUTPUT)

    private val START_SCRIPT = """
        #!/system/bin/sh
        DIR=`dirname "${'$'}0"`
        PIDFILE="${'$'}DIR/daemon.pid"
        if [ -f "${'$'}PIDFILE" ]; then
          read PID REST < "${'$'}PIDFILE"
          if [ -n "${'$'}PID" ] && kill -0 "${'$'}PID" 2>/dev/null; then
            # PID files survive reboots, and the PID may then belong to an
            # unrelated process. Only treat it as our daemon when the command
            # line matches (fall back to trusting it if /proc is unreadable).
            if [ ! -r "/proc/${'$'}PID/cmdline" ] || \
              grep -q -F "${'$'}DIR/daemon.sh" "/proc/${'$'}PID/cmdline" 2>/dev/null; then
              echo "This is Recorder shell daemon is already running (pid ${'$'}PID)."
              exit 0
            fi
          fi
        fi
        rm -f "${'$'}DIR/daemon.stop"
        nohup /system/bin/sh "${'$'}DIR/daemon.sh" </dev/null >/dev/null 2>&1 &
        echo "This is Recorder shell daemon started in the background."
    """.trimIndent() + "\n"

    private val STOP_SCRIPT = """
        #!/system/bin/sh
        DIR=`dirname "${'$'}0"`
        touch "${'$'}DIR/daemon.stop"
        echo "This is Recorder shell daemon is stopping."
    """.trimIndent() + "\n"

    private val DAEMON_SCRIPT = """
        #!/system/bin/sh
        DIR=`dirname "${'$'}0"`
        COMMAND="${'$'}DIR/command"
        RESULT="${'$'}DIR/result"
        HEARTBEAT="${'$'}DIR/heartbeat"
        START_REQ="${'$'}DIR/video.start.req"
        STOP_REQ="${'$'}DIR/video.stop.req"
        PID_FILE="${'$'}DIR/video.pid"
        STOP_FILE="${'$'}DIR/daemon.stop"

        rm -f "${'$'}STOP_FILE" "${'$'}COMMAND" "${'$'}RESULT" \
          "${'$'}START_REQ" "${'$'}STOP_REQ"
        echo ${'$'}${'$'} > "${'$'}DIR/daemon.pid.tmp"
        mv -f "${'$'}DIR/daemon.pid.tmp" "${'$'}DIR/daemon.pid"
        ACTIVE_TOKEN=""

        screenrecord_pid() {
          [ -f "${'$'}PID_FILE" ] || return 1
          REC_PID=`cat "${'$'}PID_FILE" 2>/dev/null`
          [ -n "${'$'}REC_PID" ] || return 1
          kill -0 "${'$'}REC_PID" 2>/dev/null || return 1
          # A stale PID file can point at an unrelated process after a reboot
          # or PID reuse. /proc is readable for our own shell processes; when
          # it is not, trust kill -0 rather than skipping the stop.
          if [ -r "/proc/${'$'}REC_PID/cmdline" ] && \
            ! grep -q -F screenrecord "/proc/${'$'}REC_PID/cmdline" 2>/dev/null; then
            return 1
          fi
          printf '%s\n' "${'$'}REC_PID"
        }

        stop_video() {
          REC_PID=`screenrecord_pid`
          if [ -n "${'$'}REC_PID" ]; then
            kill -2 "${'$'}REC_PID" 2>/dev/null
          fi
        }

        # A daemon that died left screenrecord running. Stop the orphan so a
        # stale recording cannot fill the storage in the background.
        stop_video

        TICK=5
        while [ ! -f "${'$'}STOP_FILE" ]; do
          # The control loop polls fast, but a 1 s heartbeat is enough for the
          # app's 6 s freshness window and avoids constant flash writes.
          TICK=${'$'}((TICK + 1))
          if [ "${'$'}TICK" -ge 5 ]; then
            TICK=0
            { read UP REST < /proc/uptime; printf '%s\n' "${'$'}UP" > "${'$'}HEARTBEAT.tmp"; }
            mv -f "${'$'}HEARTBEAT.tmp" "${'$'}HEARTBEAT" 2>/dev/null
          fi

          if [ -f "${'$'}COMMAND" ]; then
            ID=`sed -n '1p' "${'$'}COMMAND"`
            LINE=`sed -n '2p' "${'$'}COMMAND"`
            OUT=`/system/bin/sh -c "${'$'}LINE" 2>&1`
            RC=${'$'}?
            {
              printf '%s\n' "${'$'}ID"
              printf '%s\n' "${'$'}RC"
              printf '%s\n' "${'$'}OUT"
            } > "${'$'}RESULT.tmp"
            mv -f "${'$'}RESULT.tmp" "${'$'}RESULT"
            rm -f "${'$'}COMMAND"
          fi

          if [ -f "${'$'}START_REQ" ]; then
            {
              read PATH_VALUE
              read SIZE_VALUE
              read BITRATE_VALUE
              read LIMIT_VALUE
              read TOKEN_VALUE
            } < "${'$'}START_REQ"
            START_MARKER="${'$'}DIR/video.start.${'$'}TOKEN_VALUE"
            END_MARKER="${'$'}DIR/video.end.${'$'}TOKEN_VALUE"
            ERROR_FILE="${'$'}DIR/video.err.${'$'}TOKEN_VALUE"

            # A previous session may have died without stopping screenrecord.
            # Signal it and give it a moment to finish before starting another.
            if [ -f "${'$'}PID_FILE" ]; then
              OLD=`screenrecord_pid`
              stop_video
              N=0
              while [ -n "${'$'}OLD" ] && kill -0 "${'$'}OLD" 2>/dev/null && [ "${'$'}N" -lt 30 ]; do
                sleep 0.1
                N=${'$'}((N + 1))
              done
            fi

            rm -f "${'$'}START_REQ" "${'$'}START_MARKER" "${'$'}END_MARKER" \
              "${'$'}ERROR_FILE" "${'$'}PID_FILE"
            # Stop requests carry the segment token, so a stop for an older
            # segment cannot kill this one. Set the token before the child
            # starts; the stop branch below may then cancel this segment if the
            # app gave up while the start request was still queued.
            ACTIVE_TOKEN="${'$'}TOKEN_VALUE"
            (
              /system/bin/screenrecord --size "${'$'}SIZE_VALUE" \
                --bit-rate "${'$'}BITRATE_VALUE" --time-limit "${'$'}LIMIT_VALUE" \
                "${'$'}PATH_VALUE" 2>"${'$'}ERROR_FILE" &
              CHILD=${'$'}!
              echo "${'$'}CHILD" > "${'$'}PID_FILE.tmp"
              mv -f "${'$'}PID_FILE.tmp" "${'$'}PID_FILE"
              { read UP REST < /proc/uptime; printf '%s\n' "${'$'}UP" > "${'$'}START_MARKER.tmp"; }
              mv -f "${'$'}START_MARKER.tmp" "${'$'}START_MARKER"
              wait "${'$'}CHILD"
              RC=${'$'}?
              # Only drop the pid file if it still describes this segment.
              # A new segment may already have written its own pid here.
              if [ "`cat "${'$'}PID_FILE" 2>/dev/null`" = "${'$'}CHILD" ]; then
                rm -f "${'$'}PID_FILE"
              fi
              chmod 666 "${'$'}PATH_VALUE" "${'$'}ERROR_FILE" 2>/dev/null
              { read UP REST < /proc/uptime; printf '%s %s\n' "${'$'}UP" "${'$'}RC" > "${'$'}END_MARKER.tmp"; }
              mv -f "${'$'}END_MARKER.tmp" "${'$'}END_MARKER"
            ) &
          fi

          if [ -f "${'$'}STOP_REQ" ]; then
            read STOP_TOKEN < "${'$'}STOP_REQ"
            rm -f "${'$'}STOP_REQ"
            if [ "${'$'}STOP_TOKEN" = "${'$'}ACTIVE_TOKEN" ]; then
              # The daemon may process a stop in the same tick that launched a
              # segment, before the wrapper has written its pid. Give the child
              # a moment so an in-flight start is not left recording.
              N=0
              while [ ! -f "${'$'}PID_FILE" ] && [ "${'$'}N" -lt 10 ]; do
                sleep 0.1
                N=${'$'}((N + 1))
              done
              stop_video
            fi
          fi

          sleep 0.2
        done

        stop_video
        rm -f "${'$'}DIR/daemon.pid" "${'$'}STOP_FILE" "${'$'}COMMAND" "${'$'}RESULT" \
          "${'$'}START_REQ" "${'$'}STOP_REQ"
        exit 0
    """.trimIndent() + "\n"
}
