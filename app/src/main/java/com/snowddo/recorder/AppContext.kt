package com.snowddo.recorder

import android.content.Context
import com.snowddo.recorder.data.RecordingLibrary
import com.snowddo.recorder.data.SettingsRepository

val Context.recorderApplication: RecorderApplication
    get() = applicationContext as RecorderApplication

val Context.settingsRepository: SettingsRepository
    get() = recorderApplication.settingsRepository

val Context.recordingLibrary: RecordingLibrary
    get() = recorderApplication.recordingLibrary
