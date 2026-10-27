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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.hihi.recorder.data.CaptureMode
import com.hihi.recorder.data.RecordingStateStore
import com.hihi.recorder.data.SavedRecording
import com.hihi.recorder.data.ShellBackend
import com.hihi.recorder.service.RecordingService
import com.hihi.recorder.shell.AdbDaemon
import com.hihi.recorder.shell.ProjectMediaAppOp
import com.hihi.recorder.shell.ShellChecker
import com.hihi.recorder.shell.ShellFailure
import com.hihi.recorder.shell.ShellStatus
import com.hihi.recorder.ui.RecorderApp
import com.hihi.recorder.ui.RecorderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.SnackbarHostState

class MainActivity : ComponentActivity() {

    private enum class PendingStart { STANDARD, SHELL }

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val snackbarHostState = SnackbarHostState()
    private lateinit var projectionManager: MediaProjectionManager
    private var pendingStart: PendingStart? = null
    private var projectionTarget: PendingStart? = null

    private var autoStartToken by mutableIntStateOf(0)
    private var checkingShell by mutableStateOf(false)
    private var showMicPermissionDialog by mutableStateOf(false)
    private var showShellUnavailableDialog by mutableStateOf(false)
    private var shellFailure by mutableStateOf<ShellFailure?>(null)

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != SHIZUKU_PERMISSION_REQUEST) return@OnRequestPermissionResultListener
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                beginShellStart()
            } else {
                checkingShell = false
                shellFailure = ShellFailure.SHIZUKU_NO_PERMISSION
                showShellUnavailableDialog = true
            }
        }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val target = projectionTarget ?: PendingStart.STANDARD
        projectionTarget = null
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            when (target) {
                PendingStart.STANDARD -> startStandardService(result.resultCode, data)
                PendingStart.SHELL -> startShellService(result.resultCode, data)
            }
        } else {
            if (target == PendingStart.SHELL) {
                // The app-op was granted before the prompt; undo it because the
                // service will never start a projection session.
                ProjectMediaAppOp.restoreDetached(this)
            }
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
            PendingStart.STANDARD -> requestProjection(PendingStart.STANDARD)
            PendingStart.SHELL -> beginShellStart()
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
        projectionTarget = savedInstanceState
            ?.getString(KEY_PROJECTION_TARGET)
            ?.let { name -> runCatching { PendingStart.valueOf(name) }.getOrNull() }

        runCatching { Shizuku.addRequestPermissionResultListener(shizukuPermissionListener) }
        // A crash or a process kill can leave project_media allowed. Restore it
        // once the UI is alive, but not while a live session or a shell start
        // waiting for the projection consent still relies on the grant.
        val shellProjectionPending = projectionTarget == PendingStart.SHELL
        if (!shellProjectionPending && !RecordingStateStore.state.value.isActive) {
            ProjectMediaAppOp.restoreDetached(this)
        }
        runCatching { AdbDaemon.ensureScripts(this) }

        if (savedInstanceState == null && intent?.getBooleanExtra(EXTRA_AUTO_START, false) == true) {
            autoStartToken = 1
        }

        val repository = settingsRepository
        val library = recordingLibrary
        setContent {
            val settings by repository.settings.collectAsState()
            val recordingState by RecordingStateStore.state.collectAsState()
            val lastRecording by library.lastRecording.collectAsState()
            val recordings by library.recordings.collectAsState()
            val recordingsLoading by library.isLoading.collectAsState()

            RecorderTheme(settings = settings) {
                RecorderApp(
                    settings = settings,
                    recordingState = recordingState,
                    lastRecording = lastRecording,
                    recordings = recordings,
                    recordingsLoading = recordingsLoading,
                    snackbarHostState = snackbarHostState,
                    checkingShell = checkingShell,
                    autoStartToken = autoStartToken,
                    showMicPermissionDialog = showMicPermissionDialog,
                    showShellUnavailableDialog = showShellUnavailableDialog,
                    shellFailure = shellFailure,
                    onSettingsChange = repository::replace,
                    onStart = ::beginStart,
                    onStop = ::stopRecording,
                    onDismissMicPermissionDialog = { showMicPermissionDialog = false },
                    onOpenAppSettings = ::openAppSettings,
                    onDismissShellUnavailableDialog = { showShellUnavailableDialog = false },
                    onOpenRecording = ::openRecording,
                    onShareRecording = ::shareRecording,
                    onRefreshRecordings = ::refreshRecordings,
                    onRenameRecording = ::renameRecording,
                    onDeleteRecording = ::deleteRecording,
                    onOpenUrl = ::openUrl,
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
        outState.putString(KEY_PROJECTION_TARGET, projectionTarget?.name)
    }

    override fun onDestroy() {
        runCatching {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        }
        activityScope.cancel()
        super.onDestroy()
    }

    private fun beginStart() {
        if (RecordingStateStore.state.value.isActive) return
        val settings = settingsRepository.settings.value

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
            pendingStart = if (settings.captureMode == CaptureMode.SHELL) {
                PendingStart.SHELL
            } else {
                PendingStart.STANDARD
            }
            permissionsLauncher.launch(permissions.toTypedArray())
            return
        }

        if (settings.captureMode == CaptureMode.SHELL) {
            // Check the activation method before asking for playback-capture
            // consent: an unavailable backend should show its own instructions
            // instead of a projection prompt.
            beginShellStart()
        } else {
            requestProjection(PendingStart.STANDARD)
        }
    }

    private fun beginShellStart() {
        if (checkingShell) return
        val settings = settingsRepository.settings.value
        if (settings.captureMode != CaptureMode.SHELL) {
            requestProjection(PendingStart.STANDARD)
            return
        }

        checkingShell = true
        activityScope.launch {
            // A previous session may still be restoring project_media through
            // a Shizuku user service. Finish that before binding a new one.
            ProjectMediaAppOp.awaitPendingWork()
            val backend = settings.shellBackend
            val status = ShellChecker.check(this@MainActivity, backend, force = true)
            if (status is ShellStatus.Unavailable &&
                backend == ShellBackend.SHIZUKU &&
                status.reason == ShellFailure.SHIZUKU_NO_PERMISSION
            ) {
                checkingShell = false
                runCatching {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST)
                }.onFailure {
                    shellFailure = ShellFailure.SHIZUKU_NO_PERMISSION
                    showShellUnavailableDialog = true
                }
                return@launch
            }
            if (status !is ShellStatus.Available) {
                checkingShell = false
                shellFailure = (status as? ShellStatus.Unavailable)?.reason
                    ?: ShellFailure.BACKEND_ERROR
                showShellUnavailableDialog = true
                return@launch
            }

            val needsProjection = settings.audioSource.usesDeviceAudio
            if (needsProjection) {
                val granted = ProjectMediaAppOp(this@MainActivity).grant(backend)
                if (!granted) {
                    showMessage(getString(R.string.warning_appop_grant_failed))
                }
            }
            checkingShell = false
            if (needsProjection) {
                // Shell video needs no consent, but phone audio still goes
                // through MediaProjection's playback-capture API.
                requestProjection(PendingStart.SHELL)
            } else {
                startShellService()
            }
        }
    }

    private fun requestProjection(target: PendingStart) {
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay(),
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        projectionTarget = target
        try {
            projectionLauncher.launch(captureIntent)
        } catch (_: ActivityNotFoundException) {
            projectionTarget = null
            abandonShellProjection(target)
            showMessage(getString(R.string.projection_unavailable))
        } catch (_: SecurityException) {
            projectionTarget = null
            abandonShellProjection(target)
            showMessage(getString(R.string.projection_unavailable))
        }
    }

    private fun abandonShellProjection(target: PendingStart) {
        if (target == PendingStart.SHELL) {
            ProjectMediaAppOp.restoreDetached(this)
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

    private fun startShellService(
        resultCode: Int = Activity.RESULT_CANCELED,
        data: Intent? = null,
    ) {
        val intent = Intent(this, RecordingService::class.java)
            .setAction(RecordingService.ACTION_START)
        if (resultCode == Activity.RESULT_OK && data != null) {
            intent
                .putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(RecordingService.EXTRA_RESULT_DATA, data)
        }
        runCatching {
            ContextCompat.startForegroundService(this, intent)
        }.onFailure {
            ProjectMediaAppOp.restoreDetached(this)
            showMessage(it.message ?: getString(R.string.error_shell_unavailable))
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

    private fun refreshRecordings() {
        activityScope.launch {
            recordingLibrary.refresh().onFailure {
                snackbarHostState.showSnackbar(getString(R.string.error_refresh_videos))
            }
        }
    }

    private fun renameRecording(recording: SavedRecording, newName: String) {
        activityScope.launch {
            recordingLibrary.rename(recording, newName)
                .onSuccess {
                    snackbarHostState.showSnackbar(getString(R.string.rename_success))
                }
                .onFailure {
                    snackbarHostState.showSnackbar(getString(R.string.error_rename_video))
                }
        }
    }

    private fun deleteRecording(recording: SavedRecording) {
        activityScope.launch {
            recordingLibrary.delete(recording)
                .onSuccess {
                    snackbarHostState.showSnackbar(getString(R.string.delete_success))
                }
                .onFailure {
                    snackbarHostState.showSnackbar(getString(R.string.error_delete_video))
                }
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage(getString(R.string.error_open_link))
        } catch (_: SecurityException) {
            showMessage(getString(R.string.error_open_link))
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
        private const val SHIZUKU_PERMISSION_REQUEST = 4102
        private const val UI_PREFS = "miuix_recorder_ui"
        private const val KEY_NOTIFICATION_ASKED = "notification_asked"
        private const val KEY_PENDING_START = "pending_start"
        private const val KEY_PROJECTION_TARGET = "projection_target"
    }
}
