package com.hihi.recorder.data

import android.content.Context
import com.zeroui.kit.components.SettingIconStyle
import com.zeroui.kit.theme.GlassMode
import com.zeroui.kit.theme.MotionMode
import com.zeroui.kit.theme.PaletteStyle
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
    fun setShellBackend(value: ShellBackend) = update { it.copy(shellBackend = value) }

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
    fun setNavigationBlur(value: Boolean) = update { it.copy(navigationBlur = value) }

    @Synchronized
    fun setNavigationIosLabels(value: Boolean) = update { it.copy(navigationIosLabels = value) }

    @Synchronized
    fun setSaveFolder(value: String) = update { it.copy(saveFolder = normalizeSaveFolder(value)) }

    // ZeroUI theme / interface preferences.
    @Synchronized
    fun setSeedColor(value: Int) = update { it.copy(seedColor = value) }

    @Synchronized
    fun setPalette(value: PaletteStyle) = update { it.copy(palette = value) }

    @Synchronized
    fun setPureBlack(value: Boolean) = update { it.copy(pureBlack = value) }

    @Synchronized
    fun setMotion(value: MotionMode) = update { it.copy(motion = value) }

    @Synchronized
    fun setUiScale(value: Int) = update { it.copy(uiScale = value.coerceIn(80, 130)) }

    @Synchronized
    fun setGlassMode(value: GlassMode) = update { it.copy(glassMode = value) }

    @Synchronized
    fun setGlassIntensity(value: Int) = update { it.copy(glassIntensity = value.coerceIn(25, 200)) }

    @Synchronized
    fun setCompactList(value: Boolean) = update { it.copy(compactList = value) }

    @Synchronized
    fun setTopBarBlur(value: Boolean) = update { it.copy(topBarBlur = value) }

    @Synchronized
    fun setSmoothCorners(value: Boolean) = update { it.copy(smoothCorners = value) }

    @Synchronized
    fun setElasticScroll(value: Boolean) = update { it.copy(elasticScroll = value) }

    @Synchronized
    fun setDialogCentered(value: Boolean) = update { it.copy(dialogCentered = value) }

    @Synchronized
    fun setIconStyle(value: SettingIconStyle) = update { it.copy(iconStyle = value) }

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
            captureMode = preferences.captureModeValue(KEY_CAPTURE_MODE, defaults.captureMode),
            shellBackend = preferences.enumValue(KEY_SHELL_BACKEND, defaults.shellBackend),
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
            navigationBlur = preferences.getBoolean(
                KEY_NAVIGATION_BLUR,
                defaults.navigationBlur,
            ),
            navigationIosLabels = preferences.getBoolean(
                KEY_NAVIGATION_IOS_LABELS,
                defaults.navigationIosLabels,
            ),
            saveFolder = preferences.getString(KEY_SAVE_FOLDER, defaults.saveFolder)
                ?.let(::normalizeSaveFolder)
                ?: defaults.saveFolder,
            seedColor = preferences.getInt(KEY_SEED_COLOR, defaults.seedColor),
            palette = preferences.enumValue(KEY_PALETTE, defaults.palette),
            pureBlack = preferences.getBoolean(KEY_PURE_BLACK, defaults.pureBlack),
            motion = preferences.enumValue(KEY_MOTION, defaults.motion),
            uiScale = preferences.getInt(KEY_UI_SCALE, defaults.uiScale),
            glassMode = preferences.enumValue(KEY_GLASS_MODE, defaults.glassMode),
            glassIntensity = preferences.getInt(KEY_GLASS_INTENSITY, defaults.glassIntensity),
            compactList = preferences.getBoolean(KEY_COMPACT_LIST, defaults.compactList),
            topBarBlur = preferences.getBoolean(KEY_TOP_BAR_BLUR, defaults.topBarBlur),
            smoothCorners = preferences.getBoolean(KEY_SMOOTH_CORNERS, defaults.smoothCorners),
            elasticScroll = preferences.getBoolean(KEY_ELASTIC_SCROLL, defaults.elasticScroll),
            dialogCentered = preferences.getBoolean(KEY_DIALOG_CENTERED, defaults.dialogCentered),
            iconStyle = preferences.enumValue(KEY_ICON_STYLE, defaults.iconStyle),
        )
    }

    private fun writeSettings(settings: RecorderSettings) {
        preferences.edit()
            .putString(KEY_CAPTURE_MODE, settings.captureMode.name)
            .putString(KEY_SHELL_BACKEND, settings.shellBackend.name)
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
            .putBoolean(KEY_NAVIGATION_BLUR, settings.navigationBlur)
            .putBoolean(KEY_NAVIGATION_IOS_LABELS, settings.navigationIosLabels)
            .putString(KEY_SAVE_FOLDER, normalizeSaveFolder(settings.saveFolder))
            .putInt(KEY_SEED_COLOR, settings.seedColor)
            .putString(KEY_PALETTE, settings.palette.name)
            .putBoolean(KEY_PURE_BLACK, settings.pureBlack)
            .putString(KEY_MOTION, settings.motion.name)
            .putInt(KEY_UI_SCALE, settings.uiScale)
            .putString(KEY_GLASS_MODE, settings.glassMode.name)
            .putInt(KEY_GLASS_INTENSITY, settings.glassIntensity)
            .putBoolean(KEY_COMPACT_LIST, settings.compactList)
            .putBoolean(KEY_TOP_BAR_BLUR, settings.topBarBlur)
            .putBoolean(KEY_SMOOTH_CORNERS, settings.smoothCorners)
            .putBoolean(KEY_ELASTIC_SCROLL, settings.elasticScroll)
            .putBoolean(KEY_DIALOG_CENTERED, settings.dialogCentered)
            .putString(KEY_ICON_STYLE, settings.iconStyle.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enumValue(
        key: String,
        fallback: T,
    ): T {
        val raw = getString(key, null) ?: return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    /**
     * Older builds stored `CaptureMode.ROOT`. Treat it as the new shell mode
     * with its original backend so an update does not silently change how the
     * screen is captured.
     */
    private fun android.content.SharedPreferences.captureModeValue(
        key: String,
        fallback: CaptureMode,
    ): CaptureMode {
        val raw = getString(key, null) ?: return fallback
        if (raw == "ROOT") return CaptureMode.SHELL
        return runCatching { CaptureMode.valueOf(raw) }.getOrDefault(fallback)
    }

    private companion object {
        const val PREFS_NAME = "miuix_recorder_settings"
        const val KEY_CAPTURE_MODE = "capture_mode"
        const val KEY_SHELL_BACKEND = "shell_backend"
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
        const val KEY_NAVIGATION_BLUR = "navigation_blur"
        const val KEY_NAVIGATION_IOS_LABELS = "navigation_ios_labels"
        const val KEY_SAVE_FOLDER = "save_folder"
        const val KEY_NOTIFICATION_ASKED = "notification_permission_asked"
        const val KEY_SEED_COLOR = "seed_color"
        const val KEY_PALETTE = "palette"
        const val KEY_PURE_BLACK = "pure_black"
        const val KEY_MOTION = "motion"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_GLASS_MODE = "glass_mode"
        const val KEY_GLASS_INTENSITY = "glass_intensity"
        const val KEY_COMPACT_LIST = "compact_list"
        const val KEY_TOP_BAR_BLUR = "top_bar_blur"
        const val KEY_SMOOTH_CORNERS = "smooth_corners"
        const val KEY_ELASTIC_SCROLL = "elastic_scroll"
        const val KEY_DIALOG_CENTERED = "dialog_centered"
        const val KEY_ICON_STYLE = "icon_style"
    }
}
