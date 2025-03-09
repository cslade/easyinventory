package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private EditText etApiKey;
    private EditText etApiSecret;
    private EditText etLocationId;
    private Button btnAuthenticate;
    private Button btnGoToSearch;

    private String authToken;
    private String locationId;

    // Constants for intent extras and shared preferences
    private static final String AUTH_TOKEN_EXTRA = "AUTH_TOKEN";
    private static final String LOCATION_ID_EXTRA = "LOCATION_ID";
    private static final String DARK_MODE_PREF = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPreferences();  // ✅ Apply dark/light mode before setting content view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        setupClickListeners();
        loadStoredAuthToken();
    }

    // ✅ Initialize UI elements
    private void initializeUI() {
        etApiKey = findViewById(R.id.etApiKey);
        etApiSecret = findViewById(R.id.etApiSecret);
        etLocationId = findViewById(R.id.etLocationId);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);
        btnGoToSearch = findViewById(R.id.btnSearchProduct);
    }

    // ✅ Set up button click listeners
    private void setupClickListeners() {
        btnAuthenticate.setOnClickListener(view -> authenticate());

        btnGoToSearch.setOnClickListener(view -> {
            if (isUserAuthenticated()) {
                navigateToProductSearch();
            } else {
                showAuthenticationRequiredToast();
            }
        });
    }

    // ✅ Load stored authentication token (if exists)
    private void loadStoredAuthToken() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        authToken = prefs.getString(AUTH_TOKEN_EXTRA, "");
        locationId = prefs.getString(LOCATION_ID_EXTRA, "");
    }

    // ✅ Check if user is authenticated
    private boolean isUserAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    // ✅ Navigate to Product Search Activity
    private void navigateToProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(AUTH_TOKEN_EXTRA, authToken);
        intent.putExtra(LOCATION_ID_EXTRA, locationId);
        startActivity(intent);
    }

    // ✅ Show authentication required message
    private void showAuthenticationRequiredToast() {
        Toast.makeText(this, "Authenticate first!", Toast.LENGTH_SHORT).show();
    }

    // ✅ Authenticate user and store credentials
    private void authenticate() {
        String apiKey = etApiKey.getText().toString().trim();
        String apiSecret = etApiSecret.getText().toString().trim();
        locationId = etLocationId.getText().toString().trim();

        if (apiKey.isEmpty() || apiSecret.isEmpty() || locationId.isEmpty()) {
            Toast.makeText(this, "Please enter all credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate API authentication response
        authToken = "SIMULATED_AUTH_TOKEN_" + apiKey;  // ✅ Simulated authentication logic

        // Store token and locationId in SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AUTH_TOKEN_EXTRA, authToken);
        editor.putString(LOCATION_ID_EXTRA, locationId);
        editor.apply();

        Toast.makeText(this, "Authentication Successful!", Toast.LENGTH_SHORT).show();
    }

    // ✅ Apply Dark or Light Mode from User Preferences
    private void applyThemeFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean(DARK_MODE_PREF, false);

        // Apply Theme
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}

