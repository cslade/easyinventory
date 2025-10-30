package com.kinvo.easyinventory.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kinvo.easyinventory.SecurePrefs;
import com.kinvo.easyinventory.model.Product;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shopify Admin API repository (REST + GraphQL).
 * - Searches via REST /products.json (client-side filter)
 * - Updates inventory via GraphQL inventoryAdjustQuantities (delta)
 *   with fallback to REST /inventory_levels/set.json (absolute).
 * - Auto-resolves and caches LONG location_id on first use.
 */
public class ShopifyRepository implements InventoryRepository {

    private static final String TAG = "ShopifyRepository";
    private static final String REST_VER = "2023-10";  // keep stable for product list + inventory_levels
    private static final String GQL_VER  = "2025-10";  // matches your curl example

    private RequestQueue queue(Context ctx) {
        return Volley.newRequestQueue(ctx.getApplicationContext());
    }

    private boolean hasCreds(Context ctx) {
        SecurePrefs p = SecurePrefs.get(ctx);
        return !TextUtils.isEmpty(safe(p.getShopDomain()))
                && !TextUtils.isEmpty(safe(p.getShopToken()));
    }

    private String baseRest(SecurePrefs p) {
        String d = normalizeDomain(safe(p.getShopDomain()));
        return "https://" + d + "/admin/api/" + REST_VER;
    }

    private String baseGraphQL(SecurePrefs p) {
        String d = normalizeDomain(safe(p.getShopDomain()));
        return "https://" + d + "/admin/api/" + GQL_VER + "/graphql.json";
    }

    private Map<String, String> headers(SecurePrefs p) {
        Map<String, String> h = new HashMap<>();
        h.put("Accept", "application/json");
        h.put("Content-Type", "application/json");
        h.put("User-Agent", "EasyInventory/Android");
        String token = safe(p.getShopToken()).trim();
        if (TextUtils.isEmpty(token)) throw new IllegalStateException("Missing Shopify Admin API access token.");
        h.put("X-Shopify-Access-Token", token);
        return h;
    }

