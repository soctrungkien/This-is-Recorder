package com.hihi.recorder

import android.content.Context
import com.hihi.recorder.data.RecordingLibrary
import com.hihi.recorder.data.SettingsRepository

val Context.recorderApplication: RecorderApplication
    get() = applicationContext as RecorderApplication

val Context.settingsRepository: SettingsRepository
    get() = recorderApplication.settingsRepository

val Context.recordingLibrary: RecordingLibrary
    get() = recorderApplication.recordingLibrary
