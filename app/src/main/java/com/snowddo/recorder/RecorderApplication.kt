package com.snowddo.recorder

import android.app.Application
import com.snowddo.recorder.data.RecordingLibrary
import com.snowddo.recorder.data.RecordingStorage
import com.snowddo.recorder.data.SettingsRepository
import com.snowddo.recorder.service.NotificationHelper

class RecorderApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var recordingLibrary: RecordingLibrary
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        recordingLibrary = RecordingLibrary(this)
        NotificationHelper.ensureChannels(this)
        Thread {
            runCatching { RecordingStorage.cleanOldTempFiles(this) }
        }.start()
    }
}
