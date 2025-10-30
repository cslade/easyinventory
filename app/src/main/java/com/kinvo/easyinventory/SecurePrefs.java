package com.kinvo.easyinventory;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.kinvo.easyinventory.data.DataSource;
import com.kinvo.easyinventory.data.Provider; // keep for back-compat (if used elsewhere)

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Centralized encrypted prefs. Singleton via get(Context).
 * Stores: EPOS Now creds, Shopify creds (API key/secret/domain), active provider, location, tier, etc.
 * <p>
 * Updated to align with LoginActivity, which expects:
 *   getShopApiKey(), setShopApiKey()
 *   getShopApiSecret(), setShopApiSecret()
 *   getShopDomain(), setShopDomain()
 *   getShopToken(), setShopToken()  // kept as alias for legacy token-based flows
 * <p>
 * Added (Clover):
 *   getCloverAccessToken(), setCloverAccessToken()
 *   getCloverMerchantId(),  setCloverMerchantId()
 *   clearClover()
 */
public final class SecurePrefs {

    // ---- Optional local enum kept for UI/back-compat ----
    public enum ProviderType { EPOSNOW, SHOPIFY, CLOVER }

    // ---- Singleton ----
    private static SecurePrefs INSTANCE;
    public static synchronized SecurePrefs get(@NonNull Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new SecurePrefs(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    // ---- File + Keys ----
    private static final String FILE = "secure_prefs";

    // EPOS Now
    private static final String K_API_KEY        = "epos_api_key";
    private static final String K_API_SECRET     = "epos_api_secret";
    private static final String K_AUTH_BASIC     = "epos_auth_header_basic";
    private static final String K_LOCATION_ID    = "epos_location_id"; // int

    // Shopify (canonical)
    private static final String K_SHOPIFY_DOMAIN = "shopify_domain";        // e.g. myshop.myshopify.com
    private static final String K_SHOPIFY_TOKEN  = "shopify_access_token";  // Admin API token
    private static final String K_SHOPIFY_LOC_ID = "shopify_location_id";   // long (optional)

    // Shopify (new for key/secret login)
    private static final String K_SHOP_API_KEY    = "shop_api_key";
    private static final String K_SHOP_API_SECRET = "shop_api_secret";

    // Clover (new)
    private static final String K_CLOVER_TOKEN       = "clover_access_token";   // “Clover API Token” field
    private static final String K_CLOVER_MERCHANT_ID = "clover_merchant_id";    // “Merchant ID” field

    // Provider choice (string for back-compat) + enum bridge
    private static final String K_ACTIVE_PROVIDER = "active_provider"; // "EPOSNOW" | "SHOPIFY" | "CLOVER"
    private static final String KEY_PROVIDER_ENUM = "provider";        // stores DataSource.name()

    // Misc flags / plan
    private static final String K_REMEMBER_API   = "remember_api";
    private static final String K_IS_LOGGED_IN   = "is_logged_in";
    private static final String KEY_TIER         = "tier";             // "DEMO" | "BASIC" | "PREMIUM"
    private static final String KEY_PLAN_NAME    = "pref_plan_name";

    private final SharedPreferences sp;

    private SecurePrefs(Context app) {
        SharedPreferences tmp;
        try {
            MasterKey key = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            tmp = EncryptedSharedPreferences.create(
                    app,
                    FILE,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to plain SharedPreferences if crypto is unavailable
            tmp = app.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        }
        this.sp = tmp;
    }

    /** True only if the user explicitly chose a provider (enum or legacy string key is present). */
    public boolean isProviderChosen() {
        return sp.contains(KEY_PROVIDER_ENUM) || sp.contains(K_ACTIVE_PROVIDER);
    }

    public Tier getTier() {
        String name = getTierName();
        switch (name.trim().toUpperCase()) {
            case "DEMO":    return Tier.DEMO;
            case "PREMIUM": return Tier.PREMIUM;
            default:        return Tier.BASIC;
        }
    }

    // ------------------------------------------------------------------------
    // Provider (enum-first, with string back-compat)
    // ------------------------------------------------------------------------

    /** Preferred enum API used by the app. */
    public void setProvider(DataSource ds) {
        String name = (ds == null ? DataSource.EPOSNOW : ds).name();
        sp.edit().putString(KEY_PROVIDER_ENUM, name).apply();
        setProviderName(name); // keep legacy key in sync
    }

    /** Back-compat overload if some code still uses a different Provider enum. */
    public void setProvider(Provider legacyProvider) {
        String name = (legacyProvider == null ? "EPOSNOW" : legacyProvider.name());
        setProviderName(name);
    }

    public DataSource getProvider() {
        String s = sp.getString(KEY_PROVIDER_ENUM, null);
        if (s == null) s = getProviderName(); // fall back to legacy string key
        try { return DataSource.valueOf(s); } catch (Exception e) { return DataSource.EPOSNOW; }
    }

    public String getProviderName() {
        return sp.getString(K_ACTIVE_PROVIDER, "");
    }

    /** Back-compat string setter; keeps enum in sync as well. */
    public void setProviderName(String name) {
        String n = nz(name).toUpperCase();
        sp.edit().putString(K_ACTIVE_PROVIDER, n).apply();
        try {
            DataSource ds = DataSource.valueOf(n);
            sp.edit().putString(KEY_PROVIDER_ENUM, ds.name()).apply();
        } catch (Exception ignore) {
            sp.edit().putString(KEY_PROVIDER_ENUM, DataSource.EPOSNOW.name()).apply();
        }
    }

    /** Old UI helper some screens may still use. */
    public ProviderType getProviderType() {
        try { return ProviderType.valueOf(getProvider().name()); }
        catch (Exception ignore) { return ProviderType.EPOSNOW; }
    }

    // ------------------------------------------------------------------------
    // EPOS Now
    // ------------------------------------------------------------------------
    public String getApiKey()                   { return sp.getString(K_API_KEY, null); }
    public void   setApiKey(String v)           { sp.edit().putString(K_API_KEY, nz(v)).apply(); }

    public String getApiSecret()                { return sp.getString(K_API_SECRET, null); }
    public void   setApiSecret(String v)        { sp.edit().putString(K_API_SECRET, nz(v)).apply(); }

    public String getAuthHeaderBasic()          { return sp.getString(K_AUTH_BASIC, null); }
    public void   setAuthHeaderBasic(String v)  { sp.edit().putString(K_AUTH_BASIC, nz(v)).apply(); }

    public int    getLocationId()               { return sp.getInt(K_LOCATION_ID, 0); }
    public void   setLocationId(int v)          { sp.edit().putInt(K_LOCATION_ID, v).apply(); }

    // ------------------------------------------------------------------------
    // Shopify — new API key/secret + domain (as used by LoginActivity)
    // ------------------------------------------------------------------------
    public String getShopApiKey()               { return sp.getString(K_SHOP_API_KEY, null); }
    public void   setShopApiKey(String v)       { sp.edit().putString(K_SHOP_API_KEY, nz(v)).apply(); }

    public String getShopApiSecret()            { return sp.getString(K_SHOP_API_SECRET, null); }
    public void   setShopApiSecret(String v)    { sp.edit().putString(K_SHOP_API_SECRET, nz(v)).apply(); }

    /** Aliases to canonical Shopify domain storage. */
    public String getShopDomain()               { return sp.getString(K_SHOPIFY_DOMAIN, null); }
    public void   setShopDomain(String v)       { sp.edit().putString(K_SHOPIFY_DOMAIN, nz(v)).apply(); }

    /** Token-based flows kept for back-compat (alias). */
    public String getShopToken()                { return sp.getString(K_SHOPIFY_TOKEN, null); }
    public void   setShopToken(String v)        { sp.edit().putString(K_SHOPIFY_TOKEN, nz(v)).apply(); }

    /** Optional Shopify location id. */
    public long   getShopifyLocationId()        { return sp.getLong(K_SHOPIFY_LOC_ID, 0L); }
    public void   setShopifyLocationId(long v)  { sp.edit().putLong(K_SHOPIFY_LOC_ID, v).apply(); }

    // ------------------------------------------------------------------------
    // Clover (token + merchant id)
    // ------------------------------------------------------------------------
    public String getCloverAccessToken()              { return sp.getString(K_CLOVER_TOKEN, null); }
    public void   setCloverAccessToken(String token)  { sp.edit().putString(K_CLOVER_TOKEN, nz(token)).apply(); }

    public String getCloverMerchantId()               { return sp.getString(K_CLOVER_MERCHANT_ID, null); }
    public void   setCloverMerchantId(String id)      { sp.edit().putString(K_CLOVER_MERCHANT_ID, nz(id)).apply(); }

    /** Clear only Clover-related keys. */
    public void clearClover() {
        sp.edit()
                .remove(K_CLOVER_TOKEN)
                .remove(K_CLOVER_MERCHANT_ID)
                .apply();
    }

    // ------------------------------------------------------------------------
    // Flags / plan / tier
    // ------------------------------------------------------------------------
    public boolean getRememberApi()             { return sp.getBoolean(K_REMEMBER_API, false); }
    public void    setRememberApi(boolean v)    { sp.edit().putBoolean(K_REMEMBER_API, v).apply(); }

    public boolean isLoggedIn()                 { return sp.getBoolean(K_IS_LOGGED_IN, false); }
    public void    setLoggedIn(boolean v)       { sp.edit().putBoolean(K_IS_LOGGED_IN, v).apply(); }

    public String  getTierName()                { return sp.getString(KEY_TIER, "BASIC"); }
    public void    setTierName(String name)     { sp.edit().putString(KEY_TIER, nz(name)).apply(); }

    public String  getPlanName()                { return sp.getString(KEY_PLAN_NAME, ""); }
    public void    setPlanName(String name)     { sp.edit().putString(KEY_PLAN_NAME, nz(name)).apply(); }

    // ------------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------------
    /** Clear everything (used by Logout). */
    public void clearAll() { sp.edit().clear().apply(); }

    /** Back-compat alias for older code calling prefs.logout(). */
    public void logout() { clearAll(); }

    /** Clear only Shopify-related keys (handy when switching providers). */
    public void clearShopify() {
        sp.edit()
                .remove(K_SHOP_API_KEY)
                .remove(K_SHOP_API_SECRET)
                .remove(K_SHOPIFY_DOMAIN)
                .remove(K_SHOPIFY_TOKEN)
                .remove(K_SHOPIFY_LOC_ID)
                .apply();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
