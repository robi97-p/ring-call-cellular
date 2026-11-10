package com.ringcall.cellular;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/** Decides whether quiet hours are active, changes the ringer accordingly and schedules the next change. */
public final class RingerController {
    private static final String TAG = "RingCall";

    private RingerController() {}

    public static boolean isInQuietHours(Prefs p) {
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int start = p.getStartMinutes();
        int end = p.getEndMinutes();
        if (start == end) return false;
        if (start < end) return now >= start && now < end;
        return now >= start || now < end; // window crosses midnight
    }

    public static boolean shouldBeMuted(Context ctx) {
        Prefs p = Prefs.get(ctx);
        return p.isEnabled() && isInQuietHours(p);
    }

    public static boolean hasDndAccess(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    /** Brings the ringer to the state it should have right now. */
    public static synchronized void apply(Context ctx) {
        Prefs p = Prefs.get(ctx);
        if (p.isBypassActive()) {
            // An allowed caller is on the line - don't touch the ringer until the call ends.
            scheduleNext(ctx);
            return;
        }
        AudioManager am = ctx.getSystemService(AudioManager.class);
        if (shouldBeMuted(ctx)) {
            if (!p.isMutedByUs()) {
                p.setSavedRingerMode(am.getRingerMode());
                p.setMutedByUs(true);
            }
            setRinger(ctx, Prefs.MODE_VIBRATE.equals(p.getMode())
                    ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_SILENT);
        } else if (p.isMutedByUs()) {
            setRinger(ctx, p.getSavedRingerMode());
            p.setMutedByUs(false);
        }
        scheduleNext(ctx);
    }

    public static void setRinger(Context ctx, int mode) {
        AudioManager am = ctx.getSystemService(AudioManager.class);
        try {
            if (am.getRingerMode() != mode) am.setRingerMode(mode);
        } catch (SecurityException e) {
            Log.w(TAG, "Missing Do-Not-Disturb access, cannot change ringer", e);
        }
    }

    /** Arms an alarm at the next start/end boundary of the quiet hours. */
    public static void scheduleNext(Context ctx) {
        AlarmManager alarm = ctx.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1,
                new Intent(ctx, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarm.cancel(pi);
        Prefs p = Prefs.get(ctx);
        if (!p.isEnabled()) return;

        long at = Math.min(nextOccurrence(p.getStartMinutes()), nextOccurrence(p.getEndMinutes()));
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms();
        if (exact) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static long nextOccurrence(int minutesOfDay) {
        Calendar c = Calendar.getInstance();
        Calendar t = (Calendar) c.clone();
        t.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60);
        t.set(Calendar.MINUTE, minutesOfDay % 60);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (!t.after(c)) t.add(Calendar.DAY_OF_MONTH, 1);
        return t.getTimeInMillis();
    }

    public static String formatTime(int minutes) {
        return String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }
}
