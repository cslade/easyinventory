package com.kinvo.easyinventory.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

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

/** EPOS Now v4 repository: search (Basic) + update via /Inventory/{productId}/Update (Basic). */
public class EposNowRepository implements InventoryRepository {

    private static final String TAG  = "EposNowRepository";
    private static final String BASE = "https://api.eposnowhq.com/api/v4";

    private static RequestQueue q(Context ctx) {
        return Volley.newRequestQueue(ctx.getApplicationContext());
    }

    /** Build Basic auth header from prefs. */
    private static String resolveAuthHeader(Context ctx) {
        SecurePrefs p = SecurePrefs.get(ctx);
        String hdr = p.getAuthHeaderBasic();
        if (!TextUtils.isEmpty(hdr)) return hdr;

        String key = p.getApiKey();
        String secret = p.getApiSecret();
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(secret)) {
            String basic = key + ":" + secret;
            String b64 = Base64.encodeToString(basic.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            return "Basic " + b64;
        }
        return null;
    }

    private static Map<String, String> commonHeaders(String auth, boolean isPatchJson) {
        Map<String, String> h = new HashMap<>();
        h.put("Authorization", auth);
        h.put("Accept", "application/json");
        h.put("User-Agent", "EasyInventory/Android");
        if (isPatchJson) {
            // For StringRequest we’ll also return this from getBodyContentType.
            h.put("Content-Type", "application/json-patch+json");
        } else {
            h.put("Content-Type", "application/json");
        }
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
    // Search (unchanged — Basic auth)
    // --------------------------------------------------------------------------------------------

    @Override
    public void searchProducts(Context ctx, String query, int locationId, int limit, Callback<List<Product>> cb) {
        try {
            String auth = resolveAuthHeader(ctx);
            if (TextUtils.isEmpty(auth) || locationId <= 0) {
                cb.onError(new IllegalStateException("Missing EPOS auth or locationId"));
                return;
            }

            final int pageSize = limit > 0 ? Math.min(limit, 50) : 50;
            final String term = query == null ? "" : query.trim();
            final List<Product> acc = new ArrayList<>();

            String firstUrl = Uri.parse(BASE + "/inventory/stocks").buildUpon()
                    .appendQueryParameter("locationId", String.valueOf(locationId))
                    .appendQueryParameter("page", "1")
                    .appendQueryParameter("limit", String.valueOf(pageSize))
                    .build()
                    .toString();

            fetchPage(ctx, auth, firstUrl, term, limit, acc, cb);
        } catch (Exception e) {
            cb.onError(e);
        }
    }

    private void fetchPage(Context ctx, String auth, String url, String term, int maxResults,
                           List<Product> acc, Callback<List<Product>> cb) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                rsp -> {
                    try {
                        JSONArray data = rsp.optJSONArray("Data");
                        if (data != null) {
                            final boolean doFilter = !TextUtils.isEmpty(term);
                            final String needle = doFilter ? term.toLowerCase(Locale.ROOT) : "";

                            for (int i = 0; i < data.length(); i++) {
                                JSONObject o = data.optJSONObject(i);
                                if (o == null) continue;
                                Product p = mapStockItem(o);
                                if (!doFilter || matches(p, needle)) {
                                    acc.add(p);
                                    if (maxResults > 0 && acc.size() >= maxResults) break;
                                }
                            }
                        }

                        if (maxResults > 0 && acc.size() >= maxResults) {
                            cb.onSuccess(trimToLimit(acc, maxResults));
                            return;
                        }

                        JSONObject links = rsp.optJSONObject("_links");
                        String nextRel = links != null ? links.optString("NextPage", null) : null;
                        if (!TextUtils.isEmpty(nextRel) && !"null".equalsIgnoreCase(nextRel)) {
                            String hostBase = "https://api.eposnowhq.com";
                            String nextUrl = nextRel.startsWith("http") ? nextRel : (hostBase + nextRel);
                            fetchPage(ctx, auth, nextUrl, term, maxResults, acc, cb);
                        } else {
                            cb.onSuccess(acc);
                        }
                    } catch (Exception e) {
                        cb.onError(e);
                    }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return commonHeaders(auth, false); }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        q(ctx).add(req);
    }

    private static Product mapStockItem(JSONObject o) {
        Product p = new Product();

        long productId   = o.optLong("ProductId", 0L);
        int  stockItemId = o.optInt("StockItemId", 0);

        // Keep externalId = StockItemId (legacy). We’ll resolve ProductId during update.
        String externalId = stockItemId > 0 ? String.valueOf(stockItemId)
                : (productId > 0 ? String.valueOf(productId) : "");

        String name = o.optString("ProductName",
                o.optString("Description", ""));

        BigDecimal price = BigDecimal.ZERO;
        if (o.has("SalePriceIncTax")) {
            price = new BigDecimal(String.valueOf(o.optDouble("SalePriceIncTax", 0d)));
        } else if (o.has("SalePriceExcTax")) {
            price = new BigDecimal(String.valueOf(o.optDouble("SalePriceExcTax", 0d)));
        }

        p.setExternalId(externalId);
        p.setSku(o.optString("Sku", externalId));
        p.setDescription(name);
        p.setBarcode(o.optString("Barcode", ""));
        p.setCurrentStock(o.optDouble("CurrentStock", 0d));
        p.setProvider("EPOSNOW");
        p.setPriceBig(price);
        return p;
    }

