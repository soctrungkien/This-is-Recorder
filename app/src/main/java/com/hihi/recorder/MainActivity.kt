package com.hihi.recorder

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.hihi.recorder.data.AudioSource
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.RecordingStateStore
import com.hihi.recorder.root.RootChecker
import com.hihi.recorder.root.RootStatus
import com.hihi.recorder.service.RecordingService
import com.hihi.recorder.ui.RecorderApp
import com.hihi.recorder.ui.RecorderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.SnackbarHostState

class MainActivity : ComponentActivity() {

    private enum class PendingStart { STANDARD, ROOT }

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val snackbarHostState = SnackbarHostState()
    private lateinit var projectionManager: MediaProjectionManager
    private var pendingStart: PendingStart? = null

    private var autoStartToken by mutableStateOf(0)
    private var checkingRoot by mutableStateOf(false)
    private var showMicPermissionDialog by mutableStateOf(false)
    private var showRootUnavailableDialog by mutableStateOf(false)

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            startStandardService(result.resultCode, data)
        } else {
            showMessage(getString(R.string.projection_denied))
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val pending = pendingStart
        pendingStart = null
        val settings = settingsRepository.settings.value
        val needsAudio = settings.audioSource.requiresAudioPermission
        val audioGranted = !needsAudio ||
            result[Manifest.permission.RECORD_AUDIO] == true ||
            hasPermission(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            result.containsKey(Manifest.permission.POST_NOTIFICATIONS) &&
            result[Manifest.permission.POST_NOTIFICATIONS] != true
        ) {
            showMessage(getString(R.string.notification_permission_hint))
        }

        if (!audioGranted) {
            showMicPermissionDialog = true
            return@registerForActivityResult
        }
        when (pending) {
            PendingStart.STANDARD -> requestProjection()
            PendingStart.ROOT -> beginRootStart()
            null -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        projectionManager = getSystemService(MediaProjectionManager::class.java)
            ?: error("MediaProjectionManager is unavailable")
        pendingStart = savedInstanceState
            ?.getString(KEY_PENDING_START)
            ?.let { name -> runCatching { PendingStart.valueOf(name) }.getOrNull() }

        if (savedInstanceState == null && intent?.getBooleanExtra(EXTRA_AUTO_START, false) == true) {
            autoStartToken = 1
        }

        val repository = settingsRepository
        val library = recordingLibrary
        setContent {
            val settings by repository.settings.collectAsState()
            val recordingState by RecordingStateStore.state.collectAsState()
            val lastRecording by library.lastRecording.collectAsState()

            RecorderTheme(theme = settings.theme) {
                RecorderApp(
                    settings = settings,
                    recordingState = recordingState,
                    lastRecording = lastRecording,
                    snackbarHostState = snackbarHostState,
                    checkingRoot = checkingRoot,
                    autoStartToken = autoStartToken,
                    showMicPermissionDialog = showMicPermissionDialog,
                    showRootUnavailableDialog = showRootUnavailableDialog,
                    onSettingsChange = repository::replace,
                    onStart = ::beginStart,
                    onStop = ::stopRecording,
                    onDismissMicPermissionDialog = { showMicPermissionDialog = false },
                    onOpenAppSettings = ::openAppSettings,
                    onDismissRootUnavailableDialog = { showRootUnavailableDialog = false },
                    onOpenRecording = ::openRecording,
                    onShareRecording = ::shareRecording,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTO_START, false)) {
            autoStartToken++
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING_START, pendingStart?.name)
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun beginStart() {
        if (RecordingStateStore.state.value.isActive) return
        val settings = settingsRepository.settings.value

        // Root mode can only use the microphone. Downgrade a persisted internal
        // audio choice instead of failing on a configuration the UI disallows.
        if (settings.captureMode == CaptureMode.ROOT && settings.audioSource.usesDeviceAudio) {
            settingsRepository.setAudioSource(AudioSource.MICROPHONE)
        }

        val permissions = mutableListOf<String>()
        val needsAudio = settings.audioSource.requiresAudioPermission
        if (needsAudio && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
            permissions += Manifest.permission.RECORD_AUDIO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS) &&
            !notificationPermissionAsked()
        ) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
            markNotificationPermissionAsked()
        }

        if (permissions.isNotEmpty()) {
            pendingStart = if (settings.captureMode == CaptureMode.ROOT) {
                PendingStart.ROOT
            } else {
                PendingStart.STANDARD
            }
            permissionsLauncher.launch(permissions.toTypedArray())
            return
        }

        if (settings.captureMode == CaptureMode.ROOT) {
            beginRootStart()
        } else {
            requestProjection()
        }
    }

    private fun beginRootStart() {
        if (checkingRoot) return
        checkingRoot = true
        activityScope.launch {
            val status = RootChecker.check(this@MainActivity, force = false)
            checkingRoot = false
            if (status == RootStatus.Available) {
                startRootService()
            } else {
                showRootUnavailableDialog = true
            }
        }
    }

    private fun requestProjection() {
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay(),
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        try {
            projectionLauncher.launch(captureIntent)
        } catch (_: ActivityNotFoundException) {
            showMessage(getString(R.string.projection_unavailable))
        } catch (_: SecurityException) {
            showMessage(getString(R.string.projection_unavailable))
        }
    }

    private fun startStandardService(resultCode: Int, data: Intent) {
        val intent = Intent(this, RecordingService::class.java)
            .setAction(RecordingService.ACTION_START)
            .putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(RecordingService.EXTRA_RESULT_DATA, data)
        runCatching {
            ContextCompat.startForegroundService(this, intent)
        }.onFailure {
            showMessage(getString(R.string.projection_unavailable))
        }
    }

    private fun startRootService() {
        val intent = Intent(this, RecordingService::class.java)
            .setAction(RecordingService.ACTION_START)
        runCatching {
            ContextCompat.startForegroundService(this, intent)
        }.onFailure {
            showMessage(it.message ?: getString(R.string.error_root_unavailable))
        }
    }

    private fun stopRecording() {
        if (!RecordingStateStore.state.value.isActive) return
        runCatching {
            startService(
                Intent(this, RecordingService::class.java)
                    .setAction(RecordingService.ACTION_STOP),
            )
        }
    }

    private fun openAppSettings() {
        showMicPermissionDialog = false
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    private fun openRecording(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage(getString(R.string.error_open_recording))
        } catch (_: SecurityException) {
            showMessage(getString(R.string.error_open_recording))
        }
    }

    private fun shareRecording(uri: Uri) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("video/mp4")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(send, getString(R.string.action_share)))
        } catch (_: ActivityNotFoundException) {
            showMessage(getString(R.string.error_share_recording))
        } catch (_: SecurityException) {
            showMessage(getString(R.string.error_share_recording))
        }
    }

    private fun showMessage(message: String) {
        activityScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun notificationPermissionAsked(): Boolean =
        getSharedPreferences(UI_PREFS, MODE_PRIVATE).getBoolean(KEY_NOTIFICATION_ASKED, false)

    private fun markNotificationPermissionAsked() {
        getSharedPreferences(UI_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATION_ASKED, true)
            .apply()
    }

    companion object {
        const val EXTRA_AUTO_START = "com.hihi.recorder.extra.AUTO_START"
        private const val UI_PREFS = "miuix_recorder_ui"
        private const val KEY_NOTIFICATION_ASKED = "notification_asked"
        private const val KEY_PENDING_START = "pending_start"
    }
}
