package jp.co.pbl2026.sales.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 【模範解答解説: 売上トランザクションエンティティクラス (Sale)】
 * 売上データ（ID、売上日、商品ID、数量、販売時単価、メモなど）を保持する JavaBean です。
 * データベースの `sales_transaction` テーブルに対応します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 合計金額の動的算出設計 (DB非保持の実現):
 *    要件定義「7.3 売上金額の扱い」に従い、合計金額をDBにカラムとして持たず、
 *    `getTotalAmount()` で `unit_price * quantity` の算出結果を返却します。
 *    これにより、データの二重管理を防ぎ整合性を保証しています。
 * 2. マスタ価格に依存しない販売時単価（unitPrice）の保持:
 *    売上トランザクション自体に `unitPrice` カラムを持たせることで、
 *    商品マスタの価格改定後も、過去の売上実績単価が書き換わらない構造にしています。
 */
public class Sale {
    private int id;
    private LocalDate saleDate;
    private int productId;
    /** JOINにより解決された商品名 */
    private String productName;
    private int quantity;
    /** 販売時の商品単価（マスタ価格のコピー） */
    private int unitPrice;
    private String memo;
    private int registeredAccountId;
    /** JOINにより解決された登録スタッフ名 */
    private String registeredStaffName;
    private int lastUpdatedAccountId;
    private LocalDateTime updatedAt;

    /**
     * 売上の合計金額（販売時単価 × 数量）を動的に計算して返します。
     * データベースには保存されない算出プロパティです。
     * 
     * @return 合計金額
     */
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

    /**
     * 更新日時を "yyyy/MM/dd HH:mm:ss" 形式のフォーマット済み文字列として取得します。
     * 
     * @return フォーマット済みの更新日時文字列
     */
    public String getFormattedUpdatedAt() {
        if (updatedAt == null) return "";
        return updatedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    }
}
