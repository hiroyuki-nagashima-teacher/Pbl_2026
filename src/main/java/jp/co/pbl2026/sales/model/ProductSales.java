package jp.co.pbl2026.sales.model;

public class ProductSales {
    private String productName;
    private long totalAmount;

    public ProductSales(String productName, long totalAmount) {
        this.productName = productName;
        this.totalAmount = totalAmount;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }
}
