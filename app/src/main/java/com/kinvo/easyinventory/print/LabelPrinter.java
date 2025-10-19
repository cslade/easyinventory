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
        Tier tier = prefs.getTier(); // demo/basic/premium

        boolean demoWatermark = (tier == Tier.DEMO);

        try {
            Bitmap bmp = LabelBitmapRenderer.render(ctx, data, demoWatermark);
            PrintHelper helper = new PrintHelper(ctx);
            helper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            helper.printBitmap("EasyInventory Label - " + data.sku, bmp);
        } catch (Exception e) {
            e.printStackTrace();
            // You can show a toast here if you prefer
        }
    }
}
