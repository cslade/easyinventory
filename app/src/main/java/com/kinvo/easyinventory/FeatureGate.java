// FeatureGate.java
package com.kinvo.easyinventory;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

public final class FeatureGate {
    private FeatureGate(){}

    /** Allow DEMO + PREMIUM. BASIC -> show upgrade dialog and return false. */
    public static boolean requirePremiumOrDemo(Context ctx,
                                               SecurePrefs prefs,
                                               String featureName,
                                               String upgradeUrl) {
        Tier tier = resolveTier(prefs);

        if (BuildConfig.DEBUG) {
            Log.d("FeatureGate",
                    "Gate: feature=" + featureName +
                            ", flavor=" + BuildConfig.FLAVOR +
                            ", IS_DEMO=" + BuildConfig.IS_DEMO +
                            ", IS_PREMIUM=" + BuildConfig.IS_PREMIUM +
                            ", IS_BASIC=" + BuildConfig.IS_BASIC +
                            ", resolvedTier=" + tier);
        }

        if (tier == Tier.BASIC) {
            showUpgradeDialog(ctx, featureName, upgradeUrl);
            return false;
        }
        // DEMO or PREMIUM
        return true;
    }

    private static Tier resolveTier(SecurePrefs prefs) {
        // 1) BuildConfig flags are authoritative for the installed flavor
        if (BuildConfig.IS_DEMO)    return Tier.DEMO;
        if (BuildConfig.IS_PREMIUM) return Tier.PREMIUM;
        if (BuildConfig.IS_BASIC)   return Tier.BASIC; // default (never reached)

        // 2) Persisted tier (if you ever override from server/account)
        if (prefs != null) {
            Tier t = prefs.getTier();
            if (t != null) return t;
        }

        // 3) Fallback: map flavor name -> tier safely
        return TierUtils.fromFlavor(BuildConfig.FLAVOR);
    }

    private static void showUpgradeDialog(Context ctx, String featureName, String upgradeUrl) {
        new AlertDialog.Builder(ctx)
                .setTitle("Upgrade required")
                .setMessage(featureName + " is available on Premium.\nWould you like to see plans?")
                .setPositiveButton("See Plans", (d, w) -> {
                    Intent i = new Intent(ctx, WebViewLoginActivity.class);
                    i.putExtra("authUrl", upgradeUrl);
                    ctx.startActivity(i);
                })
                .setNegativeButton("Not now", null)
                .show();
    }
}
