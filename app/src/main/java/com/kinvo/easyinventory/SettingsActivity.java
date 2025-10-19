package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // simple app prefs just for theme toggle
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();              // apply before super for smoother transition
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);  // matches your XML


        // Toolbar (matches @id/toolbarSettings in your XML)
        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }
        // Back arrow click
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Theme switch
        SwitchCompat switchTheme = findViewById(R.id.switchTheme);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        switchTheme.setChecked(isDark);
        switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Text links
        TextView tvUserAgreement  = findViewById(R.id.tvUserAgreement);
        TextView tvPrivacyPolicy  = findViewById(R.id.tvPrivacyPolicy);
        TextView tvTermsOfService = findViewById(R.id.tvTermsOfService);
        TextView tvAbout          = findViewById(R.id.tvAbout);

        tvUserAgreement.setOnClickListener(v ->
                startActivity(new Intent(this, UserAgreementActivity.class)));
        tvPrivacyPolicy.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        tvTermsOfService.setOnClickListener(v ->
                startActivity(new Intent(this, TermsOfServiceActivity.class)));
        tvAbout.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        // Logout button (matches @id/buttonLogout in your XML)
        Button btnLogout = findViewById(R.id.buttonLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(this::onClickLogout);
        }
    }

    // XML android:onClick="onClickLogout" also points here
    public void onClickLogout(View view) {
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            prefs.clearAll();                      // clear encrypted creds
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear SecurePrefs on logout", e);
        }
        // Return to splash so it decides the right first screen
        Intent i = new Intent(this, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applySavedTheme() {
        boolean isDark = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
