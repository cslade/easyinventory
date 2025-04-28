package com.kinvo.easyinventory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class ProductSearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnSearch;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private String authToken;
    private int locationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        Toolbar toolbar = findViewById(R.id.pd_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Inventory Update");
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        }

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        productList = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", null);
        locationId = Integer.parseInt(sharedPreferences.getString("locationId", "0"));

        productAdapter = new ProductAdapter(this, productList, authToken, locationId);
        recyclerView.setAdapter(productAdapter);

        // Button Search click
        btnSearch.setOnClickListener(v -> {
            fadeInProgressBar();
            String query = etSearch.getText().toString().trim();
            searchProduct(query);
            etSearch.setText("");
            etSearch.clearFocus();
            hideKeyboard();
            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
        });

        // Keyboard Enter press
        etSearch.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            fadeInProgressBar();
            String query = etSearch.getText().toString().trim();
            searchProduct(query);
            etSearch.setText("");
            etSearch.clearFocus();
            hideKeyboard();
            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void searchProduct(String query) {
        progressBar.setVisibility(View.VISIBLE);
        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId;
        if (query != null && !query.isEmpty()) {
            url += "&Search=" + query;
        }

        ProductRequest productRequest = new ProductRequest(
                this,
                productList,
                productAdapter,
                progressBar,
                authToken,
                recyclerView,
                emptyView
        );

        requestQueue.add(productRequest.createRequest(url));
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void fadeInProgressBar() {
        progressBar.setAlpha(0f);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}



