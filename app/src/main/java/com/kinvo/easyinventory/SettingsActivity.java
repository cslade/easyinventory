package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
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
        applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        SwitchCompat switchTheme = findViewById(R.id.switchTheme);
        appPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isDarkModeEnabled = appPrefs.getBoolean(KEY_DARK_MODE, false);
        switchTheme.setChecked(isDarkModeEnabled);

        switchTheme.setOnCheckedChangeListener(this::onThemeToggle);

        TextView tvUserAgreement = findViewById(R.id.tvUserAgreement);
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        TextView tvAbout = findViewById(R.id.tvAbout);
        TextView tvTermsOfService = findViewById(R.id.tvTermsOfService);

        tvUserAgreement.setOnClickListener(v -> startActivity(new Intent(this, UserAgreementActivity.class)));
        tvPrivacyPolicy.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        tvAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        tvTermsOfService.setOnClickListener(v -> startActivity(new Intent(this, TermsOfServiceActivity.class)));

        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());
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

    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, MembershipLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
