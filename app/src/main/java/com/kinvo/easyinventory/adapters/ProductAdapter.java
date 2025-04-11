package com.kinvo.easyinventory.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kinvo.easyinventory.R;
import com.kinvo.easyinventory.model.Product;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private final Context context;
    private final List<Product> productList;
    private final String authToken;
    private final int locationId;

    public ProductAdapter(Context context, List<Product> productList, String authToken, int locationId) {
        this.context = context;
        this.productList = productList;
        this.authToken = authToken;
        this.locationId = locationId;
    }

    public String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount);
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
        holder.tvTotalCost.setText(formatCurrency(product.getTotalCost()));

        if (product.isStockUpdatedMessageVisible()) {
            holder.tvStockUpdatedMessage.setVisibility(View.VISIBLE);
        } else {
            holder.tvStockUpdatedMessage.setVisibility(View.GONE);
        }

        holder.btnUpdateStock.setOnClickListener(v -> {
            product.setStockUpdatedMessageVisible(true);
            notifyItemChanged(position);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                product.setStockUpdatedMessageVisible(false);
                notifyItemChanged(position);
            }, 3000);

            showUpdateStockDialog(product, holder.tvStockUpdatedMessage);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        public TextView tvProductName, tvCurrentStock, tvTotalCost, tvStockUpdatedMessage;
        public Button btnUpdateStock;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCurrentStock = itemView.findViewById(R.id.tvCurrentStock);
            tvTotalCost = itemView.findViewById(R.id.tvTotalCost);
            tvStockUpdatedMessage = itemView.findViewById(R.id.tvStockUpdatedMessage);
            btnUpdateStock = itemView.findViewById(R.id.btnUpdateStock);
        }
    }

    public void showUpdateStockDialog(Product product, TextView tvStockUpdatedMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Stock for " + product.getProductName());

        final EditText input = new EditText(context);
        input.setHint("Enter new stock value");
        builder.setView(input);

        builder.setPositiveButton("Update", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newStockValue = input.getText().toString().trim();
            if (!newStockValue.isEmpty()) {
                try {
                    double changeInStock = Double.parseDouble(newStockValue);
                    double newStock = product.getCurrentStock() + changeInStock;
                    product.setCurrentStock(newStock);
                    product.setStockUpdatedMessageVisible(true);
                    notifyDataSetChanged();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        product.setStockUpdatedMessageVisible(false);
                        notifyDataSetChanged();
                    }, 3000);

                    Toast.makeText(context, "Stock updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid stock quantity!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Please enter a stock quantity!", Toast.LENGTH_SHORT).show();
            }
        });

        // Move cancel button after dialog creation
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (d, w) -> dialog.cancel());
    }
}

