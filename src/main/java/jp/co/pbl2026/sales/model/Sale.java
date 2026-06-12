package jp.co.pbl2026.sales.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Sale {
    private int id;
    private LocalDate saleDate;
    private int productId;
    private String productName;
    private int quantity;
    private int unitPrice;
    private String memo;
    private int registeredAccountId;
    private String registeredStaffName;
    private int lastUpdatedAccountId;
    private LocalDateTime updatedAt;

    public int getTotalAmount() {
        return unitPrice * quantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public int getRegisteredAccountId() { return registeredAccountId; }
    public void setRegisteredAccountId(int registeredAccountId) { this.registeredAccountId = registeredAccountId; }
    public String getRegisteredStaffName() { return registeredStaffName; }
    public void setRegisteredStaffName(String registeredStaffName) { this.registeredStaffName = registeredStaffName; }
    public int getLastUpdatedAccountId() { return lastUpdatedAccountId; }
    public void setLastUpdatedAccountId(int lastUpdatedAccountId) { this.lastUpdatedAccountId = lastUpdatedAccountId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
