package com.kinvo.easyinventory.model;

public class Product {
    private int productId;
    private String productName;
    private int currentStock;
    private boolean stockUpdatedMessageVisible; // ✅ New property

    public Product(int productId, String productName, int currentStock) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.stockUpdatedMessageVisible = false; // Default hidden
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public boolean isStockUpdatedMessageVisible() {
        return stockUpdatedMessageVisible;
    }

    public void setStockUpdatedMessageVisible(boolean visible) {
        this.stockUpdatedMessageVisible = visible;
    }
}





