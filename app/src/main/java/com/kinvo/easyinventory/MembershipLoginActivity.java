package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

        // Launch the WebView for authentication
        Intent intent = new Intent(MembershipLoginActivity.this, WebViewLoginActivity.class);
        startActivity(intent);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

