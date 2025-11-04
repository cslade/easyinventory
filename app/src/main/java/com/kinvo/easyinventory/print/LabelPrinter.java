package com.kinvo.easyinventory.print;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.print.PrintHelper;

import com.kinvo.easyinventory.SecurePrefs;
import com.kinvo.easyinventory.Tier;
import com.kinvo.easyinventory.TierUtils;

public final class LabelPrinter {
    private LabelPrinter() {}

    public static void printSingle(Context ctx, LabelData data) {
        SecurePrefs prefs = SecurePrefs.get(ctx);
        Tier tier = TierUtils.resolveTier(prefs); // ← use name-based resolution

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

    private static String safe(String s) { return s == null ? "" : s; }
}
