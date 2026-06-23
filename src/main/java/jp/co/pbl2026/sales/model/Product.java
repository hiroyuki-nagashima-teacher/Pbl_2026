package jp.co.pbl2026.sales.model;

import java.time.LocalDateTime;

/**
 * 【模範解答解説: 商品エンティティクラス (Product)】
 * 商品情報（ID、名称、価格、販売フラグなど）を管理する JavaBean です。
 * データベースの `product_master` テーブルに対応します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 外部結合データ（categoryName）の保持:
 *    データベース設計上、カテゴリーは外部キー（`categoryId`）で持っていますが、
 *    一覧画面などで毎回JOIN後に別名マッピングする手間を省くため、エンティティ内に `categoryName` を保持させています。
 * 2. 販売中フラグ（onSale）の役割:
 *    商品が論理削除されていなくても、一時的に販売停止にしたい場合に対応するため、`onSale` フラグを持っています。
 */
public class Product {
    private int id;
    private int categoryId;
    /** JOINにより解決されたカテゴリー名 */
    private String categoryName;
    private String name;
    private int price;
    /** 販売中（true）か、販売停止（false）かを表すフラグ */
    private boolean onSale;
    /** 最後に商品を更新（または追加・削除）したアカウントのID */
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
