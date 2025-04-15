package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class WebViewLoginActivity extends AppCompatActivity {

    private static final String BASIC_URL = "https://www.easyinventory.io/basic/account";
    private static final String PREMIUM_URL = "https://www.easyinventory.io/premium/account";
    private static final String LOGIN_URL = "https://www.easyinventory.io/login";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        // Launch login in Chrome Custom Tabs
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(this, Uri.parse(LOGIN_URL));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("WebViewLogin", "onResume triggered");
        Uri data = getIntent().getData();
        if (data != null) {
            String url = data.toString();
            Log.d("WebViewLogin", "Intent data: " + url);
            if (url.contains("basic/account")) {
                saveMembershipType("Basic");
                proceedToLoginActivity();
            } else if (url.contains("premium/account")) {
                saveMembershipType("Premium");
                proceedToLoginActivity();
            } else if (url.contains("account") || url.contains("callback")) {
                Toast.makeText(this, "Redirected from login", Toast.LENGTH_SHORT).show();
                saveMembershipType("Authenticated");
                proceedToLoginActivity();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Allow onResume to pick up the updated intent
    }

    private void saveMembershipType(String membershipType) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("membershipType", membershipType);
        editor.apply();
    }

    private void proceedToLoginActivity() {
        Intent intent = new Intent(WebViewLoginActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}




