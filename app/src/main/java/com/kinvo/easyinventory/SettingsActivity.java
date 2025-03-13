package com.kinvo.easyinventory;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;

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
        applySavedTheme(); // ✅ Ensure dark mode is applied before setting layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ✅ Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // ✅ Use SwitchCompat instead of SwitchMaterial
        SwitchCompat switchTheme = findViewById(R.id.switchTheme);
        appPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isDarkModeEnabled = appPrefs.getBoolean(KEY_DARK_MODE, false);
        switchTheme.setChecked(isDarkModeEnabled);

        // ✅ Listen for theme toggle
        switchTheme.setOnCheckedChangeListener(this::onThemeToggle);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // ✅ Closes activity and returns to the previous screen
        return true;
    }

    private void onThemeToggle(CompoundButton buttonView, boolean isChecked) {
        saveThemePreference(isChecked);
        applyTheme(isChecked);
    }

    private void saveThemePreference(boolean isDarkModeEnabled) {
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putBoolean(KEY_DARK_MODE, isDarkModeEnabled);
        editor.apply();
    }

    private void applyTheme(boolean isDarkModeEnabled) {
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
