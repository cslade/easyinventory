package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etApiKey, etApiSecret, etLocationId;
    private Button btnAuthenticate;
    private ProgressBar progressBar;

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "MyAppPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // ✅ Make sure this is the correct layout

        // ✅ Initialize UI Elements
        etApiKey = findViewById(R.id.etApiKey);
        etApiSecret = findViewById(R.id.etApiSecret);
        etLocationId = findViewById(R.id.etLocationId);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);
        progressBar = findViewById(R.id.progressBar);  // ✅ Initialize ProgressBar

        // ✅ Check if ProgressBar is Null (Debugging)
        if (progressBar == null) {
            Log.e("LoginActivity", "❌ ProgressBar is NULL! Check activity_login.xml.");
        }

        // ✅ Set Click Listener
        btnAuthenticate.setOnClickListener(v -> authenticateUser());
    }


    private void authenticateUser() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE); // ✅ Show ProgressBar
        } else {
            Log.e("LoginActivity", "❌ ProgressBar is NULL! Skipping visibility change.");
        }

        // Simulated authentication process
        boolean isAuthenticated = true; // Simulate authentication success

        if (isAuthenticated) {
            navigateToProductSearch();
        } else {
            Toast.makeText(this, "Authentication failed!", Toast.LENGTH_SHORT).show();
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE); // ✅ Hide ProgressBar
        }
    }


    private void navigateToProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Close LoginActivity
    }
}
