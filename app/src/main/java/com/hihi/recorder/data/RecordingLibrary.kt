package com.hihi.recorder.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** A recording that has been finalized in the shared MediaStore. */
data class SavedRecording(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val savedAtEpochMs: Long,
    val relativePath: String = "",
)

/** Lists and manages the videos this app contributed to MediaStore. */
class RecordingLibrary(context: Context) {

    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _lastRecording = MutableStateFlow(readLastRecording())
    val lastRecording: StateFlow<SavedRecording?> = _lastRecording.asStateFlow()

    private val _recordings = MutableStateFlow<List<SavedRecording>>(emptyList())
    val recordings: StateFlow<List<SavedRecording>> = _recordings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Called when a service has just published a new MediaStore row. */
    @Synchronized
    fun save(recording: SavedRecording) {
        persistLast(recording)
        _recordings.value = (listOf(recording) + _recordings.value)
            .distinctBy { it.uri }
            .sortedByDescending { it.savedAtEpochMs }
    }

    @Synchronized
    fun forget() {
        preferences.edit().clear().apply()
        _lastRecording.value = null
    }

    /**
     * Reloads all videos owned by this package. This works without a media
     * permission because an app can always see media it contributed itself.
     */
    suspend fun refresh(): Result<Unit> {
        // Set this before switching dispatchers so pull-to-refresh can show its
        // indicator as soon as the gesture completes.
        _isLoading.value = true
        return try {
            withContext(Dispatchers.IO) {
                try {
                    val loaded = queryOwnedRecordings()
                    _recordings.value = loaded

                    val currentLast = _lastRecording.value
                    val matchingLast = currentLast?.let { last ->
                        loaded.firstOrNull { it.uri == last.uri }
                    }
                    if (matchingLast != null) {
                        persistLast(matchingLast)
                    } else if (currentLast != null) {
                        preferences.edit().clear().apply()
                        _lastRecording.value = null
                    }
                    Result.success(Unit)
                } catch (error: Throwable) {
                    Result.failure(error)
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun rename(recording: SavedRecording, newName: String): Result<SavedRecording> =
        withContext(Dispatchers.IO) {
            val safeName = sanitizeDisplayName(newName)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("The file name is empty"),
                )
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, safeName)
                }
                val changed = appContext.contentResolver.update(recording.uri, values, null, null)
                if (changed <= 0) {
                    return@withContext Result.failure(
                        IllegalStateException("The video could not be renamed"),
                    )
                }
                refresh().getOrThrow()
                val updated = _recordings.value.firstOrNull { it.uri == recording.uri }
                    ?: recording.copy(displayName = safeName)
                if (_lastRecording.value?.uri == recording.uri) {
                    persistLast(updated)
                }
                Result.success(updated)
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    suspend fun delete(recording: SavedRecording): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val removed = appContext.contentResolver.delete(recording.uri, null, null)
            if (removed <= 0) {
                return@withContext Result.failure(
                    IllegalStateException("The video could not be deleted"),
                )
            }
            val stillLast = _lastRecording.value?.uri == recording.uri
            refresh().getOrThrow()
            if (stillLast) {
                val newest = _recordings.value.firstOrNull()
                if (newest != null) {
                    persistLast(newest)
                } else {
                    preferences.edit().clear().apply()
                    _lastRecording.value = null
                }
            }
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun queryOwnedRecordings(): List<SavedRecording> {
        val resolver = appContext.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Video.Media.OWNER_PACKAGE_NAME} = ?"
        val args = arrayOf(appContext.packageName)
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val result = mutableListOf<SavedRecording>()
        resolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val takenIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = Uri.withAppendedPath(collection, id.toString())
                val taken = cursor.getLong(takenIndex)
                val added = cursor.getLong(addedIndex)
                result += SavedRecording(
                    uri = uri,
                    displayName = cursor.getString(nameIndex)
                        ?: "ThisIsRecorder_$id.mp4",
                    durationMs = cursor.getLong(durationIndex).coerceAtLeast(0L),
                    sizeBytes = cursor.getLong(sizeIndex).coerceAtLeast(0L),
                    savedAtEpochMs = if (taken > 0L) taken else added * 1_000L,
                    relativePath = cursor.getString(pathIndex).orEmpty(),
                )
            }
        }
        return result
    }

    private fun persistLast(recording: SavedRecording) {
        preferences.edit()
            .putString(KEY_URI, recording.uri.toString())
            .putString(KEY_NAME, recording.displayName)
            .putLong(KEY_DURATION, recording.durationMs)
            .putLong(KEY_SIZE, recording.sizeBytes)
            .putLong(KEY_SAVED_AT, recording.savedAtEpochMs)
            .putString(KEY_RELATIVE_PATH, recording.relativePath)
            .apply()
        _lastRecording.value = recording
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
            relativePath = preferences.getString(KEY_RELATIVE_PATH, "").orEmpty(),
        )
    }

    private companion object {
        const val PREFS_NAME = "miuix_recorder_library"
        const val KEY_URI = "last_uri"
        const val KEY_NAME = "last_name"
        const val KEY_DURATION = "last_duration"
        const val KEY_SIZE = "last_size"
        const val KEY_SAVED_AT = "last_saved_at"
        const val KEY_RELATIVE_PATH = "last_relative_path"
    }
}

/** Produces a safe MP4 display name, or null when nothing usable remains. */
fun sanitizeDisplayName(raw: String): String? {
    var name = raw
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_")
        .trim('.', ' ')
    if (name.isBlank()) return null
    if (!name.endsWith(".mp4", ignoreCase = true)) name += ".mp4"
    if (name.length > 100) {
        val extension = ".mp4"
        name = name.take(100 - extension.length) + extension
    }
    return name
}
