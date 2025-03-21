package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;


import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences appPrefs;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the saved theme before setting content view
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup Toolbar with Back Button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // Initialize UI
        SwitchCompat switchTheme = findViewById(R.id.switchTheme);
        appPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved theme preference
        boolean isDarkModeEnabled = appPrefs.getBoolean(KEY_DARK_MODE, false);
        switchTheme.setChecked(isDarkModeEnabled);

        // Listen for theme switch toggle
        switchTheme.setOnCheckedChangeListener(this::onThemeToggle);

        // Link to views
        TextView tvUserAgreement = findViewById(R.id.tvUserAgreement);
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        TextView tvAbout = findViewById(R.id.tvAbout);

        // User Agreement
        tvUserAgreement.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UserAgreementActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Privacy Policy
        tvPrivacyPolicy.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, PrivacyPolicyActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // About
        tvAbout.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Logout Button
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    // Handle Theme Toggle
    private void onThemeToggle(CompoundButton buttonView, boolean isChecked) {
        saveThemePreference(isChecked);
        applyTheme(isChecked);
    }

    // Save Theme Preference
    private void saveThemePreference(boolean isDarkModeEnabled) {
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putBoolean(KEY_DARK_MODE, isDarkModeEnabled);
        editor.apply();
    }

    // Apply Theme Dynamically
    private void applyTheme(boolean isDarkModeEnabled) {
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    // Apply Saved Theme Before Creating Activity
    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    // Logout User and Redirect to Membership Login
    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.remove("authToken");
        editor.remove("locationId");
        editor.apply();

        Intent intent = new Intent(SettingsActivity.this, MembershipLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
