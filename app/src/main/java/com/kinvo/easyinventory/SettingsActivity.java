package com.kinvo.easyinventory;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchTheme;
    private SharedPreferences appPrefs;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ Apply the saved theme before setting content view (Prevents flickering)
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ✅ Setup Toolbar with Back Button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // ✅ Initialize UI
        switchTheme = findViewById(R.id.switchTheme);
        appPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // ✅ Load saved theme preference
        boolean isDarkModeEnabled = appPrefs.getBoolean(KEY_DARK_MODE, false);
        switchTheme.setChecked(isDarkModeEnabled);

        // ✅ Listen for theme switch toggle
        switchTheme.setOnCheckedChangeListener(this::onThemeToggle);
    }

    // ✅ Handle Back Button Click
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Closes activity and returns to the previous screen
        return true;
    }

    // ✅ Theme Toggle Listener
    private void onThemeToggle(CompoundButton buttonView, boolean isChecked) {
        saveThemePreference(isChecked);
        applyTheme(isChecked);
    }

    // ✅ Save Theme Preference
    private void saveThemePreference(boolean isDarkModeEnabled) {
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putBoolean(KEY_DARK_MODE, isDarkModeEnabled);
        editor.apply();
    }

    // ✅ Apply Theme (No need to recreate activity)
    private void applyTheme(boolean isDarkModeEnabled) {
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    // ✅ Apply Saved Theme Before Creating Activity
    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
