package com.kinvo.easyinventory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public final class FeatureGate {
    private FeatureGate() {}
    private static final String TAG = "FeatureGate";

    // ---- Public helpers -----------------------------------------------------

    /** DEMO + PREMIUM allowed; BASIC blocked (shows upgrade dialog). */
    public static boolean requirePremiumOrDemo(
            Activity activity, SecurePrefs prefs, String featureName, @Nullable String upgradeUrl) {
        Tier tier = resolveTier(prefs);
        if (tier == Tier.PREMIUM || tier == Tier.DEMO) return true;
        showUpgradeDialog(activity, featureName, upgradeUrl,
                featureName + " is available on Demo and Premium plans. Upgrade to continue.");
        return false;
    }

    /** PREMIUM only; DEMO/BASIC blocked. */
    public static boolean requirePremium(
            Activity activity, SecurePrefs prefs, String featureName, @Nullable String upgradeUrl) {
        Tier tier = resolveTier(prefs);
        if (tier == Tier.PREMIUM) return true;
        showUpgradeDialog(activity, featureName, upgradeUrl,
                featureName + " is available on the Premium plan. Upgrade to continue.");
        return false;
    }

    /** Convenience for menu enable/disable. */
    public static boolean isBasic(SecurePrefs prefs) { return resolveTier(prefs) == Tier.BASIC; }

    /** Optional: call this in onResume while debugging. */
    public static void debugDump(SecurePrefs prefs) {
        String prefTier = safe(prefs == null ? null : prefs.getTierName());
        boolean bd = false, bp = false;
        String buildTier = null;
        try { bd = com.kinvo.easyinventory.BuildConfig.IS_DEMO; } catch (Throwable ignore) {}
        try { bp = com.kinvo.easyinventory.BuildConfig.IS_PREMIUM; } catch (Throwable ignore) {}
        try { buildTier = com.kinvo.easyinventory.BuildConfig.TIER; } catch (Throwable ignore) {}
        Log.d(TAG, "resolveTier debug -> IS_DEMO=" + bd + ", IS_PREMIUM=" + bp
                + ", BuildConfig.TIER=" + buildTier + ", Pref.TIER=" + prefTier);
    }

    // ---- Core resolution (single source of truth) ---------------------------

    private static Tier resolveTier(SecurePrefs prefs) {
        // 1) Flavor flags are authoritative
        try {
            if (com.kinvo.easyinventory.BuildConfig.IS_DEMO)    return Tier.DEMO;
            if (com.kinvo.easyinventory.BuildConfig.IS_PREMIUM) return Tier.PREMIUM;
        } catch (Throwable ignored) {}

        // 2) Build string (in case flags weren’t defined for some reason)
        try {
            String t = com.kinvo.easyinventory.BuildConfig.TIER; // "demo"/"basic"/"premium"
            Tier parsed = Tier.fromString(t);
            if (parsed != null) return parsed;
        } catch (Throwable ignored) {}

        // 3) Persisted preference (seeded by Splash)
        if (prefs != null) {
            try {
                Tier p = Tier.fromString(prefs.getTierName());
                if (p != null) return p;
            } catch (Throwable ignored) {}
        }

        // 4) Last resort
        return Tier.BASIC;
    }

    // ---- Internals ----------------------------------------------------------

    private static void showUpgradeDialog(Activity activity, String featureName,
                                          @Nullable String upgradeUrl, String message) {
        new AlertDialog.Builder(activity)
                .setTitle("Upgrade required")
                .setMessage(message)
                .setPositiveButton("Upgrade", (d, w) -> {
                    if (upgradeUrl != null && !upgradeUrl.trim().isEmpty()) {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
