package com.kinvo.easyinventory;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
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
    private static final String TAG = "ProductRequest";
    private final ProductSearchActivity activity;
    private final List<Product> productList;
    private final ProductAdapter productAdapter;
    private final ProgressBar progressBar;
    private final String authToken;

    public ProductRequest(ProductSearchActivity activity, List<Product> productList,
                          ProductAdapter productAdapter, ProgressBar progressBar, String authToken) {
        this.activity = activity;
        this.productList = productList;
        this.productAdapter = productAdapter;
        this.progressBar = progressBar;
        this.authToken = authToken;
    }

    public JsonObjectRequest createRequest(String url) {
        return new JsonObjectRequest(Request.Method.GET, url, null,
                this::onResponse, this::onError) {
            @Override
            public Map<String, String> getHeaders() {
                return getRequestHeaders();
            }
        };
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + authToken);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private void onResponse(JSONObject response) {
        progressBar.setVisibility(View.GONE);
        Log.d(TAG, "Response: " + response.toString());

        try {
            JSONArray dataArray = response.getJSONArray("Data");
            processProductData(dataArray);
        } catch (JSONException e) {
            handleJsonError(e);
        }
    }

    private void processProductData(JSONArray dataArray) throws JSONException {
        productList.clear();
        if (dataArray.length() > 0) {
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject productObject = dataArray.getJSONObject(i);
                Product product = parseProduct(productObject);
                productList.add(product);
                Log.d(TAG, "Added Product ID: " + product.getProductId() + " Name: " + product.getProductName());
            }
            productAdapter.notifyDataSetChanged();
        } else {
            showToast("No products found.");
            Log.d(TAG, "No products returned.");
        }
    }

    private Product parseProduct(JSONObject productObject) throws JSONException {
        int productId = productObject.getInt("ProductId");
        String productName = productObject.getString("ProductName");
        int currentStock = productObject.getInt("CurrentStock");
        return new Product(productId, productName, currentStock);
    }

    private void onError(VolleyError error) {
        progressBar.setVisibility(View.GONE);
        Log.e(TAG, "Request Failed: " + error.toString());
        handleNetworkError(error);
    }

    private void handleNetworkError(VolleyError error) {
        NetworkResponse networkResponse = error.networkResponse;
        if (networkResponse != null) {
            int statusCode = networkResponse.statusCode;
            String responseBody = new String(networkResponse.data);
            Log.e(TAG, "Status Code: " + statusCode);
            Log.e(TAG, "Response Body: " + responseBody);
            handleStatusCodeError(statusCode);
        } else {
            showToast("No response from server. Check your internet.");
        }
    }

    private void handleStatusCodeError(int statusCode) {
        switch (statusCode) {
            case 401:
                showToast("Unauthorized! Please reauthenticate.");
                break;
            case 404:
                showToast("Product not found. Try another barcode.");
                break;
            default:
                showToast("Error: " + statusCode);
                break;
        }
    }

    private void handleJsonError(JSONException e) {
        Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
        showToast("Error parsing product data.");
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
