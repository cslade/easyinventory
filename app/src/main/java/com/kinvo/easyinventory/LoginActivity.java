package com.kinvo.easyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.kinvo.easyinventory.data.DataSource;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Shared ids across provider layouts
    private EditText etApiKey;        // EPOS api key
    private EditText etApiSecret;     // EPOS secret  | CLOVER token (login_clover.xml)
    private EditText etLocationId;    // EPOS location| CLOVER merchantId (login_clover.xml)

    // Shopify-specific
    private EditText etShopifyDomain;       // inputShopifyDomain
    private EditText etShopifyAccessToken;  // inputShopifyAccessToken

    private SwitchCompat switchRemember; // switchRememberKeys (present on each login_*.xml)
    private Button btnContinue;

    private DataSource provider = DataSource.EPOSNOW; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Resolve provider from prefs
        try {
            DataSource ds = SecurePrefs.get(this).getProvider();
            if (ds != null) provider = ds;
        } catch (Exception ignored) {}

        // Choose provider-specific layout
        int layoutRes;
        switch (provider) {
            case SHOPIFY: layoutRes = R.layout.activity_login_shopify; break;
            case CLOVER:  layoutRes = R.layout.activity_login_clover;  break;
            case EPOSNOW:
            default:      layoutRes = R.layout.activity_login_eposnow; break;
        }
        setContentView(layoutRes);

        // Bind common views (present on every provider layout)
        etApiKey     = findViewById(R.id.inputApiKey);
        etApiSecret  = findViewById(R.id.inputApiSecret);
        etLocationId = findViewById(R.id.inputLocationId);
        switchRemember = findViewById(R.id.switchRememberKeys);
        btnContinue    = findViewById(R.id.buttonContinue);

// Bind Shopify-only views ONLY when the Shopify layout is active
        if (provider == DataSource.SHOPIFY) {
            etShopifyDomain      = findViewById(R.id.inputShopifyDomain);
            etShopifyAccessToken = findViewById(R.id.inputShopifyAccessToken);
        } else {
            etShopifyDomain = null;
            etShopifyAccessToken = null;
        }


        // Prefill from prefs ONLY when "remember" is ON
        SecurePrefs p = SecurePrefs.get(this);
        boolean remember = false;
        try { remember = p.getRememberApi(); } catch (Exception ignored) {}
        if (switchRemember != null) switchRemember.setChecked(remember);

        try {
            switch (provider) {
                case SHOPIFY:
                    if (remember) {
                        if (etShopifyAccessToken != null) etShopifyAccessToken.setText(nz(p.getShopToken()));
                        if (etShopifyDomain != null)      etShopifyDomain.setText(nz(p.getShopDomain()));
                    }
                    break;

                case CLOVER:
                    // login_clover.xml reuses:
                    //  - inputApiSecret   -> Clover API Token
                    //  - inputLocationId  -> Merchant ID
                    if (remember) {
                        if (etApiSecret != null)   etApiSecret.setText(nz(p.getCloverAccessToken()));
                        if (etLocationId != null)  etLocationId.setText(nz(p.getCloverMerchantId()));
                    }
                    break;

                case EPOSNOW:
                default:
                    if (remember) {
                        if (etApiKey != null)     etApiKey.setText(nz(p.getApiKey()));
                        if (etApiSecret != null)  etApiSecret.setText(nz(p.getApiSecret()));
                        if (etLocationId != null) {
                            int loc = p.getLocationId();
                            etLocationId.setText(loc > 0 ? String.valueOf(loc) : "");
                        }
                    }
                    break;
            }
        } catch (Exception ignored) {}

        if (btnContinue != null) btnContinue.setOnClickListener(v -> onClickContinue());
        // Optional "Change provider" action if present in your layout
        if (findViewById(R.id.linkChangeProvider) != null) {
            findViewById(R.id.linkChangeProvider).setOnClickListener(v -> changeProvider());
        }
    }

    private void changeProvider() {
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            // Only clear provider choice; let remember flag & creds remain as-is
            // so if the user returns to the same provider and "remember" is on, we can prefill.
            prefs.setProvider((DataSource) null);
        } catch (Exception ignored) {}

        startActivity(new Intent(this, ProviderPickerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private void onClickContinue() {
        SecurePrefs p = SecurePrefs.get(this);
        boolean remember = (switchRemember != null && switchRemember.isChecked());
        p.setRememberApi(remember); // <-- persist the toggle itself

        try {
            switch (provider) {
                case SHOPIFY: {
                    String token  = etShopifyAccessToken != null ? etShopifyAccessToken.getText().toString().trim() : "";
                    String domain = etShopifyDomain      != null ? etShopifyDomain.getText().toString().trim()      : "";

                    if (TextUtils.isEmpty(token) || TextUtils.isEmpty(domain)) {
                        toast("Enter Shopify Admin API access token and store domain");
                        return;
                    }
                    domain = normalizeDomain(domain);

                    // Always save so downstream screens (repositories) can read.
                    p.setShopToken(token);
                    p.setShopDomain(domain);

                    // Mark current provider
                    p.setProvider(DataSource.SHOPIFY);
                    break;
                }

                case CLOVER: {
                    // Reused fields:
                    //  - inputApiSecret  -> token
                    //  - inputLocationId -> merchant id
                    String token      = etApiSecret   != null ? etApiSecret.getText().toString().trim()   : "";
                    String merchantId = etLocationId  != null ? etLocationId.getText().toString().trim()  : "";

                    if (TextUtils.isEmpty(token) || TextUtils.isEmpty(merchantId)) {
                        toast("Enter Clover API Token and Merchant ID");
                        return;
                    }

                    p.setCloverAccessToken(token);
                    p.setCloverMerchantId(merchantId);
                    p.setProvider(DataSource.CLOVER);
                    break;
                }

                case EPOSNOW:
                default: {
                    String key  = etApiKey    != null ? etApiKey.getText().toString().trim()    : "";
                    String sec  = etApiSecret != null ? etApiSecret.getText().toString().trim() : "";
                    String locS = etLocationId!= null ? etLocationId.getText().toString().trim(): "";

                    if (TextUtils.isEmpty(key) || TextUtils.isEmpty(sec)) {
                        toast("Please enter API key and secret");
                        return;
                    }
                    int loc = 0;
                    try { loc = TextUtils.isEmpty(locS) ? 0 : Integer.parseInt(locS); } catch (Exception ignored) {}

                    p.setApiKey(key);
                    p.setApiSecret(sec);
                    p.setLocationId(loc);
                    p.setProvider(DataSource.EPOSNOW);
                    break;
                }
            }

            // Navigate to search once creds saved
            startActivity(new Intent(this, ProductSearchActivity.class));
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Login error", e);
            toast("Login failed");
        }
    }

    // --- utils ---

    private static String normalizeDomain(String raw) {
        if (raw == null) return "";
        String d = raw.trim().toLowerCase(Locale.ROOT);
        if (d.startsWith("https://")) d = d.substring(8);
        if (d.startsWith("http://"))  d = d.substring(7);
        if (!d.contains(".")) d = d + ".myshopify.com";
        return d;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
