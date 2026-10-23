package com.hihi.recorder

import android.app.Application
import com.hihi.recorder.data.RecordingLibrary
import com.hihi.recorder.data.RecordingStorage
import com.hihi.recorder.data.SettingsRepository
import com.hihi.recorder.service.NotificationHelper

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
