package com.kinvo.easyinventory;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class SecurePrefs {

    private static final String TAG = "SecurePrefs";
    private static final String FILE_NAME = "secure_prefs";

    // Tiers
    private static final String KEY_PLAN_NAME = "planName";
    private static final String KEY_TIER = "tier"; // stores "DEMO","BASIC","PREMIUM"


    // Keys
    private static final String KEY_API_KEY = "apiKey";
    private static final String KEY_API_SECRET = "apiSecret";
    private static final String KEY_LOCATION_ID = "locationId";
    private static final String KEY_AUTH_HEADER_BASIC = "authHeaderBasic";
    private static final String KEY_REMEMBER_API = "rememberApi";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private static volatile SecurePrefs INSTANCE;
    private final SharedPreferences prefs;

    /** Get singleton instance (application-scoped). */
    public static SecurePrefs get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (SecurePrefs.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SecurePrefs(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /** Private ctor – builds encrypted prefs, falls back to normal prefs on failure. */
    private SecurePrefs(Context ctx) {
        SharedPreferences p;
        try {
            MasterKey masterKey = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            p = EncryptedSharedPreferences.create(
                    ctx,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable; falling back to normal SharedPreferences", e);
            p = ctx.getSharedPreferences(FILE_NAME + "_fallback", Context.MODE_PRIVATE);
        }
        this.prefs = p;
    }

    // -----------------------
    // Getters / Setters (set* / get*)
    // -----------------------

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(@Nullable String value) {
        prefs.edit().putString(KEY_API_KEY, value != null ? value : "").apply();
    }

    public String getApiSecret() {
        return prefs.getString(KEY_API_SECRET, "");
    }

    public void setApiSecret(@Nullable String value) {
        prefs.edit().putString(KEY_API_SECRET, value != null ? value : "").apply();
    }

    public int getLocationId() {
        return prefs.getInt(KEY_LOCATION_ID, 0);
    }

    public void setLocationId(int value) {
        prefs.edit().putInt(KEY_LOCATION_ID, value).apply();
    }

    public String getAuthHeaderBasic() {
        return prefs.getString(KEY_AUTH_HEADER_BASIC, "");
    }

    public void setAuthHeaderBasic(@Nullable String value) {
        prefs.edit().putString(KEY_AUTH_HEADER_BASIC, value != null ? value : "").apply();
    }

    public boolean getRememberApi() {
        return prefs.getBoolean(KEY_REMEMBER_API, false);
    }

    public void setRememberApi(boolean value) {
        prefs.edit().putBoolean(KEY_REMEMBER_API, value).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply();
    }

    public String getPlanName() {
        return prefs.getString(KEY_PLAN_NAME, "");
    }

    public void setPlanName(String name) {
        prefs.edit().putString(KEY_PLAN_NAME, name == null ? "" : name).apply();
    }

    public void setTier(Tier tier) {
        prefs.edit().putString(KEY_TIER, tier == null ? Tier.BASIC.name() : tier.name()).apply();
    }

    /** If nothing stored yet, default from the current build flavor. */
    public Tier getTier() {
        String raw = prefs.getString(KEY_TIER, "");
        if (raw == null || raw.isEmpty()) {
            return TierUtils.fromFlavor(BuildConfig.FLAVOR);
        }
        return Tier.fromString(raw);
    }

    // -----------------------
    // Helpers
    // -----------------------

    /** Semantic logout: clear secrets/flags but keep storage intact. */
    public void logout() {
        try {
            setLoggedIn(false);
            setRememberApi(false);
            setAuthHeaderBasic(null);
            setApiKey(null);
            setApiSecret(null);
            setLocationId(0);
        } catch (Throwable t) {
            Log.w(TAG, "logout() partial failure (continuing): " + t);
        }
    }

    /** Hard clear of all keys (you said you already had this—keeping it). */
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    /** Optional: handy for debugging (redacted). */
    public String snapshotRedacted() {
        String apiKey = redact(getApiKey());
        String apiSecret = redact(getApiSecret());
        String auth = getAuthHeaderBasic().isEmpty() ? "" : "Basic ****";
        int loc = getLocationId();
        boolean remember = getRememberApi();
        boolean logged = isLoggedIn();
        return "apiKey=" + apiKey + ", apiSecret=" + apiSecret + ", auth=" + auth
                + ", locationId=" + loc + ", rememberApi=" + remember + ", isLoggedIn=" + logged;
    }

    private static String redact(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.length() <= 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}
