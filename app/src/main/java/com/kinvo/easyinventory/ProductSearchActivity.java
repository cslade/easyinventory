package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
    private EditText etBarcode;
    private Button btnSearchProduct;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvProductInfo;

    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private String authToken;
    private int locationId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        // ✅ Initialize UI Elements
        etBarcode = findViewById(R.id.etBarcode);
        btnSearchProduct = findViewById(R.id.btnSearchProduct);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        tvProductInfo = findViewById(R.id.tvProductInfo);

        // ✅ Retrieve Authentication Token
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");
        locationId = prefs.getInt("locationId", 0);

        Log.d("AUTH", "Retrieved Auth Token: " + authToken);
        Log.d("AUTH", "Retrieved Location ID: " + locationId);

        if (authToken.isEmpty()) {
            Log.e("AUTH_ERROR", "❌ No Auth Token Found! Cannot fetch products.");
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        // ✅ RecyclerView Setup
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, productList, authToken, locationId);
        recyclerView.setAdapter(productAdapter);

        // ✅ Search Product Button (Handles both empty and entered barcode cases)
        btnSearchProduct.setOnClickListener(v -> {
            String barcode = etBarcode.getText().toString().trim(); // ✅ Get barcode text

            if (barcode.isEmpty()) {
                fetchAllProducts(); // ✅ If no barcode, fetch all products
            } else {
                fetchProductByBarcode(barcode); // ✅ If barcode entered, fetch specific product
            }

            etBarcode.setText(""); // ✅ Reset barcode field after initiating the search

            // ✅ Hide the keyboard after clicking search
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etBarcode.getWindowToken(), 0);
            }
        });
    }

    private void fetchAllProducts() {
        progressBar.setVisibility(View.VISIBLE); // ✅ Show ProgressBar

        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId;
        Log.d("API_REQUEST", "Fetching ALL products from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE); // ✅ Hide ProgressBar on Success
                    Log.d("API_RESPONSE", "Response: " + response.toString());

                    try {
                        JSONArray dataArray = response.getJSONArray("Data");
                        productList.clear(); // ✅ Clear list before adding new products

                        if (dataArray.length() > 0) {
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject productObject = dataArray.getJSONObject(i);
                                int productId = productObject.getInt("ProductId");
                                String productName = productObject.getString("ProductName");
                                int currentStock = productObject.getInt("CurrentStock");

                                productList.add(new Product(productId, productName, currentStock));
                                Log.d("PRODUCT", "Added Product ID: " + productId + " Name: " + productName);
                            }
                            productAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "No products found.", Toast.LENGTH_SHORT).show();
                            Log.d("API_RESPONSE", "No products returned.");
                        }
                    } catch (JSONException e) {
                        Log.e("API_ERROR", "JSON Parsing Error: " + e.getMessage());
                        Toast.makeText(this, "Error parsing product data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE); // ✅ Hide ProgressBar on Failure
                    handleApiError(error);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + authToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void fetchProductByBarcode(String barcode) {
        progressBar.setVisibility(View.VISIBLE); // ✅ Show ProgressBar

        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId + "&Search=" + barcode;
        Log.d("API_REQUEST", "Fetching product from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE); // ✅ Hide ProgressBar on Success
                    Log.d("API_RESPONSE", "Response: " + response.toString());

                    try {
                        JSONArray dataArray = response.getJSONArray("Data");
                        productList.clear(); // ✅ Clear list before adding new products

                        if (dataArray.length() > 0) {
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject productObject = dataArray.getJSONObject(i);
                                int productId = productObject.getInt("ProductId");
                                String productName = productObject.getString("ProductName");
                                int currentStock = productObject.getInt("CurrentStock");

                                productList.add(new Product(productId, productName, currentStock));
                                Log.d("PRODUCT", "Added Product ID: " + productId + " Name: " + productName);
                            }
                            productAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "No products found.", Toast.LENGTH_SHORT).show();
                            Log.d("API_RESPONSE", "No products returned.");
                        }
                    } catch (JSONException e) {
                        Log.e("API_ERROR", "JSON Parsing Error: " + e.getMessage());
                        Toast.makeText(this, "Error parsing product data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE); // ✅ Hide ProgressBar on Failure
                    handleApiError(error);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + authToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void handleApiError(Exception error) {
        Log.e("API_ERROR", "Request Failed: " + error.toString());
        Toast.makeText(this, "Failed to fetch products. Check your internet.", Toast.LENGTH_LONG).show();
    }
}
