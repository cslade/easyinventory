package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String USER_PREFS = "UserPrefs";
    private static final String IS_LOGGED_IN = "isLoggedIn";
    private static final long SPLASH_DELAY = 2000; // 2 seconds delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ No need for setContentView() if using new Material Splash Screen API

        // ✅ Define navigation logic as a Runnable
        Runnable navigationRunnable = this::navigateToNextScreen;

        // ✅ Execute the navigation after SPLASH_DELAY
        new Handler(Looper.getMainLooper()).postDelayed(navigationRunnable, SPLASH_DELAY);

    }

    // ✅ Determines which screen to navigate to based on login status
    private void navigateToNextScreen() {
        SharedPreferences sharedPreferences = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(IS_LOGGED_IN, false);

        Intent intent;
        if (isLoggedIn) {
            // 🔹 If the user is logged in, go to MainActivity (ePOSNOW API login)
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // 🔹 Otherwise, go to MembershipLoginActivity
            intent = new Intent(SplashActivity.this, MembershipLoginActivity.class);
        }

        startActivity(intent);
        finish(); // 🔹 Close SplashActivity to prevent going back
    }
}

