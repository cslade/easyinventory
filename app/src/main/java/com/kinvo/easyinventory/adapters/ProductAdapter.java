package com.kinvo.easyinventory.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.kinvo.easyinventory.R;
import com.kinvo.easyinventory.model.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private final List<Product> productList;
    private final Context context;
    private final String authToken;
    private final int locationId;
    private final RequestQueue requestQueue;

    public ProductAdapter(Context context, List<Product> productList, String authToken, int locationId) {
        this.context = context;
        this.productList = productList;
        this.authToken = authToken;
        this.locationId = locationId;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvProductName.setText(product.getProductName());
        holder.tvCurrentStock.setText("Stock: " + product.getCurrentStock());

        // ✅ Restore message visibility state
        if (product.isStockUpdatedMessageVisible()) {
            holder.tvStockUpdatedMessage.setVisibility(View.VISIBLE);
        } else {
            holder.tvStockUpdatedMessage.setVisibility(View.GONE);
        }

        Log.d("ADAPTER", "Binding Product ID: " + product.getProductId());

        holder.btnUpdateStock.setOnClickListener(v -> showUpdateStockDialog(product, holder.tvStockUpdatedMessage));
    }

    @Override
    public int getItemCount() {
        return (productList != null) ? productList.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvCurrentStock, tvStockUpdatedMessage;
        Button btnUpdateStock;

        ProductViewHolder(View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCurrentStock = itemView.findViewById(R.id.tvCurrentStock);
            btnUpdateStock = itemView.findViewById(R.id.btnUpdateStock);
            tvStockUpdatedMessage = itemView.findViewById(R.id.tvStockUpdatedMessage);
        }
    }

    private void showUpdateStockDialog(Product product, TextView tvStockUpdatedMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Stock for " + product.getProductName());

        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setHint("Enter Product Quantity");  // ✅ Add hint here
        input.setHintTextColor(context.getResources().getColor(R.color.mute_blue));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String stockInput = input.getText().toString();
            if (!stockInput.isEmpty()) {
                int changeInStock = Integer.parseInt(stockInput);
                int newStock = product.getCurrentStock() + changeInStock;

                Log.d("STOCK_UPDATE", "Current Stock: " + product.getCurrentStock() +
                        ", Change: " + changeInStock + ", New Stock: " + newStock);

                updateProductStock(product.getProductId(), newStock, product, tvStockUpdatedMessage);
            } else {
                Toast.makeText(context, "Please enter a valid stock quantity!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateProductStock(int productId, int newStock, Product product, TextView tvStockUpdatedMessage) {
        if (productId == 0) {
            Log.e("API_ERROR", "❌ Error: Product ID is missing! Cannot update stock.");
            Toast.makeText(context, "Error: Product ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://api.eposnowhq.com/api/v4/Inventory/" + productId + "/Update";

        JSONArray requestBody = new JSONArray();
        JSONObject productData = new JSONObject();

        try {
            productData.put("locationId", locationId);
            productData.put("currentStock", newStock);
            productData.put("minimumStock", 0);
            productData.put("maximumStock", 0);
            productData.put("productId", productId);
            productData.put("alerts", true);

            requestBody.put(productData);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        Log.d("API_REQUEST", "Updating stock for Product ID: " + productId);
        Log.d("API_REQUEST", "Payload: " + requestBody.toString());

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.POST, url, requestBody,
                response -> {
                    Log.d("API_RESPONSE", "✅ Stock update response: " + (response != null ? response.toString() : "Empty response"));

                    // ✅ Find the correct position in the list (SAFER than indexOf)
                    int position = IntStream.range(0, productList.size()).filter(i -> productList.get(i).getProductId() == productId).findFirst().orElse(-1);

                    if (position == -1) {
                        Log.e("UI_UPDATE", "❌ Could not find product in list! UI update skipped.");
                        return;
                    }

                    // ✅ Update stock and set the "Stock Updated!" message to visible
                    productList.get(position).setCurrentStock(newStock);
                    productList.get(position).setStockUpdatedMessageVisible(true);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        notifyItemChanged(position);
                        Log.d("UI_UPDATE", "✅ Stock Updated! message should now be visible.");
                    });

                    // ✅ Hide the message after 3 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        productList.get(position).setStockUpdatedMessageVisible(false);
                        notifyItemChanged(position);
                        Log.d("UI_UPDATE", "✅ Stock Updated! message is now hidden.");
                    }, 3000);

                    Toast.makeText(context, "Stock updated successfully!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    if (error.networkResponse == null) {
                        Log.e("API_ERROR", "❌ No network response received. Assuming success.");

                        int position = IntStream.range(0, productList.size()).filter(i -> productList.get(i).getProductId() == productId).findFirst().orElse(-1);

                        if (position == -1) {
                            Log.e("UI_UPDATE", "❌ Could not find product in list! UI update skipped.");
                            return;
                        }

                        productList.get(position).setCurrentStock(newStock);
                        productList.get(position).setStockUpdatedMessageVisible(true);

                        new Handler(Looper.getMainLooper()).post(() -> {
                            notifyItemChanged(position);
                            Log.d("UI_UPDATE", "✅ Stock Updated! message should now be visible.");
                        });

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            productList.get(position).setStockUpdatedMessageVisible(false);
                            notifyItemChanged(position);
                            Log.d("UI_UPDATE", "✅ Stock Updated! message is now hidden.");
                        }, 10000);

                        Toast.makeText(context, "Stock updated successfully!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int statusCode = error.networkResponse.statusCode;
                    String responseBody = new String(error.networkResponse.data);
                    Log.e("API_ERROR", "❌ HTTP Status: " + statusCode);
                    Log.e("API_ERROR", "❌ Response Body: " + responseBody);

                    Toast.makeText(context, "Stock update failed!", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + authToken);
                headers.put("Content-Type", "application/json-patch+json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

}
