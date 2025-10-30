package com.kinvo.easyinventory.model;

import java.math.BigDecimal;

public class Product {
    private String externalId;        // EPOS: StockItemId stringified, Shopify: variant or product id
    private String provider;          // "EPOSNOW", "SHOPIFY", etc.

    private String description;       // name/description shown in UI
    private String sku;               // sku as string
    private String barcode;
    private BigDecimal priceBig = BigDecimal.ZERO;

    private Double currentStock;      // may be null
    private Long variantId;           // for Shopify etc.
    private Long inventoryItemId;     // for Shopify stock operations

    // UI helpers
    private boolean stockUpdatedMessageVisible;

    // --- getters/setters ---
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public BigDecimal getPriceBig() { return priceBig == null ? BigDecimal.ZERO : priceBig; }
    public void setPriceBig(BigDecimal priceBig) { this.priceBig = (priceBig == null ? BigDecimal.ZERO : priceBig); }

    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }

    public boolean isStockUpdatedMessageVisible() { return stockUpdatedMessageVisible; }
    public void setStockUpdatedMessageVisible(boolean v) { this.stockUpdatedMessageVisible = v; }
}
