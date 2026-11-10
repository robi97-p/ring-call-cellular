package com.ringcall.cellular;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Runs while ringer control is enabled:
 *  - keeps a small red dot over the status bar (next to the Wi-Fi icon),
 *  - listens for incoming calls and lets allowed numbers ring normally even during quiet hours,
 *  - re-checks the schedule every minute as a safety net to the alarms.
 */
public class RingControlService extends Service {
    private static final String TAG = "RingCall";
    private static final String CHANNEL = "ring_control";
    private static final int NOTIF_ID = 1;
    private static final String ACTION_REFRESH = "com.ringcall.cellular.REFRESH";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View dotView;
    private Ringtone ringtone;
    private boolean receiverRegistered;

    public static void start(Context ctx) {
        ctx.startForegroundService(new Intent(ctx, RingControlService.class));
    }

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, RingControlService.class));
    }

    /** Updates notification + dot if the service is running (never starts it from the background). */
    public static void refresh(Context ctx) {
        if (!Prefs.get(ctx).isEnabled()) return;
        try {
            ctx.startService(new Intent(ctx, RingControlService.class).setAction(ACTION_REFRESH));
        } catch (IllegalStateException ignored) {
            // App is in the background and the service is not running - nothing to refresh.
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = getSystemService(WindowManager.class);
        createChannel();
        IntentFilter f = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(callReceiver, f);
        receiverRegistered = true;
        handler.post(tick);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
        if (!Prefs.get(this).isEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        updateDot();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) unregisterReceiver(callReceiver);
        stopRinging();
        removeDot();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            RingerController.apply(RingControlService.this);
            getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
            handler.postDelayed(this, 60_000);
        }
    };

    // ------------------------------------------------------------------ red dot

    private void updateDot() {
        Prefs p = Prefs.get(this);
        boolean want = p.isEnabled() && p.isDotEnabled() && Settings.canDrawOverlays(this);
        if (!want) {
            removeDot();
            return;
        }
        int size = dp(8);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.x = dp(p.getDotOffsetDp());
        lp.y = Math.max(0, (statusBarHeight() - size) / 2);
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        try {
            if (dotView == null) {
                dotView = new View(this);
                dotView.setBackgroundResource(R.drawable.red_dot);
                windowManager.addView(dotView, lp);
            } else {
                windowManager.updateViewLayout(dotView, lp);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot show status-bar dot", e);
            dotView = null;
        }
    }

    private void removeDot() {
        if (dotView != null) {
            try {
                windowManager.removeView(dotView);
            } catch (RuntimeException ignored) {
            }
            dotView = null;
        }
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    // ------------------------------------------------------------------ allowed callers

    private final BroadcastReceiver callReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                onRinging(number);
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                stopRinging();
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                onCallEnded();
            }
        }
    };

    private void onRinging(String number) {
        // On Android 9+ the broadcast arrives twice; only the copy with the number is useful.
        if (number == null || number.isEmpty()) return;
        Prefs p = Prefs.get(this);
        if (!p.isMutedByUs() || p.isBypassActive()) return;
        if (!PhoneNumbers.isWhitelisted(number, p.getWhitelist())) return;

        Log.i(TAG, "Allowed caller - lifting mute for this call");
        p.setBypassActive(true);
        RingerController.setRinger(this, AudioManager.RINGER_MODE_NORMAL);
        AudioManager am = getSystemService(AudioManager.class);
        if (am.getStreamVolume(AudioManager.STREAM_RING) == 0) {
            am.setStreamVolume(AudioManager.STREAM_RING,
                    Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_RING) * 2 / 3), 0);
        }
        // The system ringer may already have decided to stay quiet, so ring ourselves too.
        startRinging();
    }

    private void startRinging() {
        try {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
            if (uri == null) uri = Settings.System.DEFAULT_RINGTONE_URI;
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                if (Build.VERSION.SDK_INT >= 28) ringtone.setLooping(true);
                ringtone.play();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot play ringtone", e);
        }
        Vibrator v = getSystemService(Vibrator.class);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createWaveform(new long[]{0, 800, 800}, 0));
        }
    }

    private void stopRinging() {
        if (ringtone != null) {
            try {
                ringtone.stop();
            } catch (RuntimeException ignored) {
            }
            ringtone = null;
        }
        Vibrator v = getSystemService(Vibrator.class);
        if (v != null) v.cancel();
    }

    private void onCallEnded() {
        stopRinging();
        Prefs p = Prefs.get(this);
        if (p.isBypassActive()) {
            p.setBypassActive(false);
            RingerController.apply(this); // mute again if still in quiet hours
        }
    }

    // ------------------------------------------------------------------ notification

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL,
                getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Prefs p = Prefs.get(this);
        String from = RingerController.formatTime(p.getStartMinutes());
        String to = RingerController.formatTime(p.getEndMinutes());
        String text = RingerController.shouldBeMuted(this)
                ? getString(R.string.notif_text_active, from, to)
                : getString(R.string.notif_text_idle, from, to);
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_dot)
                .setColor(0xFFE53935)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .build();
    }
}
