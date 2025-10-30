package com.kinvo.easyinventory.data;

import android.content.Context;

import com.kinvo.easyinventory.model.Product;

import java.util.List;

public interface InventoryRepository {

    interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    void searchProducts(Context ctx, String query, int locationId, int limit,
                        Callback<List<Product>> cb);

    void getProductByBarcode(Context ctx, String barcode, int locationId,
                             Callback<Product> cb);

    void updateStock(Context ctx, long productIdOrInventoryItemId, int locationId,
                     double newQty, Callback<Product> cb);

    void fetchRecentUpdates(Context ctx, int locationId, long sinceEpochMs, int limit,
                            Callback<List<Product>> cb);
}


