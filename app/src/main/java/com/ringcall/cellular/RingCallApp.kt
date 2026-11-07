package com.ringcall.cellular

import android.app.Application
import com.ringcall.cellular.data.RuleStore
import com.ringcall.cellular.device.RingerController
import com.ringcall.cellular.scheduling.Scheduler

class RingCallApp : Application() {
    val store by lazy { RuleStore(this) }
    val ringer by lazy { RingerController(this) }
    val scheduler by lazy { Scheduler(this, store, ringer) }
}
