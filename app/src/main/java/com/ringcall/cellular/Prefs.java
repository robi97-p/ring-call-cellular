package com.ringcall.cellular;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/** All persisted settings and runtime state of the app. */
public final class Prefs {
    public static final String MODE_SILENT = "silent";
    public static final String MODE_VIBRATE = "vibrate";

    private static final String FILE = "ring_call_prefs";
    private final SharedPreferences sp;

    private Prefs(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static Prefs get(Context ctx) {
        return new Prefs(ctx);
    }

    // ---- user settings ----
    public boolean isEnabled() { return sp.getBoolean("enabled", false); }
    public void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }

    /** Minutes since midnight. */
    public int getStartMinutes() { return sp.getInt("start", 22 * 60); }
    public void setStartMinutes(int v) { sp.edit().putInt("start", v).apply(); }

    public int getEndMinutes() { return sp.getInt("end", 7 * 60); }
    public void setEndMinutes(int v) { sp.edit().putInt("end", v).apply(); }

    public String getMode() { return sp.getString("mode", MODE_SILENT); }
    public void setMode(String v) { sp.edit().putString("mode", v).apply(); }

    /** "iw" (Hebrew) or "en". */
    public String getLanguage() { return sp.getString("lang", "iw"); }
    public void setLanguage(String v) { sp.edit().putString("lang", v).commit(); }

    public boolean isDotEnabled() { return sp.getBoolean("dot", true); }
    public void setDotEnabled(boolean v) { sp.edit().putBoolean("dot", v).apply(); }

    /** Distance of the red dot from the right edge of the screen, in dp. */
    public int getDotOffsetDp() { return sp.getInt("dot_offset", 90); }
    public void setDotOffsetDp(int v) { sp.edit().putInt("dot_offset", v).apply(); }

    public Set<String> getWhitelist() {
        return new TreeSet<>(sp.getStringSet("whitelist", new HashSet<>()));
    }

    public void setWhitelist(Set<String> numbers) {
        sp.edit().putStringSet("whitelist", new HashSet<>(numbers)).apply();
    }

    // ---- runtime state ----
    /** True while the ringer was changed by this app and must be restored later. */
    public boolean isMutedByUs() { return sp.getBoolean("muted_by_us", false); }
    public void setMutedByUs(boolean v) { sp.edit().putBoolean("muted_by_us", v).commit(); }

    public int getSavedRingerMode() { return sp.getInt("saved_ringer", 2 /* RINGER_MODE_NORMAL */); }
    public void setSavedRingerMode(int v) { sp.edit().putInt("saved_ringer", v).commit(); }

    /** True while an allowed caller is ringing / talking and the mute is temporarily lifted. */
    public boolean isBypassActive() { return sp.getBoolean("bypass", false); }
    public void setBypassActive(boolean v) { sp.edit().putBoolean("bypass", v).commit(); }
}
