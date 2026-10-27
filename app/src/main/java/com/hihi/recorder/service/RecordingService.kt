package com.hihi.recorder.service

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.hihi.recorder.R
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.RecorderSettings
import com.hihi.recorder.data.RecordingPhase
import com.hihi.recorder.data.RecordingStateStore
import com.hihi.recorder.data.RecordingStorage
import com.hihi.recorder.media.ProjectionRecorder
import com.hihi.recorder.media.RootRecorder
import com.hihi.recorder.recordingLibrary
import com.hihi.recorder.settingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that owns the recording session. It starts the foreground
 * notification before requesting a MediaProjection (Android 14 requirement),
 * runs the countdown, starts the selected engine and publishes the result.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stopRequested = AtomicBoolean(false)
    private var stopSignal = CompletableDeferred<Unit>()
    private var runJob: Job? = null
    private var autoStopJob: Job? = null
    private var recorder: ActiveRecorder? = null
    private var outputFile: File? = null
    private var currentSettings: RecorderSettings? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var receiverRegistered = false
    private var finishing = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            if (currentSettings?.stopOnScreenOff == true && RecordingStateStore.state.value.isActive) {
                requestStop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                requestStop()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                if (runJob?.isActive == true) return START_NOT_STICKY
                handleStart(intent)
                return START_NOT_STICKY
            }

            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun handleStart(intent: Intent) {
        val settings = applicationContext.settingsRepository.settings.value
        currentSettings = settings
        stopRequested.set(false)
        stopSignal = CompletableDeferred()
        finishing = false

        try {
            startForegroundNow(settings)
        } catch (error: Throwable) {
            RecordingStateStore.markError(
                error.message ?: getString(R.string.status_error_summary),
            )
            stopSelf()
            return
        }
        acquireWakeLock()

        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)

        runJob = scope.launch {
            runSession(settings, resultCode, data)
        }
    }

    private fun startForegroundNow(settings: RecorderSettings) {
        val notification = NotificationHelper.preparing(this)
        val type = foregroundServiceType(settings)
        ServiceCompat.startForeground(
            this,
            NotificationHelper.RECORDING_NOTIFICATION_ID,
            notification,
            type,
        )
    }

    private fun foregroundServiceType(settings: RecorderSettings): Int {
        var type = 0
        if (settings.captureMode == CaptureMode.STANDARD) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                settings.audioSource.usesMicrophone
            ) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                settings.audioSource.usesMicrophone
            ) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            if (settings.audioSource.usesDeviceAudio) {
                // Root video does not need MediaProjection, but phone audio
                // capture does.
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
        }
        return type
    }

    private suspend fun runSession(
        settings: RecorderSettings,
        resultCode: Int,
        projectionData: Intent?,
    ) {
        try {
            val countdown = settings.countdown.seconds
            if (countdown > 0) {
                for (remaining in countdown downTo 1) {
                    if (stopRequested.get()) {
                        finishCancelled()
                        return
                    }
                    RecordingStateStore.markPreparing(remaining)
                    NotificationHelper.notify(
                        this,
                        NotificationHelper.RECORDING_NOTIFICATION_ID,
                        NotificationHelper.countdown(this, remaining),
                    )
                    delay(1_000L)
                }
            }

            if (stopRequested.get()) {
                finishCancelled()
                return
            }

            RecordingStateStore.markPreparing(0)
            NotificationHelper.notify(
                this,
                NotificationHelper.RECORDING_NOTIFICATION_ID,
                NotificationHelper.preparing(this),
            )

            val file = RecordingStorage.newTempFile(this)
            outputFile = file

            val active = createRecorder(settings, file, resultCode, projectionData)
            recorder = active
            active.start()

            val startedAt = SystemClock.elapsedRealtime()
            RecordingStateStore.markRecording(startedAt, active.warning)
            NotificationHelper.notify(
                this,
                NotificationHelper.RECORDING_NOTIFICATION_ID,
                NotificationHelper.recording(this, settings, startedAt),
            )
            if (stopRequested.get()) {
                stopSignal.complete(Unit)
            }

            maybeVibrate(settings, start = true)

            val autoStop = settings.autoStop.minutes
            if (autoStop != null) {
                autoStopJob = scope.launch {
                    delay(autoStop * 60_000L)
                    requestStop()
                }
            }

            stopSignal.await()
            autoStopJob?.cancel()
            autoStopJob = null

            RecordingStateStore.markStopping()
            NotificationHelper.notify(
                this,
                NotificationHelper.RECORDING_NOTIFICATION_ID,
                NotificationHelper.stopping(this),
            )

            active.stop()
            active.warning?.let { RecordingStateStore.markWarning(it) }
            val duration = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
            val saved = withContext(Dispatchers.IO) {
                RecordingStorage.publish(
                    context = this@RecordingService,
                    source = file,
                    durationMs = duration,
                    folder = settings.saveFolder,
                )
            }
            applicationContext.recordingLibrary.save(saved)
            RecordingStateStore.markSaved()
            NotificationHelper.cancelRecording(this)
            NotificationHelper.notify(
                this,
                NotificationHelper.RESULT_NOTIFICATION_ID,
                NotificationHelper.saved(this, saved),
            )
            maybeVibrate(settings, start = false)
            finishSession()
        } catch (cancelled: CancellationException) {
            cleanupFailedSession(deleteFile = true)
            finishSession()
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.status_error_summary)
            cleanupFailedSession(deleteFile = true)
            RecordingStateStore.markError(message)
            NotificationHelper.cancelRecording(this)
            NotificationHelper.notify(
                this,
                NotificationHelper.ERROR_NOTIFICATION_ID,
                NotificationHelper.error(this, message),
            )
            finishSession()
        }
    }

    private fun createRecorder(
        settings: RecorderSettings,
        file: File,
        resultCode: Int,
        projectionData: Intent?,
    ): ActiveRecorder {
        if (settings.captureMode == CaptureMode.ROOT) {
            val needsProjection = settings.audioSource.usesDeviceAudio
            val projection = if (needsProjection) {
                if (resultCode != Activity.RESULT_OK || projectionData == null) {
                    throw IllegalStateException(getString(R.string.projection_denied))
                }
                val manager = getSystemService(MediaProjectionManager::class.java)
                    ?: throw IllegalStateException(getString(R.string.projection_unavailable))
                manager.getMediaProjection(resultCode, projectionData)
                    ?: throw IllegalStateException(getString(R.string.projection_unavailable))
            } else {
                null
            }
            return RootRecorder(
                context = this,
                settings = settings,
                outputFile = file,
                projection = projection,
                onProjectionStopped = { requestStop() },
                onUnexpectedStop = { requestStop() },
            ).asActive()
        }

        if (resultCode != Activity.RESULT_OK || projectionData == null) {
            throw IllegalStateException(getString(R.string.projection_denied))
        }
        val manager = getSystemService(MediaProjectionManager::class.java)
            ?: throw IllegalStateException(getString(R.string.projection_unavailable))
        val projection = manager.getMediaProjection(resultCode, projectionData)
            ?: throw IllegalStateException(getString(R.string.projection_unavailable))
        return ProjectionRecorder(
            context = this,
            settings = settings,
            outputFile = file,
            projection = projection,
            onProjectionStopped = { requestStop() },
        ).asActive()
    }

    private fun requestStop() {
        if (!stopRequested.compareAndSet(false, true)) {
            stopSignal.complete(Unit)
            return
        }
        if (runJob?.isActive != true) {
            NotificationHelper.cancelRecording(this)
            finishSession()
            return
        }

        val phase = RecordingStateStore.state.value.phase
        if (phase == RecordingPhase.RECORDING || phase == RecordingPhase.STOPPING) {
            RecordingStateStore.markStopping()
            NotificationHelper.notify(
                this,
                NotificationHelper.RECORDING_NOTIFICATION_ID,
                NotificationHelper.stopping(this),
            )
        }
        stopSignal.complete(Unit)
    }

    private fun finishCancelled() {
        RecordingStateStore.markIdle()
        NotificationHelper.cancelRecording(this)
        finishSession()
    }

    private fun cleanupFailedSession(deleteFile: Boolean) {
        autoStopJob?.cancel()
        autoStopJob = null
        recorder?.let { active ->
            runCatching { active.abort() }
        }
        recorder = null
        if (deleteFile) {
            outputFile?.let { runCatching { it.delete() } }
        }
        outputFile = null
    }

    private fun finishSession() {
        if (finishing) return
        finishing = true
        autoStopJob?.cancel()
        autoStopJob = null
        runJob = null
        recorder = null
        currentSettings = null
        outputFile = null
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ThisIsRecorder:recording",
        ).apply {
            setReferenceCounted(false)
            acquire(6L * 60L * 60L * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) runCatching { lock.release() }
        }
        wakeLock = null
    }

    private fun maybeVibrate(settings: RecorderSettings, start: Boolean) {
        if (!settings.hapticFeedback) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            } ?: return
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    if (start) 28L else 20L,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                ),
            )
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenOffReceiver) }
            receiverRegistered = false
        }
        if (RecordingStateStore.state.value.isActive) {
            RecordingStateStore.markError(getString(R.string.error_recording_interrupted))
        }
        cleanupFailedSession(deleteFile = true)
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private interface ActiveRecorder {
        val warning: String?
        suspend fun start()
        suspend fun stop()
        fun abort()
    }

    private fun RootRecorder.asActive(): ActiveRecorder = object : ActiveRecorder {
        override val warning: String? get() = this@asActive.warning
        override suspend fun start() = this@asActive.start()
        override suspend fun stop() = this@asActive.stop()
        override fun abort() = this@asActive.abort()
    }

    private fun ProjectionRecorder.asActive(): ActiveRecorder = object : ActiveRecorder {
        override val warning: String? get() = this@asActive.warning
        override suspend fun start() = withContext(Dispatchers.IO) { this@asActive.start() }
        override suspend fun stop() = withContext(Dispatchers.IO) { this@asActive.stop() }
        override fun abort() = this@asActive.abort()
    }

    companion object {
        const val ACTION_START = "com.hihi.recorder.action.START"
        const val ACTION_STOP = "com.hihi.recorder.action.STOP"
        const val EXTRA_RESULT_CODE = "com.hihi.recorder.extra.RESULT_CODE"
        const val EXTRA_RESULT_DATA = "com.hihi.recorder.extra.RESULT_DATA"
    }
}
