package com.kinvo.easyinventory;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class UserAgreementActivity extends AppCompatActivity {

    private static final String TAG = "UserAgreementActivity";
    private static final String USER_AGREEMENT_FILE = "file:///android_asset/user_agreement.html";
    private static final String USER_AGREEMENT_TITLE = "User Agreement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_agreement);

        // ✅ Setup Toolbar
        setupToolbar();

        // ✅ Load User Agreement
        loadUserAgreement();

    }

    /**
     * Sets up the Toolbar with a title and back button.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(USER_AGREEMENT_TITLE);
        } else {
            Log.e(TAG, "Support action bar is null");
        }
    }

    /**
     * Loads the User Agreement HTML file into the WebView.
     */
    private void loadUserAgreement() {
        WebView webView = findViewById(R.id.webView);
        if (webView == null) {
            Log.e(TAG, "WebView is null");
            return;
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false); // 🔹 Keep JS disabled unless necessary
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // ✅ Try loading the User Agreement file with error handling
        try {
            webView.loadUrl(USER_AGREEMENT_FILE);
            Log.d(TAG, "Loaded User Agreement successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading the user agreement file: " + e.getMessage());
        }
    }

    /**
     * Handles the back button in the Toolbar.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Closes activity and returns to the previous screen
        return true;
    }
}
