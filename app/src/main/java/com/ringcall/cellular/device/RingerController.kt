package com.ringcall.cellular.device

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.TargetState
import kotlin.math.roundToInt

enum class ApplyResult { APPLIED, MISSING_PERMISSION, FAILED }

/** Applies a [TargetState] to the device ringer. */
class RingerController(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    /** Do Not Disturb access is needed to switch into/out of silent mode. */
    fun hasPolicyAccess(): Boolean = notifications.isNotificationPolicyAccessGranted

    fun currentMode(): RingMode = when (audio.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingMode.SILENT
        AudioManager.RINGER_MODE_VIBRATE -> RingMode.VIBRATE
        else -> RingMode.NORMAL
    }

    fun apply(state: TargetState): ApplyResult = try {
        audio.ringerMode = when (state.mode) {
            RingMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            RingMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            RingMode.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
        val percent = state.ringVolumePercent
        if (state.mode == RingMode.NORMAL && percent != null) {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
            audio.setStreamVolume(AudioManager.STREAM_RING, percentToIndex(percent, max), 0)
        }
        ApplyResult.APPLIED
    } catch (e: SecurityException) {
        ApplyResult.MISSING_PERMISSION
    } catch (e: RuntimeException) {
        ApplyResult.FAILED
    }

    companion object {
        /** Converts 0..100 to a stream index; never 0 in normal mode, so we don't fall into vibrate. */
        fun percentToIndex(percent: Int, max: Int): Int =
            (percent.coerceIn(0, 100) * max / 100.0).roundToInt().coerceIn(1, max)
    }
}
