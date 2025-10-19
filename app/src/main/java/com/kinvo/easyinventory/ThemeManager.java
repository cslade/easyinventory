package com.kinvo.easyinventory;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    private static final String PREFS = "app_prefs";
    private static final String KEY_THEME_MODE = "theme_mode"; // "system" | "light" | "dark"
    private static final String LEGACY_KEY_DARK_MODE = "dark_mode"; // old boolean (optional)

    private ThemeManager() {}

    /** Apply saved theme. Call this in Application.onCreate() and whenever user changes theme. */
    public static void applySavedTheme(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // one-time migration from old boolean if present
        if (!sp.contains(KEY_THEME_MODE) && sp.contains(LEGACY_KEY_DARK_MODE)) {
            boolean legacyDark = sp.getBoolean(LEGACY_KEY_DARK_MODE, false);
            sp.edit()
                    .putString(KEY_THEME_MODE, legacyDark ? "dark" : "light")
                    .remove(LEGACY_KEY_DARK_MODE)
                    .apply();
        }

        String mode = sp.getString(KEY_THEME_MODE, "system");
        int nightMode;
        switch (mode) {
            case "light": nightMode = AppCompatDelegate.MODE_NIGHT_NO; break;
            case "dark":  nightMode = AppCompatDelegate.MODE_NIGHT_YES; break;
            default:      nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /** Persist and apply a new mode: "system" | "light" | "dark". */
    public static void setMode(Context ctx, String mode) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME_MODE, mode).apply();
        applySavedTheme(ctx);
    }

    public static String getMode(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME_MODE, "system");
    }
}

