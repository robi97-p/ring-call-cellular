package com.ringcall.cellular;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/** Applies the language chosen inside the app (English / Hebrew), independent of the system language. */
public final class LocaleHelper {
    private LocaleHelper() {}

    public static Context wrap(Context base) {
        String lang = Prefs.get(base).getLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }
}
