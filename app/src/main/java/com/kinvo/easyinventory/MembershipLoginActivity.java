package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class MembershipLoginActivity extends AppCompatActivity {

    private Button btnLogin;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership_login);

        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(view -> authenticateUser());

        tvSignUp.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.easyinventory.io/signup"));
            startActivity(browserIntent);
        });
    }

    private void authenticateUser() {
        progressBar.setVisibility(View.VISIBLE);

        String loginUrl = "https://www.easyinventory.io/login";  // your Memberstack login page

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();

        customTabsIntent.intent.setPackage("com.android.chrome");
        customTabsIntent.intent.putExtra("android.support.customtabs.extra.EXTRA_ENABLE_URLBAR_HIDING", true);
        customTabsIntent.intent.putExtra("android.support.customtabs.extra.EXTRA_TITLE_VISIBILITY_STATE", 1);
        customTabsIntent.launchUrl(this, Uri.parse(loginUrl));

        // ✅ After login attempt, open WebViewLoginActivity to detect success
        new android.os.Handler().postDelayed(() -> {
            startActivity(new Intent(this, WebViewLoginActivity.class));
            finish();
        }, 10000);

        progressBar.setVisibility(View.GONE);
    }

}

