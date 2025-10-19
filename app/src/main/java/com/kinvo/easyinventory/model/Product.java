package com.kinvo.easyinventory.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Unified Product model that works with both existing search/update code
 * and new printing/label UI.
 */
public class Product {

    // --- Existing fields you already had ---
    private int productId;
    private String productName;
    private double currentStock;
    /** Historically used as “price”; we’ll keep it in sync with price */
    private double salePriceExcTax;
    private boolean stockUpdatedMessageVisible;

    // --- New fields commonly expected elsewhere ---
    private String barcode;          // e.g. EAN/UPC
    private String sku;              // merchant stock code
    private BigDecimal price;        // canonical price

    // -------- Constructors --------
    public Product() {
        this.stockUpdatedMessageVisible = false;
        this.price = BigDecimal.ZERO;
    }

    /** Back-compat constructor you already used */
    public Product(int productId, String productName, double currentStock, double totalCost) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.salePriceExcTax = totalCost;
        this.price = BigDecimal.valueOf(totalCost);
    }

    /** Full constructor for newer code paths */
    public Product(int productId,
                   String productName,
                   String barcode,
                   String sku,
                   BigDecimal price,
                   double currentStock) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.sku = sku;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.currentStock = currentStock;
        this.salePriceExcTax = this.price.doubleValue();
    }

    // -------- Primary getters used by new code --------
    /** New code often calls this for the label’s top-left text */
    public String getDescription() {
        return productName != null ? productName : "";
    }

    public String getBarcode() {
        return barcode != null ? barcode : "";
    }

    public String getSku() {
        return sku != null ? sku : String.valueOf(productId);
    }

    /** Preferred price getter for label printing (BigDecimal) */
    public BigDecimal getPrice() {
        return price != null ? price : BigDecimal.valueOf(salePriceExcTax);
    }

    // -------- Back-compat getters you already had --------
    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getCurrentStock() {
        return currentStock;
    }

    public double getSalePriceExcTax() {
        return salePriceExcTax;
    }

    public boolean isStockUpdatedMessageVisible() {
        return stockUpdatedMessageVisible;
    }

    // -------- Setters --------
    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setCurrentStock(double currentStock) {
        this.currentStock = currentStock;
    }

    public void setSalePriceExcTax(double salePriceExcTax) {
        this.salePriceExcTax = salePriceExcTax;
        // keep BigDecimal in sync
        this.price = BigDecimal.valueOf(salePriceExcTax);
    }

    public void setStockUpdatedMessageVisible(boolean visible) {
        this.stockUpdatedMessageVisible = visible;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setPrice(BigDecimal price) {
        this.price = price != null ? price : BigDecimal.ZERO;
        this.salePriceExcTax = this.price.doubleValue();
    }

    // -------- Utility --------
    @Override
    public String toString() {
        return "Product{" +
                "id=" + productId +
                ", name='" + productName + '\'' +
                ", sku='" + sku + '\'' +
                ", barcode='" + barcode + '\'' +
                ", price=" + getPrice() +
                ", currentStock=" + currentStock +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return productId == product.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}







