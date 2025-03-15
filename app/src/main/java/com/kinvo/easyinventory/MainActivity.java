package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private Button btnLogout;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        btnLogout = findViewById(R.id.btnLogout);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Simulate loading process
        new android.os.Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        }, 2000);

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isLoggedIn");
        editor.apply();

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, MembershipLoginActivity.class));
        finish();
    }
}

