package com.kinvo.easyinventory;

import static com.kinvo.easyinventory.Tier.BASIC;

public final class TierUtils {
    private TierUtils() {}

    /** Maps your Gradle flavor to a default Tier. */
    public static Tier fromFlavor(String flavor) {
        if (flavor == null) return BASIC;
        switch (flavor.toLowerCase()) {
            case "demo":    return Tier.DEMO;
            case "basic":   return BASIC;
            case "premium": return Tier.PREMIUM;
            default:        return BASIC;
        }
    }
}


