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

public class MembershipLoginActivity extends AppCompatActivity {

    private static final String PREFS_USER = "UserPrefs";
    private static final String KEY_MEMBERSHIP_OK = "membershipOk";

    private Button btnLogin;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already verified, skip straight to API creds screen
        SharedPreferences sp = getSharedPreferences(PREFS_USER, MODE_PRIVATE);
        if (sp.getBoolean(KEY_MEMBERSHIP_OK, false)) {
            startActivity(new Intent(this, ProviderPickerActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_membership_login);

        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        sharedPreferences = getSharedPreferences(PREFS_USER, MODE_PRIVATE);

        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(view -> authenticateUser());

        tvSignUp.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.easyinventory.io/signup"));
            startActivity(browserIntent);
        });
    }

    private void authenticateUser() {
        progressBar.setVisibility(View.VISIBLE);

        Uri loginUri = Uri.parse(BuildConfig.AUTH_BASE_URL).buildUpon().appendPath("login").build();

        Intent intent = new Intent(this, WebViewLoginActivity.class);
        intent.putExtra("authUrl", loginUri.toString());
        startActivity(intent);
        // Keep this Activity so back returns here if the user cancels auth
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Hide spinner when returning from WebView
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        // If the WebView completed successfully, membershipOk will be set there;
        // you could auto-forward here if you prefer:
        boolean ok = getSharedPreferences(PREFS_USER, MODE_PRIVATE).getBoolean(KEY_MEMBERSHIP_OK, false);
        if (ok) {
            startActivity(new Intent(this, ProviderPickerActivity.class));
            finish();
        }
    }
}
