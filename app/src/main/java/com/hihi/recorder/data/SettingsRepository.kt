package com.hihi.recorder.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists [RecorderSettings] and exposes them as a Compose-friendly flow. */
class SettingsRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<RecorderSettings> = _settings.asStateFlow()

    @Synchronized
    fun replace(settings: RecorderSettings) = update {
        settings.copy(saveFolder = normalizeSaveFolder(settings.saveFolder))
    }

    @Synchronized
    fun setCaptureMode(value: CaptureMode) = update { it.copy(captureMode = value) }

    @Synchronized
    fun setAudioSource(value: AudioSource) = update { it.copy(audioSource = value) }

    @Synchronized
    fun setResolution(value: VideoResolution) = update { it.copy(resolution = value) }

    @Synchronized
    fun setFrameRate(value: VideoFrameRate) = update { it.copy(frameRate = value) }

    @Synchronized
    fun setBitrate(value: VideoBitrate) = update { it.copy(bitrate = value) }

    @Synchronized
    fun setCountdown(value: Countdown) = update { it.copy(countdown = value) }

    @Synchronized
    fun setAutoStop(value: AutoStop) = update { it.copy(autoStop = value) }

    @Synchronized
    fun setHapticFeedback(value: Boolean) = update { it.copy(hapticFeedback = value) }

    @Synchronized
    fun setStopOnScreenOff(value: Boolean) = update { it.copy(stopOnScreenOff = value) }

    @Synchronized
    fun setTheme(value: RecorderTheme) = update { it.copy(theme = value) }

    @Synchronized
    fun setUiStyle(value: UiStyle) = update { it.copy(uiStyle = value) }

    @Synchronized
    fun setMaterialSpec(value: MaterialSpec) = update { it.copy(materialSpec = value) }

    @Synchronized
    fun setMonetColor(value: Boolean) = update { it.copy(monetColor = value) }

    @Synchronized
    fun setNavigationBarStyle(value: NavigationBarStyle) = update {
        it.copy(navigationBarStyle = value)
    }

    @Synchronized
    fun setNavigationIosLabels(value: Boolean) = update { it.copy(navigationIosLabels = value) }

    @Synchronized
    fun setSaveFolder(value: String) = update { it.copy(saveFolder = normalizeSaveFolder(value)) }

    /**
     * Android 13+ notification permission is optional. Remember that we asked
     * once so the permission dialog does not appear before every recording.
     */
    fun hasRequestedNotificationPermission(): Boolean =
        preferences.getBoolean(KEY_NOTIFICATION_ASKED, false)

    fun markNotificationPermissionRequested() {
        preferences.edit().putBoolean(KEY_NOTIFICATION_ASKED, true).apply()
    }

    @Synchronized
    private fun update(transform: (RecorderSettings) -> RecorderSettings) {
        val updated = transform(_settings.value)
        writeSettings(updated)
        _settings.value = updated
    }

    private fun readSettings(): RecorderSettings {
        val defaults = RecorderSettings()
        return RecorderSettings(
            captureMode = preferences.enumValue(KEY_CAPTURE_MODE, defaults.captureMode),
            audioSource = preferences.enumValue(KEY_AUDIO_SOURCE, defaults.audioSource),
            resolution = preferences.enumValue(KEY_RESOLUTION, defaults.resolution),
            frameRate = preferences.enumValue(KEY_FRAME_RATE, defaults.frameRate),
            bitrate = preferences.enumValue(KEY_BITRATE, defaults.bitrate),
            countdown = preferences.enumValue(KEY_COUNTDOWN, defaults.countdown),
            autoStop = preferences.enumValue(KEY_AUTO_STOP, defaults.autoStop),
            hapticFeedback = preferences.getBoolean(KEY_HAPTIC, defaults.hapticFeedback),
            stopOnScreenOff = preferences.getBoolean(KEY_STOP_ON_SCREEN_OFF, defaults.stopOnScreenOff),
            theme = preferences.enumValue(KEY_THEME, defaults.theme),
            uiStyle = preferences.enumValue(KEY_UI_STYLE, defaults.uiStyle),
            materialSpec = preferences.enumValue(KEY_MATERIAL_SPEC, defaults.materialSpec),
            monetColor = preferences.getBoolean(KEY_MONET, defaults.monetColor),
            navigationBarStyle = preferences.enumValue(
                KEY_NAVIGATION_BAR_STYLE,
                defaults.navigationBarStyle,
            ),
            navigationIosLabels = preferences.getBoolean(
                KEY_NAVIGATION_IOS_LABELS,
                defaults.navigationIosLabels,
            ),
            saveFolder = preferences.getString(KEY_SAVE_FOLDER, defaults.saveFolder)
                ?.let(::normalizeSaveFolder)
                ?: defaults.saveFolder,
        )
    }

    private fun writeSettings(settings: RecorderSettings) {
        preferences.edit()
            .putString(KEY_CAPTURE_MODE, settings.captureMode.name)
            .putString(KEY_AUDIO_SOURCE, settings.audioSource.name)
            .putString(KEY_RESOLUTION, settings.resolution.name)
            .putString(KEY_FRAME_RATE, settings.frameRate.name)
            .putString(KEY_BITRATE, settings.bitrate.name)
            .putString(KEY_COUNTDOWN, settings.countdown.name)
            .putString(KEY_AUTO_STOP, settings.autoStop.name)
            .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
            .putBoolean(KEY_STOP_ON_SCREEN_OFF, settings.stopOnScreenOff)
            .putString(KEY_THEME, settings.theme.name)
            .putString(KEY_UI_STYLE, settings.uiStyle.name)
            .putString(KEY_MATERIAL_SPEC, settings.materialSpec.name)
            .putBoolean(KEY_MONET, settings.monetColor)
            .putString(KEY_NAVIGATION_BAR_STYLE, settings.navigationBarStyle.name)
            .putBoolean(KEY_NAVIGATION_IOS_LABELS, settings.navigationIosLabels)
            .putString(KEY_SAVE_FOLDER, normalizeSaveFolder(settings.saveFolder))
            .apply()
    }

    private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enumValue(
        key: String,
        fallback: T,
    ): T {
        val raw = getString(key, null) ?: return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    private companion object {
        const val PREFS_NAME = "miuix_recorder_settings"
        const val KEY_CAPTURE_MODE = "capture_mode"
        const val KEY_AUDIO_SOURCE = "audio_source"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_FRAME_RATE = "frame_rate"
        const val KEY_BITRATE = "bitrate"
        const val KEY_COUNTDOWN = "countdown"
        const val KEY_AUTO_STOP = "auto_stop"
        const val KEY_HAPTIC = "haptic_feedback"
        const val KEY_STOP_ON_SCREEN_OFF = "stop_on_screen_off"
        const val KEY_THEME = "theme"
        const val KEY_UI_STYLE = "ui_style"
        const val KEY_MATERIAL_SPEC = "material_spec"
        const val KEY_MONET = "monet_color"
        const val KEY_NAVIGATION_BAR_STYLE = "navigation_bar_style"
        const val KEY_NAVIGATION_IOS_LABELS = "navigation_ios_labels"
        const val KEY_SAVE_FOLDER = "save_folder"
        const val KEY_NOTIFICATION_ASKED = "notification_permission_asked"
    }
}