    private static String normalizeDomain(String raw) {
        String d = raw.trim();
        if (d.startsWith("https://")) d = d.substring(8);
        if (d.startsWith("http://"))  d = d.substring(7);
        if (!d.endsWith(".myshopify.com") && !d.contains(".")) d = d + ".myshopify.com";
        return d;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String verboseVolleyError(com.android.volley.VolleyError e) {
        NetworkResponse nr = e.networkResponse;
        if (nr == null) return e.toString();
        String body;
        try { body = new String(nr.data, StandardCharsets.UTF_8); } catch (Exception ex) { body = "(unreadable)"; }
        String hint = (nr.statusCode == 401)
                ? " (401 Unauthorized – check domain/token/scopes: read_products, read_locations, read_inventory, write_inventory)"
                : "";
        Log.e(TAG, "Volley error " + nr.statusCode + ": " + body);
        return e.toString() + " status=" + nr.statusCode + " body=" + body + hint;
    }

    // ============================================================================================
    // InventoryRepository
    // ============================================================================================

    @Override
    public void searchProducts(Context ctx, String query, int locationId, int limit, Callback<List<Product>> cb) {
        if (!hasCreds(ctx)) {
            cb.onError(new IllegalStateException("Missing Shopify credentials (domain + Admin API token)."));
            return;
        }
        SecurePrefs prefs = SecurePrefs.get(ctx);
        String url = baseRest(prefs) + "/products.json?limit=" + Math.max(1, Math.min(250, limit <= 0 ? 250 : limit));

        final String needle = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONArray products = resp.optJSONArray("products");
                        List<Product> out = mapProducts(products, needle);
                        cb.onSuccess(out);
                    } catch (Throwable t) { cb.onError(asException(t)); }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(12000, 1, 1.0f));
        queue(ctx).add(req);
    }

    @Override
    public void getProductByBarcode(Context ctx, String barcode, int locationId, Callback<Product> cb) {
        if (!hasCreds(ctx)) { cb.onError(new IllegalStateException("Missing Shopify credentials.")); return; }
        if (TextUtils.isEmpty(barcode)) { cb.onSuccess(null); return; }

        SecurePrefs prefs = SecurePrefs.get(ctx);
        String url = baseRest(prefs) + "/products.json?limit=250";
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONArray products = resp.optJSONArray("products");
                        if (products == null) { cb.onSuccess(null); return; }
                        String needle = barcode.trim();
                        for (int i = 0; i < products.length(); i++) {
                            JSONObject pr = products.optJSONObject(i);
                            if (pr == null) continue;
                            JSONArray vars = pr.optJSONArray("variants");
                            if (vars == null) continue;
                            for (int v = 0; v < vars.length(); v++) {
                                JSONObject var = vars.optJSONObject(v);
                                if (var == null) continue;
                                String bc = var.optString("barcode", "");
                                if (needle.equalsIgnoreCase(bc)) {
                                    Product p = mapOne(pr, var);
                                    cb.onSuccess(p); return;
                                }
                            }
                        }
                        cb.onSuccess(null);
                    } catch (Throwable t) { cb.onError(asException(t)); }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(12000, 1, 1.0f));
        queue(ctx).add(req);
    }

    @Override
    public void updateStock(Context ctx, long inventoryItemId, int locationIdParam, double newQty, Callback<Product> cb) {
        if (!hasCreds(ctx)) { cb.onError(new IllegalStateException("Missing Shopify credentials.")); return; }
        SecurePrefs prefs = SecurePrefs.get(ctx);

        if (inventoryItemId <= 0) {
            cb.onError(new IllegalArgumentException("Missing inventory_item_id for this variant."));
            return;
        }

        // 1) Ensure we have a LONG location id.
        ensureLocationId(ctx, new Callback<Long>() {
            @Override public void onSuccess(Long locId) {
                if (locId == null || locId <= 0) {
                    cb.onError(new IllegalStateException("No Shopify locations found."));
                    return;
                }
                // 2) Read current inventory to compute delta (GraphQL needs delta).
                getCurrentAvailable(ctx, prefs, inventoryItemId, locId, new Callback<Integer>() {
                    @Override public void onSuccess(Integer currentAvail) {
                        int desired = (int) Math.round(newQty);
                        int delta   = desired - (currentAvail == null ? 0 : currentAvail);
                        if (delta == 0) {
                            Product ok = new Product();
                            ok.setDescription("No change");
                            ok.setCurrentStock(newQty);
                            cb.onSuccess(ok);
                            return;
                        }
                        // 3) Try GraphQL adjust (delta). Fallback to REST set if userErrors.
                        doAdjustInventoryGraphQL(ctx, prefs, inventoryItemId, locId, delta, new Callback<Boolean>() {
                            @Override public void onSuccess(Boolean ok) {
                                Product res = new Product();
                                res.setDescription("Inventory updated");
                                res.setCurrentStock(newQty);
                                cb.onSuccess(res);
                            }
                            @Override public void onError(Exception gqlErr) {
                                // Fallback: absolute set via REST
                                doSetInventoryREST(ctx, prefs, inventoryItemId, locId, desired, new Callback<Boolean>() {
                                    @Override public void onSuccess(Boolean ok) {
                                        Product res = new Product();
                                        res.setDescription("Inventory updated (REST fallback)");
                                        res.setCurrentStock(newQty);
                                        cb.onSuccess(res);
                                    }
                                    @Override public void onError(Exception restErr) {
                                        cb.onError(gqlErr != null ? gqlErr : restErr);
                                    }
                                });
                            }
                        });
                    }
                    @Override public void onError(Exception e) { cb.onError(e); }
                });
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    @Override
    public void fetchRecentUpdates(Context ctx, int locationId, long sinceEpochMs, int limit, Callback<List<Product>> cb) {
        if (!hasCreds(ctx)) { cb.onError(new IllegalStateException("Missing Shopify credentials.")); return; }
        SecurePrefs prefs = SecurePrefs.get(ctx);
        String url = baseRest(prefs) + "/products.json?limit=" + Math.max(1, Math.min(250, limit <= 0 ? 250 : limit))
                + "&updated_at_min=" + ISO8601.formatUtc(sinceEpochMs);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONArray products = resp.optJSONArray("products");
                        List<Product> out = mapProducts(products, "");
                        cb.onSuccess(out);
                    } catch (Throwable t) { cb.onError(asException(t)); }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(12000, 1, 1.0f));
        queue(ctx).add(req);
    }

    // ============================================================================================
    // Public helper: ensure LONG location id is cached
    // ============================================================================================

    public void ensureLocationId(@NonNull Context ctx, @NonNull Callback<Long> cb) {
        SecurePrefs prefs = SecurePrefs.get(ctx);
        long cached = prefs.getShopifyLocationId();
        if (cached > 0) { cb.onSuccess(cached); return; }
        resolveFirstActiveLocationId(ctx, prefs, cb);
    }

    // ============================================================================================
    // Internals
    // ============================================================================================

    private void resolveFirstActiveLocationId(Context ctx, SecurePrefs prefs, Callback<Long> cb) {
        String url = baseRest(prefs) + "/locations.json";
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONArray arr = resp.optJSONArray("locations");
                        if (arr == null || arr.length() == 0) {
                            cb.onError(new IllegalStateException("Shopify has no locations on this store."));
                            return;
                        }
                        Long chosen = null;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;
                            boolean active = o.optBoolean("active", true);
                            if (active) { chosen = o.optLong("id", 0L); break; }
                        }
                        if (chosen == null || chosen <= 0) {
                            chosen = arr.optJSONObject(0).optLong("id", 0L);
                        }
                        if (chosen == null || chosen <= 0) {
                            cb.onError(new IllegalStateException("Failed to read Shopify location id."));
                            return;
                        }
                        prefs.setShopifyLocationId(chosen);
                        cb.onSuccess(chosen);
                    } catch (Throwable t) { cb.onError(asException(t)); }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(12000, 1, 1.0f));
        queue(ctx).add(req);
    }

    /** Read current available to compute delta for GraphQL adjust. */
    private void getCurrentAvailable(Context ctx, SecurePrefs prefs, long inventoryItemId, long locationIdLong,
                                     Callback<Integer> cb) {
        String url = baseRest(prefs) + "/inventory_levels.json?inventory_item_ids=" + inventoryItemId
                + "&location_ids=" + locationIdLong;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONArray levels = resp.optJSONArray("inventory_levels");
                        if (levels != null && levels.length() > 0) {
                            JSONObject lvl = levels.optJSONObject(0);
                            int available = (lvl != null) ? lvl.optInt("available", 0) : 0;
                            cb.onSuccess(available);
                        } else {
                            cb.onSuccess(0); // treat as zero if not present
                        }
                    } catch (Throwable t) { cb.onError(asException(t)); }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        queue(ctx).add(req);
    }

    /** GraphQL delta adjust via inventoryAdjustQuantities. */
    private void doAdjustInventoryGraphQL(Context ctx, SecurePrefs prefs, long inventoryItemId, long locationIdLong,
                                          int delta, Callback<Boolean> cb) {

        String gidItem = "gid://shopify/InventoryItem/" + inventoryItemId;
        String gidLoc  = "gid://shopify/Location/" + locationIdLong;

        String mutation =
                "mutation inventoryAdjustQuantities($input: InventoryAdjustQuantitiesInput!) {"
                        + "  inventoryAdjustQuantities(input: $input) {"
                        + "    userErrors { field message }"
                        + "    inventoryAdjustmentGroup { createdAt reason referenceDocumentUri changes { name delta } }"
                        + "  }"
                        + "}";

        JSONObject variables = new JSONObject();
        JSONObject input = new JSONObject();
        JSONArray changes = new JSONArray();
        JSONObject change = new JSONObject();

        try {
            change.put("delta", delta);
            change.put("inventoryItemId", gidItem);
            change.put("locationId", gidLoc);
            changes.put(change);

            input.put("reason", "correction");
            input.put("name", "available");
            input.put("referenceDocumentUri", "easyinventory://android/manual-adjust");
            input.put("changes", changes);

            variables.put("input", input);
        } catch (Exception e) { cb.onError(e); return; }

        JSONObject body = new JSONObject();
        try {
            body.put("query", mutation);
            body.put("variables", variables);
        } catch (Exception e) { cb.onError(e); return; }

        String url = baseGraphQL(prefs);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, body,
                resp -> {
                    try {
                        JSONObject data = resp.optJSONObject("data");
                        JSONObject iaq  = data != null ? data.optJSONObject("inventoryAdjustQuantities") : null;
                        JSONArray userErrors = iaq != null ? iaq.optJSONArray("userErrors") : null;

                        if (userErrors != null && userErrors.length() > 0) {
                            // propagate as error so caller can fallback to REST
                            StringBuilder sb = new StringBuilder("GraphQL userErrors: ");
                            for (int i = 0; i < userErrors.length(); i++) {
                                JSONObject ue = userErrors.optJSONObject(i);
                                if (ue == null) continue;
                                sb.append(ue.optString("message", "error"));
                                if (i < userErrors.length() - 1) sb.append("; ");
                            }
                            cb.onError(new IllegalStateException(sb.toString()));
                            return;
                        }
                        cb.onSuccess(true);
                    } catch (Throwable t) { cb.onError(asException(t)); }
                },
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.2f));
        queue(ctx).add(req);
    }

    /** Absolute set via REST (fallback if GraphQL fails). */
    private void doSetInventoryREST(Context ctx, SecurePrefs prefs, long inventoryItemId, long locationIdLong,
                                    int available, Callback<Boolean> cb) {
        String url = baseRest(prefs) + "/inventory_levels/set.json";

        JSONObject body = new JSONObject();
        try {
            body.put("location_id", locationIdLong);
            body.put("inventory_item_id", inventoryItemId);
            body.put("available", available);
        } catch (Exception e) { cb.onError(e); return; }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, body,
                resp -> cb.onSuccess(true),
                err -> cb.onError(new RuntimeException(verboseVolleyError(err)))
        ) {
            @Override public Map<String, String> getHeaders() { return headers(prefs); }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.2f));
        queue(ctx).add(req);
    }

    // ---- mapping helpers ----

    private static List<Product> mapProducts(@Nullable JSONArray products, String needleLower) {
        List<Product> out = new ArrayList<>();
        if (products == null) return out;

        final boolean filter = !TextUtils.isEmpty(needleLower);
        for (int i = 0; i < products.length(); i++) {
            JSONObject pr = products.optJSONObject(i);
            if (pr == null) continue;

            String productTitle = pr.optString("title", "");
            JSONArray vars = pr.optJSONArray("variants");

            if (vars == null || vars.length() == 0) {
                if (!filter || productTitle.toLowerCase(Locale.ROOT).contains(needleLower)) {
                    Product p = new Product();
                    p.setDescription(productTitle);
                    p.setCurrentStock(0d);
                    p.setProvider("SHOPIFY");
                    out.add(p);
                }
                continue;
            }

            for (int v = 0; v < vars.length(); v++) {
                JSONObject var = vars.optJSONObject(v);
                if (var == null) continue;

                String vTitle  = var.optString("title", "");
                String display = productTitle;
                if (!"Default Title".equalsIgnoreCase(vTitle) && !TextUtils.isEmpty(vTitle)) {
                    display = productTitle + " — " + vTitle;
                }

                String sku     = var.optString("sku", "");
                String barcode = var.optString("barcode", "");
                String priceS  = var.optString("price", "0");
                double qty     = var.optDouble("inventory_quantity", 0d);
                long inventoryItemId = var.optLong("inventory_item_id", 0L);

                if (filter) {
                    String s = sku == null ? "" : sku.toLowerCase(Locale.ROOT);
                    String b = barcode == null ? "" : barcode.toLowerCase(Locale.ROOT);
                    if (!(display.toLowerCase(Locale.ROOT).contains(needleLower)
                            || s.contains(needleLower) || b.contains(needleLower))) {
                        continue;
                    }
                }

                Product p = new Product();
                p.setDescription(display);
                p.setSku(sku);
                p.setBarcode(barcode);
                try { p.setPriceBig(new java.math.BigDecimal(priceS)); }
                catch (Exception ignored) { p.setPriceBig(java.math.BigDecimal.ZERO); }
                p.setCurrentStock(qty);
                p.setProvider("SHOPIFY");
                if (inventoryItemId > 0) p.setInventoryItemId(inventoryItemId);
                out.add(p);
            }
        }
        return out;
    }

    private static Product mapOne(JSONObject product, JSONObject variant) {
        String title   = product.optString("title", "");
        String vTitle  = variant.optString("title", "");
        String display = "Default Title".equalsIgnoreCase(vTitle) || TextUtils.isEmpty(vTitle)
                ? title : (title + " — " + vTitle);

        Product p = new Product();
        p.setDescription(display);
        p.setSku(variant.optString("sku", ""));
        p.setBarcode(variant.optString("barcode", ""));
        try { p.setPriceBig(new java.math.BigDecimal(variant.optString("price", "0"))); }
        catch (Exception ignored) { p.setPriceBig(java.math.BigDecimal.ZERO); }
        p.setCurrentStock(variant.optDouble("inventory_quantity", 0d));
        p.setProvider("SHOPIFY");
        long inventoryItemId = variant.optLong("inventory_item_id", 0L);
        if (inventoryItemId > 0) p.setInventoryItemId(inventoryItemId);
        return p;
    }

    private static Exception asException(Throwable t) {
        return (t instanceof Exception) ? (Exception) t : new Exception(t);
    }

    private static final class ISO8601 {
        static String formatUtc(long epochMs) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.format(epochMs);
        }
    }
}
