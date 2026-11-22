package com.hihi.recorder

import android.content.Intent
import com.acutecoder.crashhandler.CrashHandlerApplication
import com.acutecoder.crashhandler.util.installCrashHandler
import com.hihi.recorder.ui.CrashActivity
import com.hihi.recorder.data.RecordingLibrary
import com.hihi.recorder.data.RecordingStorage
import com.hihi.recorder.data.SettingsRepository
import com.hihi.recorder.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecorderApplication : CrashHandlerApplication() {

    init {
        installCrashHandler()
    }

    /** Show our own Miuix crash screen instead of the library's default one. */
    override fun startCrashHandlerActivity(defaultActivityClass: Class<*>) {
        startActivity(
            Intent(this, CrashActivity::class.java).apply {
                flags += Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var recordingLibrary: RecordingLibrary
        private set

    override fun onCreate() {
        super.onCreate()
        // COUI components use their native style by default; RecorderTheme
        // supplies a CouiTheme when the COUI UI style is selected.
        settingsRepository = SettingsRepository(this)
        recordingLibrary = RecordingLibrary(this)
        NotificationHelper.ensureChannels(this)
        appScope.launch {
            runCatching { RecordingStorage.cleanOldTempFiles(this@RecorderApplication) }
            recordingLibrary.refresh()
        }
    }
}
