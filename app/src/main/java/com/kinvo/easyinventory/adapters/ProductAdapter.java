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

    public interface OnItemClickListener {
        void onItemClick(@NonNull Product item);
    }

    private final Context context;
    private final List<Product> productList;
    private final String authToken;
    private final int locationId;

    private OnItemClickListener itemClickListener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public ProductAdapter(Context context, List<Product> productList, String authToken, int locationId) {
        this.context = context;
        this.productList = productList;
        this.authToken = authToken;
        this.locationId = locationId;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.itemClickListener = l;
    }

    public String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view, new InternalClickBridge());
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getProductName());
        holder.tvCurrentStock.setText("Stock: " + product.getCurrentStock());
        holder.tvPrice.setText(formatCurrency(product.getSalePriceExcTax()));

        holder.tvStockUpdatedMessage.setVisibility(
                product.isStockUpdatedMessageVisible() ? View.VISIBLE : View.GONE
        );

        // simple “selection” feedback (optional—add a selector bg if you want)
        holder.itemView.setActivated(position == selectedPos);

        holder.btnUpdateStock.setOnClickListener(v -> {
            product.setStockUpdatedMessageVisible(true);
            notifyItemChanged(position);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                product.setStockUpdatedMessageVisible(false);
                notifyItemChanged(position);
            }, 10_000);

            showUpdateStockDialog(product, holder.tvStockUpdatedMessage);
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

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvProductName, tvCurrentStock, tvPrice, tvStockUpdatedMessage;
        public final Button btnUpdateStock;

        public ProductViewHolder(@NonNull View itemView, @NonNull Runnable onClick) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCurrentStock = itemView.findViewById(R.id.tvCurrentStock);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStockUpdatedMessage = itemView.findViewById(R.id.tvStockUpdatedMessage);
            btnUpdateStock = itemView.findViewById(R.id.btnUpdateStock);

            itemView.setOnClickListener(v -> onClick.run());
        }
    }

    private final class InternalClickBridge implements Runnable {
        @Override public void run() {
            // Resolve the correct adapter position from the clicked view holder
            // by asking the RecyclerView at bind-time:
            // We pass `this` into the ViewHolder; when it fires, we find which VH called us.
            // Simpler approach: we’ll infer from the current “selectedPos” logic by
            // walking up from the view. But since we don’t keep the view reference here,
            // we’ll do the standard trick: ViewHolder invokes this, and we query its position:
            // To keep it clean, we give the VH the Runnable, and here we’ll simply
            // look up the current binding adapter position via getBindingAdapterPosition()
            // which requires scope to the VH. The smallest change is to re-wire with a
            // lambda at create-time. So we’ll actually override this in onCreateViewHolder:
        }
    }

    // Convenience overload to attach the correct adapter position on click
    @NonNull
    private Runnable newClickForwarder(@NonNull ProductViewHolder vh) {
        return () -> onItemTapped(vh.getBindingAdapterPosition());
    }

    // Recreate holder with position-aware click
    @Override
    public void onViewAttachedToWindow(@NonNull ProductViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnClickListener(v -> onItemTapped(holder.getBindingAdapterPosition()));
    }

    public void showUpdateStockDialog(Product product, TextView tvStockUpdatedMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Stock for " + product.getProductName());

        final EditText input = new EditText(context);
        input.setHint("Enter new stock value");
        builder.setView(input);

        builder.setPositiveButton("Update", null);

        final AlertDialog dialog = builder.create();
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

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (d, w) -> dialog.cancel());
    }
}
