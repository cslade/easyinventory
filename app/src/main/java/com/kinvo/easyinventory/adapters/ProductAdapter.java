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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull Product item);
    }

    private final Context context;
    private final List<Product> productList; // assumed mutable
    private final String authToken;
    private final int locationId;

    private OnItemClickListener itemClickListener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public ProductAdapter(Context context, List<Product> productList, String authToken, int locationId) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.authToken = authToken;
        this.locationId = locationId;
    }

    // ---------- Public helpers (added) ----------

    /** Returns a defensive copy of the items currently shown by the adapter. */
    @NonNull
    public List<Product> getCurrentItems() {
        return new ArrayList<>(productList);
    }

    /** Replace all items and refresh the list. */
    public void setItems(@NonNull List<Product> newItems) {
        productList.clear();
        productList.addAll(newItems);
        selectedPos = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    /** Returns the currently selected item (if any). */
    public Product getSelectedItem() {
        if (selectedPos >= 0 && selectedPos < productList.size()) {
            return productList.get(selectedPos);
        }
        return null;
    }

    /** Optional: allow screens to clear selection. */
    public void clearSelection() {
        int prev = selectedPos;
        selectedPos = RecyclerView.NO_POSITION;
        if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.itemClickListener = l;
    }

    // ---------- RecyclerView plumbing ----------

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
        holder.tvCurrentStock.setText("Stock: " + formatStock(product.getCurrentStock()));
        holder.tvPrice.setText(formatCurrency(product.getSalePriceExcTax()));

        holder.tvStockUpdatedMessage.setVisibility(
                product.isStockUpdatedMessageVisible() ? View.VISIBLE : View.GONE
        );

        // simple “selection” feedback (optional—use a selector bg if desired)
        holder.itemView.setActivated(position == selectedPos);

        holder.itemView.setOnClickListener(v -> onItemTapped(holder.getBindingAdapterPosition()));

        holder.btnUpdateStock.setOnClickListener(v -> {
            product.setStockUpdatedMessageVisible(true);
            notifyItemChanged(position);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                product.setStockUpdatedMessageVisible(false);
                notifyItemChanged(position);
            }, 10_000);

            showUpdateStockDialog(product);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    private void onItemTapped(int adapterPosition) {
        if (adapterPosition == RecyclerView.NO_POSITION) return;

        int previous = selectedPos;
        selectedPos = adapterPosition;

        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
        notifyItemChanged(selectedPos);

        if (itemClickListener != null) {
            itemClickListener.onItemClick(productList.get(adapterPosition));
        }
    }

    // ---------- ViewHolder ----------

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvProductName, tvCurrentStock, tvPrice, tvStockUpdatedMessage;
        public final Button btnUpdateStock;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCurrentStock = itemView.findViewById(R.id.tvCurrentStock);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStockUpdatedMessage = itemView.findViewById(R.id.tvStockUpdatedMessage);
            btnUpdateStock = itemView.findViewById(R.id.btnUpdateStock);
        }
    }

    // ---------- UI helpers ----------

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount);
    }

    private String formatStock(double s) {
        return (s % 1 == 0) ? String.valueOf((int) s) : String.valueOf(s);
    }

    private void showUpdateStockDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Stock for " + product.getProductName());

        final EditText input = new EditText(context);
        input.setHint("Enter new stock value (+/-)");
        builder.setView(input);

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String deltaText = input.getText().toString().trim();
            if (deltaText.isEmpty()) {
                Toast.makeText(context, "Please enter a stock quantity!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double delta = Double.parseDouble(deltaText);
                double newStock = product.getCurrentStock() + delta;
                product.setCurrentStock(newStock);
                product.setStockUpdatedMessageVisible(true);
                notifyDataSetChanged();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    product.setStockUpdatedMessageVisible(false);
                    notifyDataSetChanged();
                }, 3000);

                Toast.makeText(context, "Stock updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid stock quantity!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
