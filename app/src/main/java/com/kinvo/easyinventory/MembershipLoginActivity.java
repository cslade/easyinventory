package com.kinvo.easyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MembershipLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership_login);

        // ✅ Initialize UI Elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        // ✅ Set Login Button Click Listener
        btnLogin.setOnClickListener(v -> authenticateUser());
    }

    private void authenticateUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Show Progress Bar
        progressBar.setVisibility(View.VISIBLE);

        // 🔹 Simulated Authentication (Replace with real authentication logic)
        btnLogin.postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            if (email.equals("test@easyinventory.com") && password.equals("password123")) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                // ✅ Redirect to LoginActivity instead of MainActivity
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();  // Close MembershipLoginActivity
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        }, 2000); // Simulate network delay
    }
}
