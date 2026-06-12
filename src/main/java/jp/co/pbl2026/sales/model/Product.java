package jp.co.pbl2026.sales.model;

import java.time.LocalDateTime;

public class Product {
    private int id;
    private int categoryId;
    private String categoryName;
    private String name;
    private int price;
    private boolean onSale;
    private int lastUpdatedAccountId;
    private LocalDateTime updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public boolean isOnSale() { return onSale; }
    public void setOnSale(boolean onSale) { this.onSale = onSale; }
    public int getLastUpdatedAccountId() { return lastUpdatedAccountId; }
    public void setLastUpdatedAccountId(int lastUpdatedAccountId) { this.lastUpdatedAccountId = lastUpdatedAccountId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
