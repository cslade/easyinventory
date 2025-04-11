package com.kinvo.easyinventory.model;

public class Product {
    private int productId;
    private String productName;
    private double currentStock;
    private double totalCost;
    private boolean stockUpdatedMessageVisible; // ✅ New property

    // Simplified constructor
    public Product(int productId, String productName, double currentStock, double totalCost) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.totalCost = totalCost;
        this.stockUpdatedMessageVisible = false; // Default hidden
    }

    // Getters
    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getCurrentStock() {
        return currentStock;
    }

    public double getTotalCost() {
        return totalCost;
    }

    // Setters
    public void setCurrentStock(double currentStock) {
        this.currentStock = currentStock;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }


    public boolean isStockUpdatedMessageVisible() {
        return stockUpdatedMessageVisible;
    }

    public void setStockUpdatedMessageVisible(boolean visible) {
        this.stockUpdatedMessageVisible = visible;
    }
}






