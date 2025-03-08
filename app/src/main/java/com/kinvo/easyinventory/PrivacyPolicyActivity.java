package com.kinvo.easyinventory;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;

public class PrivacyPolicyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // ✅ Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ✅ Enable Back Button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Privacy Policy");
        }

        // ✅ Load Privacy Policy Content
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        tvPrivacyPolicy.setText(getPrivacyPolicyText());
    }

    // ✅ Handle Back Button Click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // ✅ Closes the activity when back arrow is clicked
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ Privacy Policy Content (Can be replaced with content from a file)
    private String getPrivacyPolicyText() {
        return "Privacy Policy\n\n" +
                "This Privacy Policy explains how we collect, use, and protect your information.\n\n" +
                "1. **Data Collection**: We collect minimal data necessary for app functionality.\n" +
                "2. **Data Usage**: Your data is not shared with third parties.\n" +
                "3. **Security**: We use industry-standard security measures.\n" +
                "4. **Your Rights**: You can request to delete your data at any time.\n\n" +
                "For full details, please visit our website.";
    }
}






