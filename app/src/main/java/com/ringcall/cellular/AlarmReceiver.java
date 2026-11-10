package com.ringcall.cellular;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires at the start / end of quiet hours. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RingerController.apply(context);
        RingControlService.refresh(context);
    }
}
