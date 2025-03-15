package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kinvo.easyinventory.adapters.ProductAdapter;
import com.kinvo.easyinventory.model.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductSearchActivity extends AppCompatActivity {
    private static final String TAG = "ProductSearchActivity";
    private static final String PREFS_NAME = "MyAppPrefs";

    private EditText etBarcode;
    private Button btnSearchProduct;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();

    private String authToken;
    private int locationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        initializeToolbar();
        initializeUI();
        retrievePreferences();
        setupRecyclerView();
        setupSearchButton();
    }

    private void initializeToolbar() {
        Toolbar toolbar = findViewById(R.id.pd_toolbar);
        setSupportActionBar(toolbar);

    }

    private void initializeUI() {
        etBarcode = findViewById(R.id.etBarcode);
        btnSearchProduct = findViewById(R.id.btnSearchProduct);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
    }

    private void retrievePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");
        locationId = parseLocationId(prefs.getString("locationId", "0"));

        Log.d(TAG, "✅ Retrieved Auth Token: " + authToken);
        Log.d(TAG, "✅ Retrieved Location ID: " + locationId);

        if (authToken.isEmpty()) {
            handleAuthenticationError();
        }
    }

    private int parseLocationId(String storedLocationId) {
        try {
            return Integer.parseInt(storedLocationId);
        } catch (NumberFormatException e) {
            Log.e(TAG, "❌ Error parsing locationId: " + storedLocationId, e);
            return 0;
        }
    }

    private void handleAuthenticationError() {
        Log.e(TAG, "❌ No Auth Token Found!");
        showToast("Authentication error. Please log in again.");
        finish(); // Close activity if authentication fails
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, productList, authToken, locationId);
        recyclerView.setAdapter(productAdapter);
    }

    private void setupSearchButton() {
        btnSearchProduct.setOnClickListener(v -> fetchProductByBarcode());
    }

    // ✅ Fetch Products
    private void fetchProductByBarcode() {
        String barcode = etBarcode.getText().toString().trim();
        showProgressBar();

        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId;
        if (!barcode.isEmpty()) {
            url += "&Search=" + barcode;
        }

        Log.d(TAG, "🔍 Fetching product from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    hideProgressBar();
                    Log.d(TAG, "✅ Response: " + response.toString());
                    handleProductResponse(response);
                },
                error -> {
                    hideProgressBar();
                    handleRequestError(error);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                return getRequestHeaders();
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // ✅ Handle API Response
    private void handleProductResponse(JSONObject response) {
        try {
            JSONArray dataArray = response.getJSONArray("Data");
            productList.clear();

            if (dataArray.length() > 0) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject productObject = dataArray.getJSONObject(i);
                    int productId = productObject.getInt("ProductId");
                    String productName = productObject.getString("ProductName");
                    int currentStock = productObject.getInt("CurrentStock");

                    Product product = new Product(productId, productName, currentStock);
                    productList.add(product);
                    Log.d(TAG, "✅ Added Product: " + product);
                }
                productAdapter.notifyDataSetChanged();
            } else {
                showToast("No products found.");
            }
        } catch (JSONException e) {
            Log.e(TAG, "❌ JSON Parsing Error: " + e.getMessage());
            showToast("Error parsing product data.");
        }
    }

    // ✅ Handle API Errors
    private void handleRequestError(com.android.volley.VolleyError error) {
        Log.e(TAG, "❌ Request Failed: " + error.toString());

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.e(TAG, "❌ Status Code: " + statusCode);
            switch (statusCode) {
                case 401:
                    showToast("Unauthorized! Please reauthenticate.");
                    break;
                case 404:
                    showToast("Product not found. Try another barcode.");
                    break;
                default:
                    showToast("Error: " + statusCode);
            }
        } else {
            showToast("No response from server. Check your internet.");
        }
    }

    // ✅ Logout User
    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, MembershipLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ✅ Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_product_search, menu);
        return true;
    }

    // ✅ Handle Menu Clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_user_agreement) {
            startActivity(new Intent(this, UserAgreementActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_privacy_policy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
        } else if (id == R.id.action_logout) {
            logoutUser();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // ✅ Helper Methods
    private void showProgressBar() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + authToken);
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
