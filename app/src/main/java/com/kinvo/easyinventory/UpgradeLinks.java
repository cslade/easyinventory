package com.kinvo.easyinventory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public final class UpgradeLinks {
    private UpgradeLinks() {}

    private static boolean isDemoFlavor() {
        return BuildConfig.FLAVOR != null && BuildConfig.FLAVOR.equalsIgnoreCase("demo");
    }

    /** Choose a sensible upgrade destination based on current tier. */
    public static String getUrlForUpgrade(SecurePrefs p) {
        Tier t = TierUtils.resolveTier(p);
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
