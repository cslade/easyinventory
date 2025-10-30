package com.kinvo.easyinventory.print;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.print.PrintHelper;

import com.kinvo.easyinventory.SecurePrefs;
import com.kinvo.easyinventory.Tier;

public final class LabelPrinter {
    private LabelPrinter() {}

    public static void printSingle(Context ctx, LabelData data) {
        SecurePrefs prefs = SecurePrefs.get(ctx);
        Tier tier = resolveTier(prefs); // ← use name-based resolution

        boolean demoWatermark = (tier == Tier.DEMO);

        try {
            Bitmap bmp = LabelBitmapRenderer.render(ctx, data, demoWatermark);
            PrintHelper helper = new PrintHelper(ctx);
            helper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            helper.printBitmap("EasyInventory Label - " + (data == null ? "" : safe(data.sku)), bmp);
        } catch (Exception e) {
            e.printStackTrace();
            // Optional: Toast.makeText(ctx, "Print failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // --- helpers ---

    private static Tier resolveTier(SecurePrefs prefs) {
        if (prefs != null) {
            try {
                String name = prefs.getTierName(); // "DEMO", "BASIC", "PREMIUM"
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
        // Fallback to build flags if present
        try {
            if (com.kinvo.easyinventory.BuildConfig.IS_DEMO)    return Tier.DEMO;
            if (com.kinvo.easyinventory.BuildConfig.IS_PREMIUM) return Tier.PREMIUM;
        } catch (Throwable ignored) {}
        return Tier.BASIC;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
