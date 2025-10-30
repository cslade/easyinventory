package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;


public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // Simple app prefs just for theme toggle
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    // Views
    private SwitchCompat switchTheme;

    // Secure prefs
    private SecurePrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();                 // apply before super for smoother transition
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = SecurePrefs.get(this);

        // Toolbar (must exist in XML as @id/toolbarSettings)
        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());



        // Theme switch
        switchTheme = findViewById(R.id.switchTheme);
        SharedPreferences appPrefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        boolean isDark = appPrefs.getBoolean(KEY_DARK_MODE, false);
        switchTheme.setChecked(isDark);
        switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
            appPrefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
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

        // Logout
        Button btnLogout = findViewById(R.id.buttonLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(this::onClickLogout);
        }
    }

    // Logout clears secure prefs and returns to Splash
    public void onClickLogout(View v) {
        try {
            SecurePrefs p = SecurePrefs.get(this);
            try {
                p.clearAll();
            } catch (Throwable ignored) {
                p.setApiKey(null);
                p.setApiSecret(null);
                p.setLocationId(0);
                p.setProvider((com.kinvo.easyinventory.data.DataSource) null); // ← fixed
                p.setProviderName(null);
            }
        } catch (Throwable ignored) {}

        Intent i = new Intent(this, MembershipLoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }



    private void applySavedTheme() {
        boolean isDark = getSharedPreferences(APP_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private static String safeText(EditText e) {
        return (e == null || e.getText() == null) ? "" : e.getText().toString().trim();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
