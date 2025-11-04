package com.kinvo.easyinventory;

import androidx.annotation.Nullable;

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

    /** Returns a tier stored in SecurePrefs, or {@code null} if nothing has been persisted. */
    @Nullable
    public static Tier storedTier(@Nullable SecurePrefs prefs) {
        if (prefs == null || !prefs.hasStoredTier()) return null;
        try {
            return Tier.fromString(prefs.getTierName());
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * Resolves the effective tier using the stored membership first, falling back to flavor-based
     * defaults only for QA builds.
     */
    public static Tier resolveTier(@Nullable SecurePrefs prefs) {
        return resolveTier(prefs, isEntitlementFlavorBuild());
    }

    /** Same as {@link #resolveTier(SecurePrefs)} but lets callers override the fallback flag. */
    public static Tier resolveTier(@Nullable SecurePrefs prefs, boolean allowFlavorFallback) {
        Tier stored = storedTier(prefs);
        if (stored != null) return stored;

        if (allowFlavorFallback && isEntitlementFlavorBuild()) {
            try {
                return fromFlavor(BuildConfig.FLAVOR);
            } catch (Throwable ignore) {
                // fall through to BASIC
            }
        }
        return BASIC;
    }

    /** Human readable label for the provided tier. */
    public static String displayName(@Nullable Tier tier) {
        if (tier == null) return "Basic";
        switch (tier) {
            case DEMO:    return "Demo";
            case PREMIUM: return "Premium";
            default:     return "Basic";
        }
    }

    /**
     * Returns the best label to show for the active plan – stored plan name when available,
     * otherwise a friendly tier display name.
     */
    public static String planLabel(@Nullable SecurePrefs prefs) {
        if (prefs != null) {
            try {
                String stored = prefs.getPlanName();
                if (stored != null && !stored.trim().isEmpty()) {
                    return stored.trim();
                }
            } catch (Throwable ignore) {
                // fall back to tier display name below
            }
        }
        return displayName(resolveTier(prefs));
    }

    /** True when build-time flavor flags should be honored (debug/demo/premium QA builds). */
    public static boolean isEntitlementFlavorBuild() {
        try {
            return BuildConfig.DEBUG || BuildConfig.IS_DEMO || BuildConfig.IS_PREMIUM;
        } catch (Throwable ignore) {
            return false;
        }
    }
}


