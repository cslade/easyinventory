package com.kinvo.easyinventory.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.kinvo.easyinventory.SecurePrefs;

/**
 * Chooses a concrete repository based on saved provider in SecurePrefs.
 * Repositories are created with NO constructor args; per-call Context is passed
 * via the InventoryRepository method signatures.
 */
public final class ProviderFactory {

    private ProviderFactory() {}

    @NonNull
    public static InventoryRepository get(@NonNull Context ctx) {
        SecurePrefs prefs = SecurePrefs.get(ctx);
        String provider = String.valueOf(prefs.getProvider()); // e.g., "eposnow", "shopify", "clover"
        if (provider == null) provider = "eposnow";

        switch (provider.toLowerCase()) {
            case "shopify":
                return new ShopifyRepository();   // no Context in ctor
            case "clover":
                return new CloverRepository();    // no Context in ctor
            case "eposnow":
            default:
                return new EposNowRepository();   // no Context in ctor
        }
    }
}
