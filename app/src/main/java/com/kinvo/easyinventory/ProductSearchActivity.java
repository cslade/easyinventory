package com.kinvo.easyinventory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.kinvo.easyinventory.adapters.ProductAdapter;
import com.kinvo.easyinventory.model.Product;
import com.kinvo.easyinventory.print.LabelData;
import com.kinvo.easyinventory.print.LabelPrinter;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private String lastThemeMode;

    private static final String TAG = "ProductSearchActivity";

    // Legacy keys for back-compat reads
    private static final String LEGACY_PREFS = "MyAppPrefs";
    private static final String LEGACY_KEY_TOKEN = "authToken";
    private static final String LEGACY_KEY_LOCATION = "locationId";

    // Modern keys used by intents/secure prefs
    private static final String KEY_AUTH_HEADER_BASIC = "authHeaderBasic";
    private static final String KEY_API_KEY = "apiKey";
    private static final String KEY_API_SECRET = "apiSecret";
    private static final String KEY_LOCATION_ID = "locationId";


    // Views
    private EditText etSearch;
    private Button btnSearch;
    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;

    // Data / UI
    private final List<Product> productList = new ArrayList<>();
    private ProductAdapter productAdapter;
    private Product selectedProduct; // used by the Print action

    // Networking
    private RequestQueue requestQueue;

    // Resolved creds
    private String authHeader;     // "Basic <base64>"
    private int locationId;

    // Secure prefs
    private SecurePrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lastThemeMode = ThemeManager.getMode(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Inventory Update");
        }

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        recyclerView = findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestQueue = Volley.newRequestQueue(this);
        prefs = SecurePrefs.get(this);

        // Resolve creds before adapter
        authHeader = resolveAuthHeader();
        locationId = resolveLocationId();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "authHeader present? " + (authHeader != null) + ", locationId=" + locationId);
        }

        productAdapter = new ProductAdapter(this, productList, authHeader, locationId);
        recyclerView.setAdapter(productAdapter);

        // Selection -> used by the Print toolbar item
        productAdapter.setOnItemClickListener(item -> {
            selectedProduct = item;
            Toast.makeText(this, "Selected: " + item.getDescription(), Toast.LENGTH_SHORT).show();
        });

        // Guard rails
        if (authHeader == null || locationId <= 0) {
            Toast.makeText(this, "Missing credentials. Please log in and set Location.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Old behavior preserved
        btnSearch.setOnClickListener(this::onClickSearch);
        etSearch.setOnEditorActionListener((tv, actionId, keyEvent) -> {
            onClickSearch(tv);
            return true;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        String now = ThemeManager.getMode(this);
        if (!now.equals(lastThemeMode)) {
            lastThemeMode = now;
            recreate();
        }
    }

    // Hook for android:onClick="onClickSearch"
    public void onClickSearch(View v) {
        String query = safeText(etSearch);
        if (locationId <= 0) {
            Toast.makeText(this, "No location selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        fadeInProgressBar();
        searchProducts(query);
        etSearch.setText("");
        etSearch.clearFocus();
        hideKeyboard();
        Toast.makeText(this, "Searching…", Toast.LENGTH_SHORT).show();
    }

    private void searchProducts(String query) {
        progressBar.setVisibility(View.VISIBLE);

        Uri.Builder b = Uri.parse("https://api.eposnowhq.com/api/v4/Inventory/stocks")
                .buildUpon()
                .appendQueryParameter("LocationID", String.valueOf(locationId));
        if (!TextUtils.isEmpty(query)) {
            b.appendQueryParameter("Search", query);
        }
        String url = b.build().toString();
        if (BuildConfig.DEBUG) Log.d(TAG, "Request URL: " + url);

        // Your existing request wrapper
        ProductRequest req = new ProductRequest(
                this,
                productList,
                productAdapter,
                progressBar,
                authHeader,
                recyclerView,
                emptyView
        );
        requestQueue.add(req.createRequest(url));
    }

    // ---------- Credential resolution ----------

    private String resolveAuthHeader() {
        // 1) Intent extras
        Intent i = getIntent();
        if (i != null) {
            String fromExtra = firstNonEmpty(
                    i.getStringExtra(KEY_AUTH_HEADER_BASIC),
                    i.getStringExtra(LEGACY_KEY_TOKEN),
                    i.getStringExtra("token")
            );
            if (!isNullOrEmpty(fromExtra)) return normalizeBasic(fromExtra);
        }

        // 2) SecurePrefs
        String basic = prefs.getAuthHeaderBasic();
        if (!isNullOrEmpty(basic)) return normalizeBasic(basic);

        // Try to build from API key/secret if present
        String apiKey = prefs.getApiKey();
        String apiSecret = prefs.getApiSecret();
        if (!isNullOrEmpty(apiKey) && !isNullOrEmpty(apiSecret)) {
            String b64 = Base64.encodeToString((apiKey + ":" + apiSecret).getBytes(), Base64.NO_WRAP);
            return "Basic " + b64;
        }

        // 3) Legacy
        SharedPreferences legacy = getSharedPreferences(LEGACY_PREFS, MODE_PRIVATE);
        String legacyToken = legacy.getString(LEGACY_KEY_TOKEN, null);
        if (!isNullOrEmpty(legacyToken)) return normalizeBasic(legacyToken);

        return null;
    }

    private int resolveLocationId() {
        // 1) Intent extra
        Intent i = getIntent();
        if (i != null && i.hasExtra(KEY_LOCATION_ID)) {
            int v = i.getIntExtra(KEY_LOCATION_ID, 0);
            if (v > 0) return v;
        }
        // 2) SecurePrefs
        int fromSecure = prefs.getLocationId();
        if (fromSecure > 0) return fromSecure;

        // 3) Legacy (string)
        SharedPreferences legacy = getSharedPreferences(LEGACY_PREFS, MODE_PRIVATE);
        String legacyLoc = legacy.getString(LEGACY_KEY_LOCATION, "0");
        try {
            return Integer.parseInt(legacyLoc);
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ---------- Printing ----------

    private void onClickPrintFor(Product p) {
        LabelData data = new LabelData(
                nullSafe(p.getDescription()),          // top-left
                nullSafe(resolveBarcode(p)),           // barcode value
                String.valueOf(resolveSku(p)),         // bottom-left line 1
                resolveStockNumber(p),                 // bottom-left line 2
                toBigDecimal(resolvePrice(p))          // big price (BigDecimal)
        );
        LabelPrinter.printSingle(this, data);
    }

    // ---------- UI helpers ----------

    private void fadeInProgressBar() {
        progressBar.setAlpha(0f);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate().alpha(1f).setDuration(150).start();
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v == null) v = etSearch;
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    // ---------- Mini utils ----------

    private static String safeText(EditText e) {
        return e == null || e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonEmpty(String... vals) {
        if (vals == null) return "";
        for (String s : vals) {
            if (s != null) {
                String t = s.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return "";
    }

    private static String normalizeBasic(String tokenOrHeader) {
        String t = tokenOrHeader.trim();
        if (t.regionMatches(true, 0, "Basic ", 0, 6)) return t;
        return "Basic " + t;
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static Object resolvePrice(Product p) { return p.getPrice(); }
    private static String resolveBarcode(Product p) { return p.getBarcode(); }
    private static Object resolveSku(Product p) { return p.getSku(); }
    private static String resolveStockNumber(Product p) { return "STOCK_NUMBER"; }

    private static java.math.BigDecimal toBigDecimal(Object price) {
        if (price instanceof java.math.BigDecimal) return (java.math.BigDecimal) price;
        if (price instanceof Double) return java.math.BigDecimal.valueOf((Double) price);
        if (price instanceof Float) return java.math.BigDecimal.valueOf(((Float) price).doubleValue());
        if (price instanceof Integer) return java.math.BigDecimal.valueOf(((Integer) price).doubleValue());
        if (price instanceof Long) return java.math.BigDecimal.valueOf(((Long) price).doubleValue());
        if (price instanceof String) {
            try { return new java.math.BigDecimal((String) price); } catch (Exception ignored) {}
        }
        return java.math.BigDecimal.ZERO;
    }

    // ---------- Toolbar menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu); // includes action_settings + action_print
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (id == R.id.action_print) {
            if (selectedProduct != null) {
                onClickPrintFor(selectedProduct);
            } else {
                Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show();
            }
            return true;

        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
