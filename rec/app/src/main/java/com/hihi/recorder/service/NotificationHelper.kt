package com.hihi.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import com.hihi.recorder.MainActivity
import com.hihi.recorder.R
import com.hihi.recorder.data.AudioSource
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.SavedRecording
import com.hihi.recorder.data.VideoResolution

/** Builds the ongoing and result notifications used by [RecordingService]. */
object NotificationHelper {

    const val CHANNEL_RECORDING = "recording"
    const val CHANNEL_RESULTS = "results"

    const val RECORDING_NOTIFICATION_ID = 1201
    const val RESULT_NOTIFICATION_ID = 1202
    const val ERROR_NOTIFICATION_ID = 1203

    private const val ACCENT = 0xFFE5484D.toInt()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val recording = NotificationChannel(
            CHANNEL_RECORDING,
            context.getString(R.string.notification_channel_recording),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_recording_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val results = NotificationChannel(
            CHANNEL_RESULTS,
            context.getString(R.string.notification_channel_results),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_results_desc)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(recording, results))
    }

    fun preparing(context: Context, text: String = context.getString(R.string.notification_preparing_text)): Notification =
        baseRecording(context)
            .setContentTitle(context.getString(R.string.notification_preparing_title))
            .setContentText(text)
            .setOngoing(true)
            .build()

    fun countdown(context: Context, seconds: Int): Notification =
        baseRecording(context)
            .setContentTitle(context.getString(R.string.notification_countdown_title, seconds))
            .setContentText(context.getString(R.string.notification_countdown_text))
            .setOngoing(true)
            .addAction(stopAction(context))
            .build()

    fun recording(
        context: Context,
        settings: RecorderSettings,
        startedAtElapsedRealtime: Long,
    ): Notification {
        val elapsed = (SystemClock.elapsedRealtime() - startedAtElapsedRealtime).coerceAtLeast(0L)
        val text = recordingText(context, settings)
        return baseRecording(context)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText(text)
            .setWhen(System.currentTimeMillis() - elapsed)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setOngoing(true)
            .addAction(stopAction(context))
            .build()
    }

    fun stopping(context: Context): Notification =
        baseRecording(context)
            .setContentTitle(context.getString(R.string.notification_stopping_title))
            .setContentText(context.getString(R.string.notification_stopping_text))
            .setOngoing(true)
            .build()

    fun saved(context: Context, recording: SavedRecording): Notification {
        val size = formatBytes(recording.sizeBytes)
        val duration = formatDuration(recording.durationMs)
        val text = context.getString(R.string.notification_saved_text, size, duration)
        val open = openPendingIntent(context, recording.uri)
        return Notification.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_stat_recorder)
            .setColor(ACCENT)
            .setContentTitle(context.getString(R.string.notification_saved_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_recorder),
                    context.getString(R.string.notification_action_open),
                    open,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_recorder),
                    context.getString(R.string.notification_action_share),
                    sharePendingIntent(context, recording.uri),
                ).build(),
            )
            .build()
    }

    fun error(context: Context, message: String): Notification {
        val text = message.ifBlank { context.getString(R.string.notification_error_title) }
        return Notification.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_stat_recorder)
            .setColor(ACCENT)
            .setContentTitle(context.getString(R.string.notification_error_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(activityPendingIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ERROR)
            .build()
    }

    fun cancelRecording(context: Context) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(RECORDING_NOTIFICATION_ID)
        }
    }

    fun notify(context: Context, id: Int, notification: Notification) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                ?.notify(id, notification)
        }
    }

    private fun baseRecording(context: Context): Notification.Builder =
        Notification.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_recorder)
            .setColor(ACCENT)
            .setContentIntent(activityPendingIntent(context))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }

    private fun stopAction(context: Context): Notification.Action {
        val intent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_STOP)
        val pending = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_stat_recorder),
            context.getString(R.string.notification_action_stop),
            pending,
        ).build()
    }

    private fun activityPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openPendingIntent(context: Context, uri: Uri): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun sharePendingIntent(context: Context, uri: Uri): PendingIntent {
        val send = Intent(Intent.ACTION_SEND)
            .setType("video/mp4")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, context.getString(R.string.action_share))
        return PendingIntent.getActivity(
            context,
            3,
            chooser,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun recordingText(context: Context, settings: RecorderSettings): String {
        val resolution = when (settings.resolution) {
            VideoResolution.P720 -> context.getString(R.string.resolution_720)
            VideoResolution.P1080 -> context.getString(R.string.resolution_1080)
            VideoResolution.P1440 -> context.getString(R.string.resolution_1440)
            VideoResolution.NATIVE -> context.getString(R.string.resolution_native)
        }
        val effectiveAudio = if (settings.captureMode == CaptureMode.ROOT) {
            if (settings.audioSource == AudioSource.NONE) AudioSource.NONE else AudioSource.MICROPHONE
        } else {
            settings.audioSource
        }
        if (effectiveAudio == AudioSource.NONE) {
            return context.getString(R.string.notification_recording_text_no_audio, resolution)
        }
        val audio = when (effectiveAudio) {
            AudioSource.NONE -> context.getString(R.string.audio_none)
            AudioSource.MICROPHONE -> context.getString(R.string.audio_mic)
            AudioSource.DEVICE -> context.getString(R.string.audio_device)
            AudioSource.MIC_AND_DEVICE -> context.getString(R.string.audio_mix)
        }
        return context.getString(R.string.notification_recording_text, resolution, audio)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val megabytes = bytes / (1024.0 * 1024.0)
        return if (megabytes >= 1.0) {
            String.format(java.util.Locale.getDefault(), "%.1f MB", megabytes)
        } else {
            String.format(java.util.Locale.getDefault(), "%.0f KB", bytes / 1024.0)
        }
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
