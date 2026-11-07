package com.ringcall.cellular.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ringcall.cellular.RingCallApp

/** Fires at each scheduled transition. */
class TransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as RingCallApp).scheduler.refresh()
    }
}

/** Re-arms after reboot, app update, clock/timezone change or exact-alarm permission change. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as RingCallApp).scheduler.refresh()
    }
}
