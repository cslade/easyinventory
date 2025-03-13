package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etApiKey, etApiSecret, etLocationId;
    private Button btnAuthenticate;
    private ProgressBar progressBar;
    private CheckBox checkboxRememberMe; // ✅ Added Remember Me checkbox

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "MyAppPrefs";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // ✅ Make sure this is the correct layout

        // ✅ Initialize UI Elements
        etApiKey = findViewById(R.id.etApiKey);
        etApiSecret = findViewById(R.id.etApiSecret);
        etLocationId = findViewById(R.id.etLocationId);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);
        progressBar = findViewById(R.id.progressBar);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMeAPI); // ✅ Initialize checkbox

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // ✅ Load saved credentials if "Remember Me" was checked
        loadSavedCredentials();

        // ✅ Set Click Listener
        btnAuthenticate.setOnClickListener(v -> authenticateUser());
    }

    private void authenticateUser() {
        String apiKey = etApiKey.getText().toString().trim();
        String apiSecret = etApiSecret.getText().toString().trim();
        String locationIdString = etLocationId.getText().toString().trim();

        Log.d(TAG, "Entered API Key: " + apiKey);
        Log.d(TAG, "Entered API Secret: " + apiSecret);
        Log.d(TAG, "Entered Location ID: " + locationIdString);

        if (apiKey.isEmpty() || apiSecret.isEmpty() || locationIdString.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        int locationId;
        try {
            locationId = Integer.parseInt(locationIdString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Location ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Generate Base64 authToken using API Key & Secret
        String credentials = apiKey + ":" + apiSecret;
        String authToken = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        Log.d(TAG, "Generated Auth Token: " + authToken);  // Debugging

        // ✅ Store authToken and locationId in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("authToken", authToken);
        editor.putInt("locationId", locationId);
        editor.apply();

        Log.d(TAG, "Saved Auth Token: " + authToken);
        Log.d(TAG, "Saved Location ID: " + locationId);

        navigateToProductSearch();
    }

    private void navigateToProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    // ✅ Save API credentials only if "Remember Me" is checked
    private void saveCredentials(String apiKey, String apiSecret, String locationId, boolean rememberMe) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (rememberMe) {
            editor.putString("apiKey", apiKey);
            editor.putString("apiSecret", apiSecret);
            editor.putString("locationId", locationId);
            editor.putBoolean("rememberMe", true);
            Log.d(TAG, "✅ Saving API Key: " + apiKey);
            Log.d(TAG, "✅ Saving API Secret: " + apiSecret);
            Log.d(TAG, "✅ Saving Location ID: " + locationId);
        } else {
            editor.clear();
            Log.d(TAG, "❌ Clearing saved credentials.");
        }
        editor.apply();
    }


    // ✅ Load saved credentials if "Remember Me" was checked
    private void loadSavedCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean("rememberMe", false);
        if (rememberMe) {
            etApiKey.setText(sharedPreferences.getString("apiKey", ""));
            etApiSecret.setText(sharedPreferences.getString("apiSecret", ""));
            etLocationId.setText(sharedPreferences.getString("locationId", ""));
            checkboxRememberMe.setChecked(true);
        }
    }
}
