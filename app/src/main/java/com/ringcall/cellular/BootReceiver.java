package com.ringcall.cellular;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores the schedule, the red dot and the allowed-caller listener after reboot / app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs.get(context).setBypassActive(false);
        RingerController.apply(context);
        if (Prefs.get(context).isEnabled()) RingControlService.start(context);
    }
}
