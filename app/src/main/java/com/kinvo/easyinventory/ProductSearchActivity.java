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
    private EditText etBarcode;
    private Button btnSearchProduct;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private String authToken;
    private int locationId = 0;  // ✅ Ensure locationId is always an integer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        // ✅ Setup Toolbar with Menu
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ✅ Initialize UI Elements
        etBarcode = findViewById(R.id.etBarcode);
        btnSearchProduct = findViewById(R.id.btnSearchProduct);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        // ✅ Retrieve Authentication Token & Location ID
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        // ✅ Retrieve locationId safely
        try {
            String locationIdStr = prefs.getString("locationId", "0"); // Retrieve as String
            locationId = Integer.parseInt(locationIdStr); // Convert to Integer safely
        } catch (NumberFormatException e) {
            Log.e("AUTH_ERROR", "❌ Invalid location ID format: " + e.getMessage());
            locationId = 0; // Default to 0 if conversion fails
        }

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

        // ✅ Search Product Button
        btnSearchProduct.setOnClickListener(v -> fetchProductByBarcode());
    }

    // ✅ Create the Options Menu (Ellipsis Button)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_product_search, menu);
        return true;
    }

    // ✅ Handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_user_agreement) {
            startActivity(new Intent(this, UserAgreementActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_privacy_policy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ Fetch Product by Barcode
    private void fetchProductByBarcode() {
        String barcode = etBarcode.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);  // ✅ Show ProgressBar

        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId;
        if (!barcode.isEmpty()) {
            url += "&Search=" + barcode;  // ✅ Add barcode search if not empty
        }

        Log.d("API_REQUEST", "Fetching product from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);  // ✅ Hide ProgressBar on Success
                    Log.d("API_RESPONSE", "Response: " + response.toString());

                    try {
                        JSONArray dataArray = response.getJSONArray("Data");
                        productList.clear();  // ✅ Clear the list before adding new products

                        if (dataArray.length() > 0) {
                            for (int i = 0; i < dataArray.length(); i++) {  // ✅ Loop through all products
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
                    progressBar.setVisibility(View.GONE);  // ✅ Hide ProgressBar on Failure
                    Log.e("API_ERROR", "Request Failed: " + error.toString());

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = new String(error.networkResponse.data);
                        Log.e("API_ERROR", "Status Code: " + statusCode);
                        Log.e("API_ERROR", "Response Body: " + responseBody);

                        if (statusCode == 401) {
                            Toast.makeText(this, "Unauthorized! Please reauthenticate.", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 404) {
                            Toast.makeText(this, "Product not found. Try another barcode.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Error: " + statusCode, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "No response from server. Check your internet.", Toast.LENGTH_LONG).show();
                    }
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

    // ✅ Logout Function
    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MembershipLoginActivity.class);
        startActivity(intent);
        finish();
    }
}
