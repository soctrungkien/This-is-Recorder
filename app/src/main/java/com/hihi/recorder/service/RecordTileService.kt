package com.hihi.recorder.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hihi.recorder.MainActivity
import com.hihi.recorder.R
import com.hihi.recorder.data.RecordingPhase
import com.hihi.recorder.data.RecordingState
import com.hihi.recorder.data.RecordingStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Quick Settings tile. Active sessions are stopped directly; otherwise the
 * activity is opened so it can request runtime permissions and, in standard
 * mode, the one-shot MediaProjection consent.
 */
class RecordTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            RecordingStateStore.state.collect { state -> refresh(state) }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        refresh(RecordingStateStore.state.value)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        requestListeningState(this, ComponentName(this, RecordTileService::class.java))
    }

    // The Intent overload is only reached below Android 14; newer systems use
    // the PendingIntent overload above. Lint cannot see through that guard.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val state = RecordingStateStore.state.value
        if (state.isActive) {
            startService(
                Intent(this, RecordingService::class.java)
                    .setAction(RecordingService.ACTION_STOP),
            )
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_AUTO_START, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun refresh(state: RecordingState) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_recorder)
        tile.state = if (state.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = when (state.phase) {
            RecordingPhase.PREPARING -> getString(R.string.status_preparing_title)
            RecordingPhase.RECORDING -> getString(R.string.status_recording_title)
            RecordingPhase.STOPPING -> getString(R.string.status_stopping_title)
            RecordingPhase.SAVED -> getString(R.string.status_saved_title)
            RecordingPhase.ERROR -> getString(R.string.status_error_title)
            RecordingPhase.IDLE -> getString(R.string.status_idle_title)
        }
        tile.updateTile()
    }
}
