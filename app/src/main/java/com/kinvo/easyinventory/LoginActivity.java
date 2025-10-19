package com.kinvo.easyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.nio.charset.StandardCharsets;

/**
 * Collects API Key, Secret, and Location ID, persists them via SecurePrefs,
 * and navigates to ProductSearchActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etApiKey;
    private EditText etApiSecret;
    private EditText etLocationId;
    private SwitchCompat switchRemember;
    private Button btnContinue;

    private SecurePrefs prefs; // Encrypted helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // ensure this layout has the expected IDs

        etApiKey = findViewById(R.id.inputApiKey);
        etApiSecret = findViewById(R.id.inputApiSecret);
        etLocationId = findViewById(R.id.inputLocationId);
        switchRemember = findViewById(R.id.switchRememberKeys);
        btnContinue = findViewById(R.id.buttonContinue);

        try {
            prefs = SecurePrefs.get(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init SecurePrefs", e);
            Toast.makeText(this, "Storage init failed. Please restart the app.", Toast.LENGTH_LONG).show();
            // If we can’t initialize secure storage, there’s no point continuing.
            finish();
            return;
        }

        // Pre-fill from secure prefs if user chose "remember"
        boolean rememberApi = prefs.getRememberApi();
        String savedKey = prefs.getApiKey();
        String savedSecret = prefs.getApiSecret();
        int savedLocation = prefs.getLocationId();

        Log.d(TAG, "onCreate -> remember=" + rememberApi
                + ", apiKey=" + mask(savedKey)
                + ", apiSecret=" + mask(savedSecret)
                + ", locationId=" + savedLocation);

        if (rememberApi) {
            if (!TextUtils.isEmpty(savedKey)) etApiKey.setText(savedKey);
            if (!TextUtils.isEmpty(savedSecret)) etApiSecret.setText(savedSecret);
            if (savedLocation > 0) etLocationId.setText(String.valueOf(savedLocation));
            if (switchRemember != null) switchRemember.setChecked(true);
        }

        btnContinue.setOnClickListener(v -> onContinue());
    }

    private void onContinue() {
        hideKeyboard();

        String apiKey = safeTrim(etApiKey.getText());
        String apiSecret = safeTrim(etApiSecret.getText());
        String locationStr = safeTrim(etLocationId.getText());
        boolean remember = switchRemember != null && switchRemember.isChecked();

        Log.d(TAG, "onContinue -> remember=" + remember
                + ", apiKey=" + mask(apiKey)
                + ", apiSecret=" + mask(apiSecret)
                + ", locationStr=" + locationStr);

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(apiSecret)) {
            Toast.makeText(this, "Please enter API Key and Secret", Toast.LENGTH_SHORT).show();
            return;
        }

        int locationId = parseLocation(locationStr);
        if (locationId <= 0) {
            Toast.makeText(this, "Please enter a valid Location ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String basicHeader = buildBasicHeader(apiKey, apiSecret);
        Log.d(TAG, "Built Basic header (masked): " + maskHeader(basicHeader));

        // Persist securely
        // If "remember" is OFF, we still keep location + header but clear key/secret.
        prefs.setApiKey(remember ? apiKey : "");
        prefs.setApiSecret(remember ? apiSecret : "");
        prefs.setLocationId(locationId);
        prefs.setAuthHeaderBasic(basicHeader);
        prefs.setRememberApi(remember);

        Log.d(TAG, "Saved to SecurePrefs -> remember=" + remember
                + ", locationId=" + locationId
                + ", apiKey=" + mask(apiKey));

        // Navigate to ProductSearchActivity (it should read everything from SecurePrefs)
        Intent next = new Intent(this, ProductSearchActivity.class);
        startActivity(next);
        finish();
    }

    private static String buildBasicHeader(String key, String secret) {
        String combo = key + ":" + secret;
        String enc = Base64.encodeToString(combo.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return "Basic " + enc;
    }

    private static int parseLocation(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String safeTrim(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) v.clearFocus();
    }

    // Mask helpers for safe logging
    private static String mask(String s) {
        if (TextUtils.isEmpty(s)) return "";
        if (s.length() <= 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }

    private static String maskHeader(String h) {
        if (TextUtils.isEmpty(h)) return "";
        int idx = h.indexOf(' ');
        if (idx < 0 || idx == h.length() - 1) return "Basic ****";
        return h.substring(0, idx + 1) + "****";
    }
}
