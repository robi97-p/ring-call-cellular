package com.ringcall.cellular.device

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.TargetState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RingerControllerTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val controller = RingerController(ctx)

    @Test fun `applies each mode`() {
        shadowOf(ctx.getSystemService(NotificationManager::class.java)).setNotificationPolicyAccessGranted(true)
        assertEquals(ApplyResult.APPLIED, controller.apply(TargetState(RingMode.VIBRATE)))
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
        controller.apply(TargetState(RingMode.SILENT))
        assertEquals(RingMode.SILENT, controller.currentMode())
        controller.apply(TargetState(RingMode.NORMAL))
        assertEquals(AudioManager.RINGER_MODE_NORMAL, audio.ringerMode)
    }

    @Test fun `applies ring volume as a fraction of the max`() {
        controller.apply(TargetState(RingMode.NORMAL, 50))
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        assertEquals(RingerController.percentToIndex(50, max), audio.getStreamVolume(AudioManager.STREAM_RING))
    }

    @Test fun `percent conversion never mutes in normal mode`() {
        assertEquals(1, RingerController.percentToIndex(0, 7))
        assertEquals(4, RingerController.percentToIndex(50, 7))
        assertEquals(7, RingerController.percentToIndex(100, 7))
        assertEquals(8, RingerController.percentToIndex(50, 15))
        assertEquals(15, RingerController.percentToIndex(150, 15))
    }
}
