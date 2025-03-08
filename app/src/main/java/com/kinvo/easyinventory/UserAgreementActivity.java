package com.kinvo.easyinventory;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class UserAgreementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_agreement);

        // ✅ Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ✅ Enable Back Button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Agreement");
        }

        // ✅ Set User Agreement Text
        TextView tvUserAgreement = findViewById(R.id.tvUserAgreementContent);
        tvUserAgreement.setText(getUserAgreementText());
    }

    // ✅ Handle Back Button Click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ User Agreement Content (Can be replaced with content from a file)
    private String getUserAgreementText() {
        return "User Agreement\n\n" +
                "Welcome to EasyInventory! By using this app, you agree to the following terms:\n\n" +
                "1. You will not misuse this service.\n" +
                "2. You acknowledge that data accuracy is your responsibility.\n" +
                "3. We do not share your personal data with third parties.\n\n" +
                "For full terms, visit our website.";
    }
}


