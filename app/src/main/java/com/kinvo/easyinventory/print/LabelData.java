package com.kinvo.easyinventory.print;

import java.math.BigDecimal;

public class LabelData {
    public final String description;
    public final String barcode;      // what we encode (e.g., SKU or UPC/EAN/Code128 data)
    public final String sku;          // shown bottom-left line 1
    public final String stockNumber;  // shown bottom-left line 2
    public final BigDecimal price;    // e.g., 10.70

    public LabelData(String description, String barcode, String sku, String stockNumber, BigDecimal price) {
        this.description = description == null ? "" : description;
        this.barcode = barcode == null ? "" : barcode;
        this.sku = sku == null ? "" : sku;
        this.stockNumber = stockNumber == null ? "" : stockNumber;
        this.price = price == null ? BigDecimal.ZERO : price;
    }
}

