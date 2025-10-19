package com.kinvo.easyinventory.print;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public final class BarcodeUtil {
    private BarcodeUtil() {}

    public static Bitmap code128(String data, int widthPx, int heightPx) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(
                data, BarcodeFormat.CODE_128, widthPx, heightPx);
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        return bmp;
    }
}

