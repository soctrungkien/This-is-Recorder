package com.hihi.recorder.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecordingPhase {
    IDLE,
    PREPARING,
    RECORDING,
    STOPPING,
    SAVED,
    ERROR,
}

data class RecordingState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val startedAtElapsedRealtime: Long = 0L,
    val countdownSeconds: Int = 0,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
) {
    val isActive: Boolean
        get() = phase == RecordingPhase.PREPARING ||
            phase == RecordingPhase.RECORDING ||
            phase == RecordingPhase.STOPPING
}

/**
 * Process-wide recording state. It is written by the foreground service and
 * observed by the activity and the Quick Settings tile.
 */
object RecordingStateStore {
    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun markPreparing(countdownSeconds: Int = 0) {
        _state.value = RecordingState(
            phase = RecordingPhase.PREPARING,
            countdownSeconds = countdownSeconds,
        )
    }

    fun markRecording(startedAtElapsedRealtime: Long, warningMessage: String? = null) {
        _state.value = RecordingState(
            phase = RecordingPhase.RECORDING,
            startedAtElapsedRealtime = startedAtElapsedRealtime,
            warningMessage = warningMessage,
        )
    }

    fun markStopping() {
        val current = _state.value
        _state.value = current.copy(
            phase = RecordingPhase.STOPPING,
            countdownSeconds = 0,
        )
    }

    fun markWarning(message: String) {
        _state.value = _state.value.copy(warningMessage = message)
    }

    fun markIdle() {
        _state.value = RecordingState()
    }

    fun markSaved() {
        _state.value = _state.value.copy(
            phase = RecordingPhase.SAVED,
            countdownSeconds = 0,
            errorMessage = null,
        )
    }

    fun markError(message: String) {
        _state.value = RecordingState(
            phase = RecordingPhase.ERROR,
            errorMessage = message,
        )
    }
}
