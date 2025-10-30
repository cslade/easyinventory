package com.kinvo.easyinventory.print;

import android.content.Context;
import android.graphics.*;
import android.text.TextUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class LabelBitmapRenderer {
    private LabelBitmapRenderer() {}

    /**
     * Renders a high-res label bitmap ~300dpi for a 4.0" x 2.5" label (1200 x 750 px).
     * Adjust WIDTH/HEIGHT to match your printer/label size if needed.
     */
    public static Bitmap render(Context ctx, LabelData data, boolean demoWatermark) throws Exception {
        final int WIDTH = 1200;   // ~4.0" at 300dpi
        final int HEIGHT = 750;   // ~2.5" at 300dpi

        Bitmap bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        // Background
        c.drawColor(Color.WHITE);

        // Margins
        float m = WIDTH * 0.04f;
        RectF content = new RectF(m, m, WIDTH - m, HEIGHT - m);

        // Paints
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.BLACK);

        Paint bold = new Paint(text);
        bold.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        // 1) Item Description (top-left)
        float titleSize = content.height() * 0.16f;  // big but fits
        text.setTextSize(titleSize);
        text.setTypeface(Typeface.SANS_SERIF);
        String title = ellipsize(data.description, text, content.width() * 0.70f); // leave room for price on the right
        float titleBaseline = content.top + titleSize;
        c.drawText(title, content.left, titleBaseline, text);

        // 2) Price (large, right side)
        String priceStr = "$" + data.price.setScale(2, RoundingMode.HALF_UP).toPlainString();
        float priceSize = content.height() * 0.34f;  // huge
        bold.setTextSize(priceSize);
        float priceWidth = bold.measureText(priceStr);
        float priceX = content.right - priceWidth;
        float priceY = content.centerY() + (priceSize * 0.35f);
        c.drawText(priceStr, priceX, priceY, bold);

        // 3) Barcode (center-left area)
        float barcodeLeft = content.left;
        float barcodeRight = priceX - (content.width() * 0.05f);
        float barcodeTop = content.centerY() - (content.height() * 0.12f);
        float barcodeBottom = content.centerY() + (content.height() * 0.12f);
        RectF barcodeBox = new RectF(barcodeLeft, barcodeTop, barcodeRight, barcodeBottom);
        int bcW = Math.max(200, (int) barcodeBox.width());
        int bcH = Math.max(80, (int) barcodeBox.height());
        Bitmap barcodeBmp = BarcodeUtil.code128(emptyToPlaceholder(data.barcode), bcW, bcH);
        Rect src = new Rect(0, 0, barcodeBmp.getWidth(), barcodeBmp.getHeight());
        RectF dst = new RectF(barcodeBox);
        c.drawBitmap(barcodeBmp, src, dst, null);

        // 4) Bottom-left lines: SKU + STOCK_NUMBER
        float smallSize = content.height() * 0.12f;
        text.setTextSize(smallSize);
        float bottomFirst = content.bottom - (smallSize * 1.2f);
        c.drawText(emptyToPlaceholder(data.sku), content.left, bottomFirst, text);

        float bottomSecond = content.bottom;
        String second = TextUtils.isEmpty(data.stockNumber) ? "STOCK_NUMBER" : data.stockNumber;
        c.drawText(second, content.left, bottomSecond, text);

        // 5) DEMO watermark if demo tier
        if (demoWatermark) {
            Paint water = new Paint(Paint.ANTI_ALIAS_FLAG);
            water.setColor(Color.argb(40, 0, 0, 0));
            water.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            water.setTextSize(HEIGHT * 0.28f);
            water.setTextAlign(Paint.Align.CENTER);
            c.save();
            c.rotate(-18, WIDTH / 2f, HEIGHT / 2f);
            c.drawText("DEMO", WIDTH / 2f, HEIGHT / 2f, water);
            c.restore();
        }

        return bmp;
    }

    private static String ellipsize(String s, Paint p, float maxWidth) {
        if (TextUtils.isEmpty(s)) return "";
        if (p.measureText(s) <= maxWidth) return s;
        String ell = "...";
        float e = p.measureText(ell);
        for (int len = s.length() - 1; len > 0; len--) {
            String tryS = s.substring(0, len);
            if (p.measureText(tryS) + e <= maxWidth) return tryS + ell;
        }
        return ell;
    }

    private static String emptyToPlaceholder(String s) {
        return TextUtils.isEmpty(s) ? "000000000000" : s;
    }
}

