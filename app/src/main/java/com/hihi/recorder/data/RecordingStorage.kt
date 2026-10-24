package com.hihi.recorder.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Temporary files and MediaStore publication for finished recordings. */
object RecordingStorage {

    private const val TEMP_FOLDER = "recordings"
    private const val MAX_TEMP_AGE_MS = 24L * 60L * 60L * 1000L

    fun newTempFile(context: Context): File {
        val directory = File(context.cacheDir, TEMP_FOLDER).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(directory, "recording_$stamp.mp4")
    }

    fun newSessionDirectory(context: Context): File {
        val directory = File(context.cacheDir, "root_sessions").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(directory, "session_$stamp").apply { mkdirs() }
    }

    fun cleanOldTempFiles(context: Context) {
        val now = System.currentTimeMillis()
        val directories = listOf(
            File(context.cacheDir, TEMP_FOLDER),
            File(context.cacheDir, "root_sessions"),
        )
        directories.forEach { directory ->
            directory.listFiles()?.forEach { file ->
                if (now - file.lastModified() > MAX_TEMP_AGE_MS) {
                    file.deleteRecursively()
                }
            }
        }
    }

    /**
     * Publishes [source] into `Movies/<folder>`. The temporary file is deleted
     * only after the MediaStore row has been finalized.
     */
    fun publish(
        context: Context,
        source: File,
        durationMs: Long,
        folder: String = RecorderSettings.DEFAULT_SAVE_FOLDER,
    ): SavedRecording {
        require(source.isFile && source.length() > 0) { "Recording file is empty" }

        val resolver = context.contentResolver
        val displayName = buildDisplayName()
        val relativePath = "${Environment.DIRECTORY_MOVIES}/${normalizeSaveFolder(folder)}"
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Video.Media.DATE_TAKEN, now)
            put(MediaStore.Video.Media.IS_PENDING, 1)
            // OWNER_PACKAGE_NAME is read-only; MediaProvider stamps the calling
            // package automatically and RecordingLibrary queries it by owner.
        }

        val uri = resolver.insert(collection, values)
            ?: error("Could not create a MediaStore entry")

        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open the output file")

            val size = source.length()
            // Publishing the row must not depend on optional metadata columns
            // being writable on every OEM MediaStore implementation.
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null,
            )
            runCatching {
                resolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.Video.Media.SIZE, size)
                        put(MediaStore.Video.Media.DURATION, durationMs)
                    },
                    null,
                    null,
                )
            }

            runCatching { source.delete() }
            return SavedRecording(
                uri = uri,
                displayName = displayName,
                durationMs = durationMs,
                sizeBytes = size,
                savedAtEpochMs = now,
                relativePath = relativePath,
            )
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    fun discard(context: Context, uri: Uri?) {
        if (uri == null) return
        runCatching { context.contentResolver.delete(uri, null, null) }
    }

    private fun buildDisplayName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "ThisIsRecorder_$stamp.mp4"
    }
}
