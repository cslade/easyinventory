package com.kinvo.easyinventory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public final class UpgradeLinks {
    private UpgradeLinks() {}

    private static boolean isDemoFlavor() {
        return BuildConfig.FLAVOR != null && BuildConfig.FLAVOR.equalsIgnoreCase("demo");
    }

    // --- tier resolution via name, with flavor fallbacks ---
    private static Tier resolveTier(SecurePrefs p) {
        if (p != null) {
            try {
                String name = p.getTierName(); // "DEMO" | "BASIC" | "PREMIUM"
                if (name != null) {
                    switch (name.trim().toUpperCase()) {
                        case "DEMO":    return Tier.DEMO;
                        case "PREMIUM": return Tier.PREMIUM;
                        case "BASIC":
                        default:        return Tier.BASIC;
                    }
                }
            } catch (Throwable ignored) {}
        }
        try {
            if (BuildConfig.IS_DEMO)    return Tier.DEMO;
            if (BuildConfig.IS_PREMIUM) return Tier.PREMIUM;
        } catch (Throwable ignored) {}
        return Tier.BASIC;
    }

    /** Choose a sensible upgrade destination based on current tier. */
    public static String getUrlForUpgrade(SecurePrefs p) {
        Tier t = resolveTier(p);
        if (t == Tier.DEMO) {
            return isDemoFlavor()
                    ? "https://easyinventory.webflow.io/upgrade-basic"
                    : "https://www.easyinventory.io/pricing";
        }
        if (t == Tier.BASIC) {
            return isDemoFlavor()
                    ? "https://easyinventory.webflow.io/upgrade-premium"
                    : "https://www.easyinventory.io/pricing";
        }
        // PREMIUM -> no upgrade
        return "";
    }

    public static Intent intentToUpgrade(Activity a, SecurePrefs p) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(getUrlForUpgrade(p)));
    }
}
