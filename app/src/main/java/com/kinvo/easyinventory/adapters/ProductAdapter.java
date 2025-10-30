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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull Product item);
    }

    /** Callback to let the Activity perform the real API update. */
    public interface OnUpdateStockRequested {
        void onUpdateStock(@NonNull Product product,
                           double newStock,
                           @NonNull Runnable onSuccess,
                           @NonNull Runnable onFailure);
    }

    private final Context context;
    private final List<Product> productList; // mutable backing list
    // kept for legacy ctor signature; not used inside this adapter
    @SuppressWarnings("unused") private final String authToken;
    @SuppressWarnings("unused") private final int locationId;

    private OnItemClickListener itemClickListener;
    private OnUpdateStockRequested updateStockListener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public ProductAdapter(Context context, List<Product> productList, String authToken, int locationId) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.authToken = authToken;
        this.locationId = locationId;
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.itemClickListener = l; }

    public void setOnUpdateStockRequested(OnUpdateStockRequested l) { this.updateStockListener = l; }

    @NonNull
    public List<Product> getCurrentItems() { return new ArrayList<>(productList); }

    public void setItems(@NonNull List<Product> newItems) {
        productList.clear();
        productList.addAll(newItems);
        selectedPos = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public Product getSelectedItem() {
        if (selectedPos >= 0 && selectedPos < productList.size()) return productList.get(selectedPos);
        return null;
    }

    public void clearSelection() {
        int prev = selectedPos;
        selectedPos = RecyclerView.NO_POSITION;
        if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
    }

    @NonNull @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(nullSafe(product.getDescription()));
        holder.tvCurrentStock.setText("Current Stock: " + formatStock(product.getCurrentStock() == null ? 0d : product.getCurrentStock()));
        holder.tvPrice.setText("Price: " + formatCurrency(product.getPriceBig()));

        holder.tvStockUpdatedMessage.setVisibility(
                product.isStockUpdatedMessageVisible() ? View.VISIBLE : View.GONE
        );

        holder.itemView.setActivated(position == selectedPos);
        holder.itemView.setOnClickListener(v -> onItemTapped(holder.getBindingAdapterPosition()));

        holder.btnUpdateStock.setOnClickListener(v -> showUpdateStockDialog(product, holder.getBindingAdapterPosition()));
    }

    @Override public int getItemCount() { return productList.size(); }

    private void onItemTapped(int adapterPosition) {
        if (adapterPosition == RecyclerView.NO_POSITION) return;

        int previous = selectedPos;
        selectedPos = adapterPosition;

        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
        notifyItemChanged(selectedPos);

        if (itemClickListener != null) itemClickListener.onItemClick(productList.get(adapterPosition));
    }

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

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(0);
        }
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount.doubleValue());
    }

    private String formatStock(double s) {
        return (s % 1 == 0) ? String.valueOf((int) s) : String.valueOf(s);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private void showUpdateStockDialog(Product product, int adapterPos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Stock for " + nullSafe(product.getDescription()));

        final EditText input = new EditText(context);
        input.setHint("Enter new stock value (+/- to adjust)");
        builder.setView(input);

        builder.setPositiveButton("Apply", null);
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
                double oldStock = product.getCurrentStock() == null ? 0d : product.getCurrentStock();
                double newStock = oldStock + delta;

                // optimistic UI
                product.setCurrentStock(newStock);
                product.setStockUpdatedMessageVisible(true);
                notifyItemChanged(adapterPos);

                Runnable undo = () -> {
                    product.setCurrentStock(oldStock);
                    product.setStockUpdatedMessageVisible(false);
                    notifyItemChanged(adapterPos);
                };
                Runnable done = () -> {
                    // keep new value, briefly show status text
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        product.setStockUpdatedMessageVisible(false);
                        notifyItemChanged(adapterPos);
                    }, 3000);
                };

                if (updateStockListener != null) {
                    updateStockListener.onUpdateStock(product, newStock, done, () -> {
                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                        undo.run();
                    });
                } else {
                    // fallback: local-only UI
                    done.run();
                    Toast.makeText(context, "Stock updated (local only).", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid stock quantity!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
