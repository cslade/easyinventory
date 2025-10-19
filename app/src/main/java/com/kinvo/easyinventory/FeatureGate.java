package com.kinvo.easyinventory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;

public final class FeatureGate {
    private FeatureGate() {}

    // Example gates: tune as needed
    public static boolean canUseBatchAdjust(Tier t)  { return t.ordinal() >= Tier.BASIC.ordinal(); }
    public static boolean canUseAdvancedFilters(Tier t){ return t.ordinal() >= Tier.BASIC.ordinal(); }
    public static boolean canUseBulkExport(Tier t)   { return t.ordinal() >= Tier.PREMIUM.ordinal(); }

    public static boolean requireTier(Activity a, Tier have, Tier need, String featureName, String upgradeUrl) {
        if (have.ordinal() >= need.ordinal()) return true;

        new AlertDialog.Builder(a)
                .setTitle("Upgrade required")
                .setMessage(featureName + " is available on " + need.name() + " and above.")
                .setPositiveButton("Upgrade", (d, w) -> {
                    if (upgradeUrl != null && !upgradeUrl.isEmpty()) {
                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUrl)));
                    }
                })
                .setNegativeButton("Maybe later", null)
                .show();
        return false;
    }
}
