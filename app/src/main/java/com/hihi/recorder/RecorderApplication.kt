package com.hihi.recorder

import android.app.Application
import com.hihi.recorder.data.RecordingLibrary
import com.hihi.recorder.data.RecordingStorage
import com.hihi.recorder.data.SettingsRepository
import com.hihi.recorder.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecorderApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var recordingLibrary: RecordingLibrary
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        recordingLibrary = RecordingLibrary(this)
        NotificationHelper.ensureChannels(this)
        appScope.launch {
            runCatching { RecordingStorage.cleanOldTempFiles(this@RecorderApplication) }
            recordingLibrary.refresh()
        }
    }
}
