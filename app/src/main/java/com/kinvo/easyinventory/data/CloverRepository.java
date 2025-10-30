package com.kinvo.easyinventory.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kinvo.easyinventory.SecurePrefs;
import com.kinvo.easyinventory.model.Product;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Clover repository — search via /v3/merchants/{m}/items?expand=itemStock
 * and stock update via POST /v3/merchants/{m}/item_stocks/{itemId}.
 *
 * Sandbox baseUrl is used if the appId ends with ".demo" OR tier is "DEMO".
 */
public class CloverRepository implements InventoryRepository {

    private static final String TAG = "CloverRepository";

    private static RequestQueue queue(Context ctx) {
        return Volley.newRequestQueue(ctx.getApplicationContext());
    }

    private static String baseUrl(Context ctx) {
        boolean isDemo = false;
        try {
            String pkg = ctx.getPackageName();
            isDemo = (pkg != null && pkg.endsWith(".demo"));
        } catch (Throwable ignore) {}
        try {
            String tier = SecurePrefs.get(ctx).getTierName();
            if ("DEMO".equalsIgnoreCase(tier)) isDemo = true;
        } catch (Throwable ignore) {}
        // Keep using apisandbox for demo (you confirmed search works there).
        return isDemo ? "https://apisandbox.dev.clover.com" : "https://api.clover.com";
    }

    private static Map<String,String> headers(String token) {
        Map<String,String> h = new HashMap<>();
        h.put("Accept", "application/json");
        h.put("Authorization", "Bearer " + token);
        h.put("User-Agent", "EasyInventory/Android");
        return h;
    }

    private static String verboseVolleyError(com.android.volley.VolleyError e) {
        NetworkResponse nr = e.networkResponse;
        if (nr == null) return e.toString();
        String body;
        try { body = new String(nr.data, StandardCharsets.UTF_8); } catch (Exception ex) { body = ""; }
        return e.toString() + " status=" + nr.statusCode + " body=" + body;
    }

