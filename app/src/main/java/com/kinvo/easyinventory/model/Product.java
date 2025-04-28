package com.kinvo.easyinventory.model;

public class Product {
    private int productId;
    private String productName;
    private double currentStock;
    private double salePriceExcTax;
    private boolean stockUpdatedMessageVisible; // ✅ New property

    // Simplified constructor
    public Product(int productId, String productName, double currentStock, double totalCost) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.salePriceExcTax = totalCost;
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

    public double getSalePriceExcTax() {
        return salePriceExcTax;
    }

    // Setters
    public void setCurrentStock(double currentStock) {
        this.currentStock = currentStock;
    }

    public void setSalePriceExcTax(double salePriceExcTax) {
        this.salePriceExcTax = salePriceExcTax;
    }


    public boolean isStockUpdatedMessageVisible() {
        return stockUpdatedMessageVisible;
    }

    public void setStockUpdatedMessageVisible(boolean visible) {
        this.stockUpdatedMessageVisible = visible;
    }
}






