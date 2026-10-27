package com.hihi.recorder.shell

import android.content.Context
import com.hihi.recorder.data.ShellBackend

/** Creates one session for the selected activation method. */
object ShellSessions {

    fun create(context: Context, backend: ShellBackend): ShellSession = when (backend) {
        ShellBackend.ADB -> AdbShellSession(context.applicationContext)
        ShellBackend.SHIZUKU -> ShizukuShellSession(context.applicationContext)
        ShellBackend.ROOT -> RootShellSession()
    }

    /**
     * Opens a session, runs [block] and always closes it. Used for short-lived
     * app-op work where no screenrecord process is needed.
     */
    suspend fun <T> withSession(
        context: Context,
        backend: ShellBackend,
        block: suspend (ShellSession) -> T,
    ): T {
        val session = create(context, backend)
        try {
            session.open()
            return block(session)
        } finally {
            session.close()
        }
    }
}
