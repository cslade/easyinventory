package com.kinvo.easyinventory;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.WebStorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.Normalizer;

import com.kinvo.easyinventory.adapters.ProductAdapter;
import com.kinvo.easyinventory.data.CloverRepository;
import com.kinvo.easyinventory.data.DataSource;
import com.kinvo.easyinventory.data.InventoryRepository;
import com.kinvo.easyinventory.data.ProviderFactory;
import com.kinvo.easyinventory.model.Product;
import com.kinvo.easyinventory.print.LabelData;
import com.kinvo.easyinventory.print.LabelPrinter;
import com.kinvo.easyinventory.util.CsvExporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductSearchActivity extends AppCompatActivity {

    private static final String TAG = "ProductSearchActivity";
    private static final boolean PREFETCH_ON_LAUNCH = false;
    private static final String UPGRADE_URL = "https://www.easyinventory.io/pricing";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText etSearch;

    private final List<Product> productList = new ArrayList<>();
    private ProductAdapter productAdapter;
    private Product selectedProduct = null;

    private boolean isLoading = false;

    // legacy EPOS-only fields (kept for adapter ctor compatibility)
    private String eposAuthHeader = "";
    private int eposLocationId = 0;

    private String providerName = "EPOSNOW"; // UI only



// ...inside ProductSearchActivity (static helpers) ----------------------------

    /** Replace curly quotes/em dashes, strip control chars, remove diacritics; keep printable ASCII. */
    private static String sanitizeForThermalPrinter(String s) {
        if (s == null) return "";

        // 1) Normalize and standardize punctuation that trips many label languages (ZPL/EPL/ESC/POS)
        String txt = s
                .replace('’','\'').replace('‘','\'')
                .replace('“','"').replace('”','"')
                .replace('—','-').replace('–','-')
                .replace('•','*');

        // 2) Remove control chars except \n and \t
        txt = txt.replaceAll("[\\p{Cntrl}&&[^\n\t]]", " ");

        // 3) If your printer language is ZPL/EPL, ^ and ~ are control; replace them
        txt = txt.replace("^", " ").replace("~", " ");

        // 4) Normalize unicode then strip combining marks (accents) to become ASCII-friendly
        String n = Normalizer.normalize(txt, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+", ""); // drop diacritics

        // 5) Remove any remaining non-printable (and most emoji/symbols)
        // Keep common punctuation and basic Latin
        n = n.replaceAll("[^\\p{Print}]", ""); // safety
        n = n.replaceAll("[^\\x20-\\x7E\\n\\t]", ""); // ASCII window

        // 6) Collapse whitespace
        n = n.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();

        // 7) Optional: truncate to avoid overflow on small labels
        int MAX_LEN = 48; // tune for your label width/font
        if (n.length() > MAX_LEN) n = n.substring(0, MAX_LEN - 1) + "…";

        return n;
    }

    /** Some printers reject empty strings; ensure we always send something safe. */
    private static String printableOrDash(String s) {
        String out = sanitizeForThermalPrinter(s);
        return out.isEmpty() ? "-" : out;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        toolbar      = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerProducts);
        progressBar  = findViewById(R.id.progressBar);
        etSearch     = findViewById(R.id.etSearch);

        setSupportActionBar(toolbar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // prefs -> provider & legacy epos bits
        try {
            SecurePrefs p = SecurePrefs.get(this);
            providerName = String.valueOf(p.getProvider());
            if (TextUtils.isEmpty(providerName) || "null".equalsIgnoreCase(providerName)) {
                providerName = p.getProviderName();
                if (TextUtils.isEmpty(providerName)) providerName = "EPOSNOW";
            }
            providerName = providerName.toUpperCase(Locale.ROOT);

            String key = safe(p.getApiKey());
            String secret = safe(p.getApiSecret());
            eposLocationId = p.getLocationId();
            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(secret)) {
                String creds = key + ":" + secret;
                eposAuthHeader = "Basic " + Base64.encodeToString(
                        creds.getBytes(StandardCharsets.UTF_8),
                        Base64.NO_WRAP
                );
            }
        } catch (Throwable ignored) {}

        productAdapter = new ProductAdapter(this, productList, eposAuthHeader, eposLocationId);
        recyclerView.setAdapter(productAdapter);

        productAdapter.setOnItemClickListener(item -> {
            selectedProduct = item;
            Toast.makeText(this, "Selected: " + safe(item.getDescription()), Toast.LENGTH_SHORT).show();
        });

        // Provider-aware stock update
        productAdapter.setOnUpdateStockRequested((product, newStock, onSuccess, onFailure) -> {
            InventoryRepository repo = ProviderFactory.get(this);

            // Determine active provider
            DataSource ds = DataSource.EPOSNOW;
            try {
                DataSource saved = SecurePrefs.get(this).getProvider();
                if (saved != null) ds = saved;
            } catch (Throwable ignored) {}

            switch (ds) {
                case EPOSNOW: {
                    // EPOS expects StockItemId and locationId from EPOS prefs
                    String externalId = product.getExternalId(); // map StockItemId -> externalId
                    long stockItemId = parseLongSafe(externalId);
                    int locationForUpdate = 0;
                    try { locationForUpdate = SecurePrefs.get(this).getLocationId(); } catch (Throwable ignored) {}

                    if (stockItemId <= 0 || locationForUpdate <= 0) {
                        onFailure.run();
                        Toast.makeText(this, "Missing EPOS StockItemId or Location Id. Please re-login.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    repo.updateStock(this, stockItemId, locationForUpdate, newStock, new InventoryRepository.Callback<Product>() {
                        @Override public void onSuccess(Product result) {
                            onSuccess.run(); // optimistic UI ok
                            Toast.makeText(ProductSearchActivity.this, "Stock updated", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onError(Exception e) {
                            Log.e(TAG, "EPOS update failed", e);
                            onFailure.run();
                            Toast.makeText(ProductSearchActivity.this,
                                    "Update failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }

                case SHOPIFY: {
                    // Need inventory_item_id only; let repo resolve LONG location id & do GraphQL delta
                    Long invItem = product.getInventoryItemId();
                    long inventoryItemId = (invItem == null ? 0L : invItem);

                    if (inventoryItemId <= 0) {
                        onFailure.run();
                        Toast.makeText(this,
                                "Missing Shopify inventory_item_id for this item. Re-search or refresh your catalog.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Pass locationId=0 -> repository will ensure LONG location id and proceed
                    repo.updateStock(this, inventoryItemId, /*locationId*/ 0, newStock,
                            new InventoryRepository.Callback<Product>() {
                                @Override public void onSuccess(Product result) {
                                    onSuccess.run();
                                    Toast.makeText(ProductSearchActivity.this, "Stock updated", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(Exception e) {
                                    Log.e(TAG, "Shopify update failed", e);
                                    onFailure.run();
                                    Toast.makeText(ProductSearchActivity.this,
                                            "Update failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                    break;
                }

                case CLOVER: {
                    if (!(repo instanceof CloverRepository)) {
                        onFailure.run();
                        Toast.makeText(this, "Clover repo not available.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Double prior = product.getCurrentStock();
                    product.setCurrentStock(newStock); // Clover expects absolute qty in body

                    ((CloverRepository) repo).updateStock(this, product, new InventoryRepository.Callback<Boolean>() {
                        @Override public void onSuccess(Boolean ok) {
                            onSuccess.run();
                            Toast.makeText(ProductSearchActivity.this, ok ? "Stock updated" : "No change", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onError(Exception e) {
                            product.setCurrentStock(prior); // restore if you want extra safety
                            Log.e(TAG, "Clover update failed", e);
                            onFailure.run();
                            Toast.makeText(ProductSearchActivity.this,
                                    "Update failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }

                default: {
                    onFailure.run();
                    Toast.makeText(this, "Provider not supported for update: " + ds, Toast.LENGTH_SHORT).show();
                }
            }
        });

        View btn = findViewById(R.id.btnSearch);
        if (btn != null) btn.setOnClickListener(this::onClickSearch);
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((tv, actionId, ev) -> {
                onClickSearch(tv);
                return true;
            });
        }

        if (PREFETCH_ON_LAUNCH) onClickSearch(null);
    }

    public void onClickSearch(View v) {
        // hide keyboard
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && etSearch != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        } catch (Throwable ignored) {}

        String query = etSearch != null ? etSearch.getText().toString().trim() : "";

        selectedProduct = null;
        productList.clear();
        productAdapter.notifyDataSetChanged();

        InventoryRepository repo = ProviderFactory.get(this);

        // EPOS uses location; others ignore
        int locationId = 0;
        try { locationId = SecurePrefs.get(this).getLocationId(); } catch (Throwable ignored) {}

        isLoading = true;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        repo.searchProducts(this, query, locationId, /*limit*/250, new InventoryRepository.Callback<List<Product>>() {
            @Override public void onSuccess(List<Product> result) {
                isLoading = false;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                productList.clear();
                if (result != null) productList.addAll(result);
                productAdapter.notifyDataSetChanged();

                if (productList.isEmpty()) {
                    Toast.makeText(ProductSearchActivity.this, "No products found.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onError(Exception e) {
                isLoading = false;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Search failed", e);
                Toast.makeText(ProductSearchActivity.this,
                        "Request failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true; // keep enabled; gating will happen when tapped
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        SecurePrefs prefs = SecurePrefs.get(this);
        Tier tier = TierUtils.resolveTier(prefs);
        MenuItem print = menu.findItem(R.id.action_print);
        MenuItem csv   = menu.findItem(R.id.action_export_csv);

        if (print != null) {
            print.setVisible(true);
            print.setEnabled(true); // keep enabled so taps trigger dialog for BASIC/DEMO
        }
        if (csv != null) {
            csv.setVisible(true);
            csv.setEnabled(true);
            if (tier != Tier.PREMIUM) {
                csv.setTitle("Export CSV (Premium)");
            } else {
                csv.setTitle("Export CSV");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (id == R.id.action_print) {
            if (!FeatureGate.requirePremiumOrDemo(this, SecurePrefs.get(this), "Printing labels", UPGRADE_URL)) {
                return true; // blocked by dialog
            }
            if (selectedProduct != null) {
                onClickPrintFor(selectedProduct);
            } else {
                Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show();
            }
            return true;

        } else if (id == R.id.action_export_csv) {
            if (!FeatureGate.requirePremium(this, SecurePrefs.get(this), "Export CSV", UPGRADE_URL)) {
                return true; // blocked by dialog
            }
            exportCsv();
            return true;

        } else if (id == R.id.action_logout) {
            doLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onClickPrintFor(@NonNull Product p) {
        if (!FeatureGate.requirePremiumOrDemo(this, SecurePrefs.get(this), "Printing labels", UPGRADE_URL)) return;

        // Sanitize strings specifically for label printers
        String name     = printableOrDash(p.getDescription());
        String barcode  = sanitizeForThermalPrinter(safe(p.getBarcode())); // keep digits/ASCII
        String sku      = sanitizeForThermalPrinter(String.valueOf(safe(p.getSku())));

        LabelData data = new LabelData(
                name,
                barcode,
                sku,
                "STOCK_NUMBER",
                p.getPriceBig() != null ? p.getPriceBig() : BigDecimal.ZERO
        );

        try {
            LabelPrinter.printSingle(this, data);
        } catch (Throwable t) {
            // Failsafe: try a super-conservative pass if your LabelPrinter is extra picky
            LabelData fallback = new LabelData(
                    sanitizeForThermalPrinter(name),
                    sanitizeForThermalPrinter(barcode),
                    sanitizeForThermalPrinter(sku),
                    "STOCK_NUMBER",
                    p.getPriceBig() != null ? p.getPriceBig() : BigDecimal.ZERO
            );
            try { LabelPrinter.printSingle(this, fallback); }
            catch (Throwable t2) {
                Log.e("LabelPrint", "Label print failed", t2);
                Toast.makeText(this, "Label print failed: " + (t2.getMessage() == null ? t2 : t2.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }


    private void exportCsv() {
        try {
            if (!FeatureGate.requirePremium(this, SecurePrefs.get(this), "Export CSV", UPGRADE_URL)) return;
            if (productList.isEmpty()) {
                Toast.makeText(this, "Nothing to export.", Toast.LENGTH_SHORT).show();
                return;
            }

            String csv = CsvExporter.toCsv(productList);

            File dir = new File(getCacheDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            File out = new File(dir, "inventory-" + ts + ".csv");

            try (FileOutputStream fos = new FileOutputStream(out);
                 OutputStreamWriter w = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                w.write(csv);
            }

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    out
            );

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("inventory.csv", csv));
            } catch (Throwable ignored) {}

            startActivity(Intent.createChooser(send, "Export CSV"));
            Toast.makeText(this, "Export complete (" + productList.size() + " rows).", Toast.LENGTH_SHORT).show();

        } catch (Throwable t) {
            Log.e(TAG, "CSV export failed", t);
            Toast.makeText(this, "Export failed.", Toast.LENGTH_LONG).show();
        }
    }

    private void doLogout() {
        // 1) Wipe encrypted provider/creds
        try { SecurePrefs.get(this).clearAll(); } catch (Throwable ignored) {}

        // 2) Wipe membership gate flag so MembershipLoginActivity shows again
        try {
            SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            sp.edit().clear().apply();
        } catch (Throwable ignored) {}

        // 3) Clear web auth cookies/storage
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.removeAllCookies(null);
            cm.flush();
            WebStorage.getInstance().deleteAllData();
        } catch (Throwable ignored) {}

        // 4) Reset stack to MembershipLogin
        Intent i = new Intent(this, MembershipLoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu(); // re-check tier if it changed while away
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static long parseLongSafe(String s) {
        if (TextUtils.isEmpty(s)) return 0L;
        try { return Long.parseLong(s.trim()); } catch (Throwable t) { return 0L; }
    }
}