    private static boolean matches(Product p, String needle) {
        String name = safe(p.getDescription());
        String barcode = safe(p.getBarcode());
        String sku = safe(p.getSku());
        return name.toLowerCase(Locale.ROOT).contains(needle)
                || barcode.toLowerCase(Locale.ROOT).contains(needle)
                || sku.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<Product> trimToLimit(List<Product> acc, int limit) {
        if (limit <= 0 || acc.size() <= limit) return acc;
        return new ArrayList<>(acc.subList(0, limit));
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override
    public void getProductByBarcode(Context ctx, String barcode, int locationId, Callback<Product> cb) {
        searchProducts(ctx, barcode, locationId, 1, new Callback<List<Product>>() {
            @Override public void onSuccess(List<Product> result) {
                cb.onSuccess((result != null && !result.isEmpty()) ? result.get(0) : null);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    // --------------------------------------------------------------------------------------------
    // UPDATE STOCK — POST /Inventory/{productId}/Update with JSON-**array** body (absolute qty)
    // Uses StringRequest to tolerate empty/204 responses (no ParseError).
    // --------------------------------------------------------------------------------------------

    @Override
    public void updateStock(Context ctx,
                            long productIdOrStockItemId,
                            int locationId,
                            double newQty,
                            Callback<Product> cb) {
        final String auth = resolveAuthHeader(ctx);
        if (TextUtils.isEmpty(auth) || locationId <= 0 || productIdOrStockItemId <= 0) {
            cb.onError(new IllegalArgumentException("Missing EPOS auth, locationId, or stock/product id."));
            return;
        }

        // 1) Lookup stock rows at this location to resolve true ProductId & preserve min/max/alerts
        String lookupUrl = Uri.parse(BASE + "/inventory/stocks").buildUpon()
                .appendQueryParameter("locationId", String.valueOf(locationId))
                .appendQueryParameter("page", "1")
                .appendQueryParameter("limit", "200")
                .build()
                .toString();

        JsonObjectRequest getReq = new JsonObjectRequest(
                Request.Method.GET, lookupUrl, null,
                rsp -> {
                    try {
                        JSONArray data = rsp.optJSONArray("Data");
                        if (data == null || data.length() == 0) {
                            cb.onError(new RuntimeException("No stocks found at this location."));
                            return;
                        }

                        JSONObject match = null;
                        long productIdFound = 0L;
                        int minStock = 0, maxStock = 0;
                        boolean alerts = true;

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject row = data.optJSONObject(i);
                            if (row == null) continue;

                            long productId = row.optLong("ProductId", 0L);
                            int stockItemId = row.optInt("StockItemId", 0);

                            if (productIdOrStockItemId == productId || productIdOrStockItemId == stockItemId) {
                                match = row;
                                productIdFound = productId;
                                minStock = (int) Math.round(row.optDouble("MinimumStock", 0d));
                                maxStock = (int) Math.round(row.optDouble("MaximumStock", 0d));
                                alerts   = row.has("Alerts") ? row.optBoolean("Alerts", true) : true;
                                break;
                            }
                        }

                        if (match == null || productIdFound <= 0) {
                            cb.onError(new RuntimeException("Product not found for id=" + productIdOrStockItemId));
                            return;
                        }

                        // 2) Build JSON-ARRAY body (per EPOS docs/curl)
                        String url = BASE + "/Inventory/" + productIdFound + "/Update";
                        JSONArray body = new JSONArray();
                        JSONObject entry = new JSONObject();
                        entry.put("locationId", locationId);
                        entry.put("currentStock", (int) Math.round(newQty));
                        entry.put("minimumStock", minStock);
                        entry.put("maximumStock", maxStock);
                        entry.put("productId", productIdFound);
                        entry.put("alerts", alerts);
                        body.put(entry);

                        // 3) POST with StringRequest (handles empty/204 OK)
                        JSONObject finalMatch = match;
                        StringRequest postReq = new StringRequest(
                                Request.Method.POST,
                                url,
                                resp -> {
                                    // Treat any 2xx as success; resp may be empty
                                    Product p = mapStockItem(finalMatch);
                                    p.setCurrentStock(newQty);
                                    cb.onSuccess(p);
                                },
                                err -> cb.onError(new RuntimeException("EPOS update error: " + verboseVolleyError(err)))
                        ) {
                            @Override
                            public byte[] getBody() {
                                return body.toString().getBytes(StandardCharsets.UTF_8);
                            }

                            @Override
                            public String getBodyContentType() {
                                return "application/json-patch+json; charset=UTF-8";
                            }

                            @Override
                            public Map<String, String> getHeaders() {
                                return commonHeaders(auth, true);
                            }
                        };

                        postReq.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
                        q(ctx).add(postReq);

                    } catch (Exception e) {
                        cb.onError(e);
                    }
                },
                err -> cb.onError(new RuntimeException("EPOS lookup error: " + verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return commonHeaders(auth, false); }
        };

        getReq.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        q(ctx).add(getReq);
    }

    @Override
    public void fetchRecentUpdates(Context ctx, int locationId, long sinceEpochMs, int limit, Callback<List<Product>> cb) {
        cb.onError(new UnsupportedOperationException("EPOS fetchRecentUpdates not implemented yet"));
    }
}
