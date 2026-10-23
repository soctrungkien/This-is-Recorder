package com.snowddo.recorder.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A recording that has been finalized in the shared MediaStore. */
data class SavedRecording(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val savedAtEpochMs: Long,
)

/** Keeps a small, persistent reference to the most recent recording. */
class RecordingLibrary(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _lastRecording = MutableStateFlow(readLastRecording())
    val lastRecording: StateFlow<SavedRecording?> = _lastRecording.asStateFlow()

    fun save(recording: SavedRecording) {
        preferences.edit()
            .putString(KEY_URI, recording.uri.toString())
            .putString(KEY_NAME, recording.displayName)
            .putLong(KEY_DURATION, recording.durationMs)
            .putLong(KEY_SIZE, recording.sizeBytes)
            .putLong(KEY_SAVED_AT, recording.savedAtEpochMs)
            .apply()
        _lastRecording.value = recording
    }

    fun forget() {
        preferences.edit().clear().apply()
        _lastRecording.value = null
    }

    private fun readLastRecording(): SavedRecording? {
        val rawUri = preferences.getString(KEY_URI, null) ?: return null
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return null
        return SavedRecording(
            uri = uri,
            displayName = preferences.getString(KEY_NAME, null) ?: return null,
            durationMs = preferences.getLong(KEY_DURATION, 0L),
            sizeBytes = preferences.getLong(KEY_SIZE, 0L),
            savedAtEpochMs = preferences.getLong(KEY_SAVED_AT, 0L),
        )
    }

    private companion object {
        const val PREFS_NAME = "miuix_recorder_library"
        const val KEY_URI = "last_uri"
        const val KEY_NAME = "last_name"
        const val KEY_DURATION = "last_duration"
        const val KEY_SIZE = "last_size"
        const val KEY_SAVED_AT = "last_saved_at"
    }
}
