package com.kinvo.easyinventory;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kinvo.easyinventory.adapters.ProductAdapter;
import com.kinvo.easyinventory.model.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductRequest {
    private final Context context;
    private final List<Product> productList;
    private final ProductAdapter adapter;
    private final ProgressBar progressBar;
    private final String authHeader;      // full header (e.g., "Basic xxxx")
    private final View recyclerView;
    private final TextView emptyView;

    public ProductRequest(Context context,
                          List<Product> productList,
                          ProductAdapter adapter,
                          ProgressBar progressBar,
                          String authHeader,
                          View recyclerView,
                          TextView emptyView) {
        this.context = context;
        this.productList = productList;
        this.adapter = adapter;
        this.progressBar = progressBar;
        this.authHeader = authHeader;
        this.recyclerView = recyclerView;
        this.emptyView = emptyView;
    }

    public JsonObjectRequest createRequest(String url) {
        Log.d("ProductRequest", "Request URL: " + url);

        return new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    productList.clear();
                    try {
                        JSONArray dataArray = response.getJSONArray("Data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);

                            // Build Product via setters to match your current model
                            Product p = new Product();
                            // Provider + ids
                            p.setProvider("EPOSNOW");
                            p.setExternalId(String.valueOf(obj.optInt("ProductId")));

                            // Core display fields
                            p.setDescription(obj.optString("ProductName", ""));

                            // Optional fields if present
                            // (Uncomment if your endpoint returns these keys)
                            // p.setSku(obj.optString("SKU", null));
                            // p.setBarcode(obj.optString("Barcode", null));

                            // Stock and price
                            double stock = obj.optDouble("CurrentStock", 0.0);
                            p.setCurrentStock(stock);

                            double price = obj.optDouble("SalePriceExcTax", 0.0);
                            p.setPriceBig(BigDecimal.valueOf(price));

                            productList.add(p);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error parsing product data", Toast.LENGTH_SHORT).show();
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        progressBar.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction(() -> progressBar.setVisibility(View.GONE));

                        updateEmptyView();
                    }, 500);
                },
                error -> {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        progressBar.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction(() -> progressBar.setVisibility(View.GONE));

                        Toast.makeText(context, "Failed to fetch products", Toast.LENGTH_SHORT).show();
                        updateEmptyView();
                    }, 500);

                    Log.e("ProductRequest", "Volley error: " + error.toString());
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", authHeader); // already includes "Basic "
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    }

    private void updateEmptyView() {
        if (productList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setAlpha(0f);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setListener(null);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}
