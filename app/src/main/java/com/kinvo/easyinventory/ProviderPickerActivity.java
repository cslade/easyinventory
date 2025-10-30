package com.kinvo.easyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.kinvo.easyinventory.data.Provider;

public class ProviderPickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_picker);

        Button btnEpos = findViewById(R.id.btnProviderEpos);
        Button btnShop = findViewById(R.id.btnProviderShopify);
        Button btnClover = findViewById(R.id.btnProviderClover); // add in XML if you support Clover

        btnEpos.setOnClickListener(v -> select(Provider.EPOSNOW));
        btnShop.setOnClickListener(v -> select(Provider.SHOPIFY));
        if (btnClover != null) {
            btnClover.setOnClickListener(v -> select(Provider.CLOVER));
        }
    }

    private void select(Provider p) {
        // Save the user’s selection
        SecurePrefs prefs = SecurePrefs.get(this);
        prefs.setProvider(p);

        // Go straight to provider-specific credential screen
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
