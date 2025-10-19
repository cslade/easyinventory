package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Splash";

    // Primary user prefs for membership flag
    private static final String PREFS_USER = "UserPrefs";
    private static final String KEY_MEMBERSHIP_OK = "membershipOk";

    // Tiny delay just for visual splash
    private static final long SPLASH_DELAY_MS = 900L;

    // Auth URLs (adjust if your production URL differs)
    private static final String DEMO_AUTH_URL = "https://easyinventory.webflow.io/login";
    private static final String PROD_AUTH_URL = "https://easyinventory.io/login";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // keep this lightweight

        // Set default tier from the current flavor so the app can gate features immediately.
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            Tier tier = TierUtils.fromFlavor(BuildConfig.FLAVOR); // demo/basic/premium
            prefs.setTier(tier);
            prefs.setPlanName(tier.name()); // or a friendlier label if you prefer
        } catch (Exception e) {
            Log.w(TAG, "SecurePrefs tier bootstrap failed", e);
        }

        Log.d(TAG, "onCreate() flavor=" + BuildConfig.FLAVOR
                + " isDemo=" + BuildConfig.IS_DEMO
                + " isPremium=" + BuildConfig.IS_PREMIUM);

        // If opened by a deep link, this helps verify what arrived
        Intent launch = getIntent();
        if (launch != null && launch.getData() != null) {
            Log.d(TAG, "Launch data URI: " + launch.getData());
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        SharedPreferences sp = getSharedPreferences(PREFS_USER, MODE_PRIVATE);
        boolean membershipOk = sp.getBoolean(KEY_MEMBERSHIP_OK, false);
        Log.d(TAG, "membershipOk=" + membershipOk);

        if (membershipOk) {
            // Membership already verified -> go collect API creds
            Log.d(TAG, "Routing -> LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Not verified yet -> show hosted login in WebView
        String authUrl = BuildConfig.IS_DEMO ? DEMO_AUTH_URL : PROD_AUTH_URL;
        if (authUrl == null || authUrl.trim().isEmpty()) {
            Toast.makeText(this, "Missing auth URL", Toast.LENGTH_SHORT).show();
            authUrl = DEMO_AUTH_URL;
        }

        Log.d(TAG, "Routing -> WebViewLoginActivity with authUrl=" + authUrl);
        Intent i = new Intent(this, WebViewLoginActivity.class);
        i.putExtra("authUrl", authUrl);
        startActivity(i);
        finish();
    }
}
