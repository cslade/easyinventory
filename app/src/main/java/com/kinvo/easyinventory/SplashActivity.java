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
        bootstrapTierIfMissing();
        setContentView(R.layout.activity_splash); // keep this lightweight

        // If opened by a deep link, log it for debugging
        Intent launch = getIntent();
        if (launch != null && launch.getData() != null) {
            Log.d(TAG, "Launch data URI: " + launch.getData());
        }

        // 2) Route after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    // --- Seed the tier only when nothing has been persisted yet ---
    private void bootstrapTierIfMissing() {
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            if (prefs.hasStoredTier()) {
                Log.d(TAG, "Existing stored tier detected (" + prefs.getTierName() + "); skipping bootstrap");
                return;
            }

            boolean allowFlavorSeed = TierUtils.isEntitlementFlavorBuild();
            Tier seed = TierUtils.resolveTier(prefs, allowFlavorSeed);

            prefs.setTierName(seed.name());
            if (prefs.getPlanName() == null || prefs.getPlanName().trim().isEmpty()) {
                prefs.setPlanName(TierUtils.displayName(seed));
            }

            Log.d(TAG, "Seeded initial tier=" + seed
                    + (allowFlavorSeed ? " (from flavor " + BuildConfig.FLAVOR + ")" : " (default)"));
        } catch (Exception e) {
            Log.w(TAG, "SecurePrefs tier bootstrap failed", e);
        }
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
