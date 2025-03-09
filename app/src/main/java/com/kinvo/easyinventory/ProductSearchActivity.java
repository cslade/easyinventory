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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.kinvo.easyinventory.adapters.ProductAdapter;
import com.kinvo.easyinventory.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchActivity extends AppCompatActivity {
    private EditText etBarcode;
    private Button btnSearchProduct;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private String authToken;
    private int locationId = 0;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        // ✅ Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ✅ Initialize UI Elements
        etBarcode = findViewById(R.id.etBarcode);
        btnSearchProduct = findViewById(R.id.btnSearchProduct);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

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

        // ✅ Initialize Volley Request Queue
        requestQueue = Volley.newRequestQueue(this);

        // ✅ Setup RecyclerView
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

    // 🔹 Handle Menu Clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_user_agreement) {
            startActivity(new Intent(this, UserAgreementActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_privacy_policy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ Fetch Product by Barcode
    private void fetchProductByBarcode() {
        String barcode = etBarcode.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);  // ✅ Show ProgressBar

        // 🔹 Construct API URL
        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId;
        if (!barcode.isEmpty()) {
            url += "&Search=" + barcode;  // ✅ Add barcode search if user enters one
        }

        Log.d("API_REQUEST", "Fetching product from URL: " + url);

        // ✅ Use ProductRequest Class to Handle API Calls
        ProductRequest productRequest = new ProductRequest(
                this, productList, productAdapter, progressBar, authToken
        );

        requestQueue.add(productRequest.createRequest(url));
    }
}

