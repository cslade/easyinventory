package com.kinvo.easyinventory.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyUtils {
    private CurrencyUtils() {}

    /** Formats money using the device locale; null-safe. */
    public static String format(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.getDefault());
        return nf.format(amount == null ? BigDecimal.ZERO : amount);
    }

    /** Optional: explicit locale override. */
    public static String format(BigDecimal amount, Locale locale) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale == null ? Locale.getDefault() : locale);
        return nf.format(amount == null ? BigDecimal.ZERO : amount);
    }
}
