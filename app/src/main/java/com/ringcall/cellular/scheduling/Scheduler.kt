package com.ringcall.cellular.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ringcall.cellular.data.RuleStore
import com.ringcall.cellular.device.ApplyResult
import com.ringcall.cellular.device.RingerController
import com.ringcall.cellular.domain.ScheduleCalculator
import com.ringcall.cellular.domain.Transition
import java.time.Clock
import java.time.ZonedDateTime

/** Applies the current state and keeps exactly one alarm armed for the next transition. */
class Scheduler(
    private val context: Context,
    private val store: RuleStore,
    private val ringer: RingerController,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun nextTransition(): Transition? =
        ScheduleCalculator.nextTransition(store.rules.value, ZonedDateTime.now(clock), store.defaultState)

    /** Applies the state that should be active right now. */
    fun applyNow(): ApplyResult {
        val state = ScheduleCalculator.targetStateAt(store.rules.value, ZonedDateTime.now(clock), store.defaultState)
        return ringer.apply(state)
    }

    fun scheduleNext(): Transition? {
        val pending = pendingIntent()
        alarms.cancel(pending)
        val next = nextTransition() ?: return null
        val at = next.at.toInstant().toEpochMilli()
        if (canScheduleExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
        return next
    }

    /** Called after rules change: apply immediately and re-arm. */
    fun refresh(): ApplyResult {
        val result = applyNow()
        scheduleNext()
        return result
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TransitionReceiver::class.java).setAction(ACTION_TRANSITION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_TRANSITION = "com.ringcall.cellular.TRANSITION"
    }
}
