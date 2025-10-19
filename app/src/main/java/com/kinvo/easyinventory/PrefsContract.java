package com.kinvo.easyinventory;

public final class PrefsContract {
    // SharedPrefs (primary)
    public static final String PREFS_USER = "UserPrefs";
    public static final String KEY_API_KEY = "apiKey";
    public static final String KEY_API_SECRET = "apiSecret";
    public static final String KEY_LOCATION_ID = "locationId";
    public static final String KEY_AUTH_HEADER_BASIC = "authHeaderBasic";
    public static final String KEY_REMEMBER_API = "rememberApi";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    // Legacy mirror (if older code paths still read these)
    public static final String PREFS_LEGACY = "MyAppPrefs";
    public static final String LEGACY_API_KEY = "API_KEY";
    public static final String LEGACY_API_SECRET = "API_SECRET";
    public static final String LEGACY_LOCATION_ID = "LOCATION_ID";
    public static final String LEGACY_AUTH_HEADER = "AUTH_HEADER";

    private PrefsContract() {}
}