    // --------------------------------------------------------------------------------------------
    // SEARCH
    // --------------------------------------------------------------------------------------------
    @Override
    public void searchProducts(@NonNull Context ctx,
                               String query,
                               int locationId,
                               int limit,
                               @NonNull Callback<List<Product>> cb) {
        try {
            SecurePrefs p = SecurePrefs.get(ctx);
            String token = p.getCloverAccessToken();
            String merchantId = p.getCloverMerchantId();

            if (TextUtils.isEmpty(token) || TextUtils.isEmpty(merchantId)) {
                cb.onError(new IllegalStateException("Missing Clover token or merchant id."));
                return;
            }

            int pageLimit = (limit <= 0 ? 100 : Math.min(limit, 100));
            String url = baseUrl(ctx) + "/v3/merchants/" + merchantId
                    + "/items?limit=" + pageLimit + "&expand=itemStock";

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    rsp -> {
                        try {
                            // Clover returns { "elements": [ ... ], "href": "...", "next": "..." }
                            JSONArray arr = rsp.optJSONArray("elements");
                            if (arr == null) arr = rsp.optJSONArray("items"); // safety
                            List<Product> out = new ArrayList<>();
                            String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
                            boolean doFilter = !TextUtils.isEmpty(needle);

                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject o = arr.optJSONObject(i);
                                    if (o == null) continue;

                                    String name    = o.optString("name", "");
                                    String sku     = o.optString("code", "");
                                    String barcode = o.optString("alternateName", ""); // not always present
                                    long priceCents = o.optLong("price", 0L);
                                    BigDecimal price = BigDecimal.valueOf(priceCents).movePointLeft(2);

                                    double qty = 0d;
                                    JSONObject stock = o.optJSONObject("itemStock");
                                    if (stock != null) {
                                        // quantity or stockCount depending on account
                                        qty = stock.optDouble("quantity",
                                                stock.optDouble("stockCount", 0d));
                                    }

                                    if (doFilter) {
                                        String ln = name.toLowerCase(Locale.ROOT);
                                        String ls = sku == null ? "" : sku.toLowerCase(Locale.ROOT);
                                        String lb = barcode == null ? "" : barcode.toLowerCase(Locale.ROOT);
                                        if (!(ln.contains(needle) || ls.contains(needle) || lb.contains(needle))) {
                                            continue;
                                        }
                                    }

                                    Product pOut = new Product();
                                    pOut.setDescription(name);
                                    pOut.setSku(sku);
                                    pOut.setBarcode(barcode);
                                    pOut.setPriceBig(price);
                                    pOut.setCurrentStock(qty);
                                    pOut.setProvider("CLOVER");
                                    // Keep Clover's item id for updates:
                                    pOut.setExternalId(o.optString("id", ""));
                                    out.add(pOut);
                                }
                            }

                            cb.onSuccess(out);
                        } catch (Throwable t) {
                            cb.onError(asException(t));
                        }
                    },
                    err -> cb.onError(new RuntimeException("Clover search error: " + verboseVolleyError(err)))
            ) {
                @Override public Map<String, String> getHeaders() { return headers(token); }
            };
            req.setRetryPolicy(new DefaultRetryPolicy(12000, 1, 1.0f));
            queue(ctx).add(req);

        } catch (Throwable t) {
            cb.onError(asException(t));
        }
    }

    @Override
    public void getProductByBarcode(@NonNull Context ctx,
                                    @NonNull String barcode,
                                    int locationId,
                                    @NonNull Callback<Product> cb) {
        // Simple client-side filter by calling searchProducts with a high limit.
        searchProducts(ctx, barcode, locationId, 100, new Callback<List<Product>>() {
            @Override public void onSuccess(List<Product> result) {
                if (result == null) { cb.onSuccess(null); return; }
                for (Product p : result) {
                    if (barcode.equalsIgnoreCase(p.getBarcode())) { cb.onSuccess(p); return; }
                    if (barcode.equalsIgnoreCase(p.getSku()))     { cb.onSuccess(p); return; }
                }
                cb.onSuccess(null);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    @Override
    public void fetchRecentUpdates(@NonNull Context ctx,
                                   int locationId,
                                   long sinceEpochMillis,
                                   int limit,
                                   @NonNull Callback<List<Product>> cb) {
        // Optional: page /items and filter by modifiedTime here if needed.
        cb.onSuccess(new ArrayList<>());
    }

    // --------------------------------------------------------------------------------------------
    // STOCK UPDATE (provider-specific helper used by your older ProductSearchActivity)
    // POST /v3/merchants/{merchantId}/item_stocks/{itemId}
    //
    // Expected JSON body:
    // {
    //   "item": { "id": "<itemId>" },
    //   "quantity": <number>,            // absolute quantity
    //   "modifiedTime": <epochMillis>
    // }
    //
    // NOTE: We use StringRequest because Clover may return an empty body; this avoids JSON parse errors.
    // --------------------------------------------------------------------------------------------
    public void updateStock(@NonNull Context ctx,
                            @NonNull Product product,
                            @NonNull Callback<Boolean> cb) {
        try {
            SecurePrefs prefs = SecurePrefs.get(ctx);
            String token      = prefs.getCloverAccessToken();
            String merchantId = prefs.getCloverMerchantId();

            if (TextUtils.isEmpty(token) || TextUtils.isEmpty(merchantId)) {
                cb.onError(new IllegalStateException("Missing Clover token or merchant id."));
                return;
            }
            String itemId = product.getExternalId();
            if (TextUtils.isEmpty(itemId)) {
                cb.onError(new IllegalArgumentException("Missing Clover item id on Product.externalId."));
                return;
            }

            int targetQty = (int) Math.round(product.getCurrentStock() == null ? 0d : product.getCurrentStock());
            long now = System.currentTimeMillis();

            final String url = baseUrl(ctx) + "/v3/merchants/" + merchantId + "/item_stocks/" + itemId;

            JSONObject body = new JSONObject();
            JSONObject itemObj = new JSONObject();
            itemObj.put("id", itemId);
            body.put("item", itemObj);
            body.put("quantity", targetQty);
            body.put("modifiedTime", now);

            StringRequest req = new StringRequest(
                    Request.Method.POST,
                    url,
                    resp -> cb.onSuccess(true), // treat any 2xx as success
                    err  -> cb.onError(new RuntimeException("Clover update error: " + verboseVolleyError(err)))
            ) {
                @Override
                public byte[] getBody() {
                    return body.toString().getBytes(StandardCharsets.UTF_8);
                }
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=UTF-8";
                }
                @Override
                public Map<String, String> getHeaders() {
                    Map<String,String> h = headers(token);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            queue(ctx).add(req);

        } catch (Throwable t) {
            cb.onError(asException(t));
        }
    }

    // --------------------------------------------------------------------------------------------
    // Interface override (not used by your current activity for Clover).
    // Left as unsupported to avoid guessing numeric ids for Clover.
    // --------------------------------------------------------------------------------------------
    @Override
    public void updateStock(@NonNull Context ctx,
                            long productIdOrInventoryItemId,
                            int locationId,
                            double newQty,
                            @NonNull Callback<Product> cb) {
        cb.onError(new UnsupportedOperationException(
                "Use CloverRepository.updateStock(Context, Product, Callback<Boolean>) with Product.externalId set."));
    }

    private static Exception asException(Throwable t) {
        return (t instanceof Exception) ? (Exception) t : new Exception(t);
    }
}
