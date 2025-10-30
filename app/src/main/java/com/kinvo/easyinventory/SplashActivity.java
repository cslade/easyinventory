package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Splash";

    // UserPrefs (non-sensitive): we only keep a simple membership flag here.
    private static final String PREFS_USER = "UserPrefs";
    private static final String KEY_MEMBERSHIP_OK = "membershipOk";

    // Tiny delay for visual splash
    private static final long SPLASH_DELAY_MS = 900L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTierFromBuild();
        setContentView(R.layout.activity_splash); // keep this lightweight

        // 1) Seed tier from current Gradle flavor so gates are ready immediately.
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            Tier current = TierUtils.fromFlavor(BuildConfig.FLAVOR); // demo/basic/premium
            // prefs.setTier(current);  // ← OLD (compile error)
            prefs.setTierName(current.name());  // ← NEW
            // optional: friendly plan label
            prefs.setPlanName(current.name());
            Log.d(TAG, "Seeded tier from flavor: " + current + " (flavor=" + BuildConfig.FLAVOR + ")");
        } catch (Exception e) {
            Log.w(TAG, "SecurePrefs tier bootstrap failed", e);
        }

        // If opened by a deep link, log it for debugging
        Intent launch = getIntent();
        if (launch != null && launch.getData() != null) {
            Log.d(TAG, "Launch data URI: " + launch.getData());
        }

        // 2) Route after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    // --- Force the tier from the installed flavor (authoritative) ---
    private void initTierFromBuild() {
        SecurePrefs prefs = SecurePrefs.get(this);
        Tier tier = resolveTierFromBuild();
        // prefs.setTier((tier == null) ? Tier.BASIC : tier);  // ← OLD
        prefs.setTierName(((tier == null) ? Tier.BASIC : tier).name());  // ← NEW

        // If your SecurePrefs also tracks a plan label, you can set it here:
        // prefs.setPlanName(tier.name());
    }

    private Tier resolveTierFromBuild() {
        if (BuildConfig.IS_DEMO)    return Tier.DEMO;
        if (BuildConfig.IS_PREMIUM) return Tier.PREMIUM;
        return Tier.BASIC; // default
    }

    private boolean ensureProviderSelected() {
        SecurePrefs prefs = SecurePrefs.get(this);
        String provider = String.valueOf(prefs.getProvider());
        if (provider == null || provider.trim().isEmpty()) {
            // Let the user pick once; when they hit Continue the picker returns to Splash
            startActivity(new Intent(this, ProviderPickerActivity.class));
            finish(); // stop Splash so we re-enter fresh after picker
            return false;
        }
        return true;
    }

    private void navigateNext() {
        if (!ensureProviderSelected()) return;
        SharedPreferences sp = getSharedPreferences(PREFS_USER, MODE_PRIVATE);
        boolean membershipOk = sp.getBoolean(KEY_MEMBERSHIP_OK, false);
        Log.d(TAG, "membershipOk=" + membershipOk);

        if (membershipOk) {
            Log.d(TAG, "Routing -> LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            Log.d(TAG, "Routing -> MembershipLoginActivity");
            startActivity(new Intent(this, MembershipLoginActivity.class));
        }
        finish();
    }
}
