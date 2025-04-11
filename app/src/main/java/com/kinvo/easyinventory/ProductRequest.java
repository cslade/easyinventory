package com.kinvo.easyinventory;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kinvo.easyinventory.adapters.ProductAdapter;
import com.kinvo.easyinventory.model.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductRequest {
    private final Context context;
    private final List<Product> productList;
    private final ProductAdapter adapter;
    private final ProgressBar progressBar;
    private final String authToken;

    public ProductRequest(Context context, List<Product> productList, ProductAdapter adapter, ProgressBar progressBar, String authToken) {
        this.context = context;
        this.productList = productList;
        this.adapter = adapter;
        this.progressBar = progressBar;
        this.authToken = authToken;
    }

    public JsonObjectRequest createRequest(String url) {
        Log.d("ProductRequest", "Request URL: " + url);

        return new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    productList.clear();
                    try {
                        JSONArray dataArray = response.getJSONArray("Data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            Product product = new Product(
                                    obj.getInt("ProductId"),
                                    obj.getString("ProductName"),
                                    obj.getDouble("CurrentStock"),
                                    obj.getDouble("TotalCost")
                            );
                            productList.add(product);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error parsing product data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("ProductRequest", "Volley error: " + error.toString());
                    Toast.makeText(context, "Failed to fetch products", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + authToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    }
}
