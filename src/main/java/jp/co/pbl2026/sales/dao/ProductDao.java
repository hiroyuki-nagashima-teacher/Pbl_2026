package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.pbl2026.sales.model.Product;

/**
 * 【模範解答解説: 商品マスタデータアクセスオブジェクト (ProductDao)】
 * 商品マスタ（product_master）に対するデータ操作（CRUD）および検索・検証処理を実装します。
 * 
 * ■ 設計・実装のポイント:
 * 1. ページングと動的ソートの統合:
 *    大量データ（商品100件〜）の表示負荷を下げるため、`LIMIT`・`OFFSET`によるページングおよびSQLインジェクション対策済みの動的ソートをサポートしています。
 * 2. 販売中フラグと論理削除のライフサイクル管理:
 *    - `deleted = false` は商品自体が存在することを示し、`on_sale = true` は売上登録画面で今選択可能かを示します。
 *    - `softDelete` 時には最終更新者アカウントIDも合わせて更新し、誰が最後に削除（更新）したかを監査できるようにしています。
 */
public class ProductDao {

    /**
     * ソート・ページ指定なしで、すべてのアクティブな（未削除の）商品情報を取得します。
     * 
     * @return アクティブな商品のリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Product> findAllActive() throws SQLException {
        return findAllActive(null, null);
    }

    /**
     * 指定されたカラムおよび順序でソートしたアクティブな商品情報を取得します。
     * ソート列はホワイトリスト方式でチェックし、SQLインジェクション脆弱性を完全に排除しています。
     * 
     * @param sortBy ソート対象のカラム名を表す文字列
     * @param order 昇順 ("asc") または 降順 ("desc")
     * @return ソート済みのアクティブな商品のリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Product> findAllActive(String sortBy, String order) throws SQLException {
        // デフォルトのソート列
        String sortColumn = "p.product_id";
        if ("product_name".equals(sortBy)) {
            sortColumn = "p.product_name";
        } else if ("category_name".equals(sortBy)) {
            sortColumn = "c.category_name";
        } else if ("price".equals(sortBy)) {
            sortColumn = "p.price";
        } else if ("on_sale".equals(sortBy)) {
            sortColumn = "p.on_sale";
        } else if ("updated_at".equals(sortBy)) {
            sortColumn = "p.updated_at";
        }

        // デフォルトのソート順
        String sortOrder = "ASC";
        if ("desc".equalsIgnoreCase(order)) {
            sortOrder = "DESC";
        }

        // カテゴリーマスタをJOINし、カテゴリー名も合わせて取得します。
        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE p.deleted = false "
                + "ORDER BY " + sortColumn + " " + sortOrder + ", p.product_id ASC";
        
        List<Product> products = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(map(rs));
            }
        }
        return products;
    }

    /**
     * 売上登録時に選択可能な「アクティブ（未削除）」かつ「販売中（on_sale = true）」の商品のみを取得します。
     * 要件定義「7.4 削除済みの商品は、売上追加時の商品選択肢には表示しない」に対応します。
     * 
     * @return 販売可能な商品のリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Product> findSellable() throws SQLException {
        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE p.deleted = false AND p.on_sale = true ORDER BY p.product_id";
        List<Product> products = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(map(rs));
            }
        }
        return products;
    }

    /**
     * 指定されたIDのアクティブな商品を取得します（削除済み商品は対象外）。
     * 
     * @param id 商品ID
     * @return 商品情報を格納した Optional オブジェクト
     * @throws SQLException データベース処理に失敗した場合
     */
    public Optional<Product> findActiveById(int id) throws SQLException {
        return findById(id, "p.deleted = false");
    }

    /**
     * 指定されたIDの販売中かつアクティブな商品を取得します。
     * 売上登録処理時の二重検証（画面表示から登録確定までの間に非表示化・削除されていないか）に利用します。
     * 
     * @param id 商品ID
     * @return 商品情報を格納した Optional オブジェクト
     * @throws SQLException データベース処理に失敗した場合
     */
    public Optional<Product> findSellableById(int id) throws SQLException {
        return findById(id, "p.deleted = false AND p.on_sale = true");
    }

    /**
     * 新しい商品を登録します。
     * 
     * @param product 登録する商品情報
     * @throws SQLException データベース処理に失敗した場合
     */
    public void insert(Product product) throws SQLException {
        String sql = "INSERT INTO product_master (category_id, product_name, price, on_sale, last_updated_account_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            setWritableFields(ps, product);
            ps.executeUpdate();
        }
    }

    /**
     * 既存の商品情報を更新します。
     * 
     * @param product 更新する商品情報
     * @throws SQLException データベース処理に失敗した場合
     */
    public void update(Product product) throws SQLException {
        String sql = "UPDATE product_master SET category_id = ?, product_name = ?, price = ?, on_sale = ?, "
                + "last_updated_account_id = ? WHERE product_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            setWritableFields(ps, product);
            ps.setInt(6, product.getId());
            ps.executeUpdate();
        }
    }

    /**
     * 商品を論理削除（deleted = true）します。
     * 「7.4 過去の売上に紐づく商品が削除されても、過去売上表示には影響させない」ために、物理レコードは残します。
     * 
     * @param id 削除する商品ID
     * @param accountId 操作を行ったアカウントID（監査証跡用として last_updated_account_id に記録）
     * @throws SQLException データベース処理に失敗した場合
     */
    public void softDelete(int id, int accountId) throws SQLException {
        String sql = "UPDATE product_master SET deleted = true, last_updated_account_id = ? WHERE product_id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * 指定された条件で商品をID検索する内部共通メソッド。
     */
    private Optional<Product> findById(int id, String condition) throws SQLException {
        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id WHERE p.product_id = ? AND " + condition;
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Insert/Update時の PreparedStatement へのパラメータ設定共通ロジック。
     */
    private void setWritableFields(PreparedStatement ps, Product product) throws SQLException {
        ps.setInt(1, product.getCategoryId());
        ps.setString(2, product.getName());
        ps.setInt(3, product.getPrice());
        ps.setBoolean(4, product.isOnSale());
        ps.setInt(5, product.getLastUpdatedAccountId());
    }

    /**
     * ResultSet の現在行を Product オブジェクトにマッピングします。
     */
    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("product_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setName(rs.getString("product_name"));
        p.setPrice(rs.getInt("price"));
        p.setOnSale(rs.getBoolean("on_sale"));
        p.setLastUpdatedAccountId(rs.getInt("last_updated_account_id"));
        p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return p;
    }

    /**
     * 論理削除されていないアクティブな商品データの総数を取得します（ページング処理に利用）。
     * 
     * @return アクティブな商品の総件数
     * @throws SQLException データベース処理に失敗した場合
     */
    public int countAllActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_master WHERE deleted = false";
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * ページングおよびソートを適用してアクティブな商品リストを取得します。
     * 
     * @param sortBy ソート列
     * @param order ソート順（"asc"/"desc"）
     * @param page 取得対象のページ番号（1始まり）
     * @param pageSize 1ページあたりの件数
     * @return 指定されたページの商品のリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Product> findAllActive(String sortBy, String order, int page, int pageSize) throws SQLException {
        String sortColumn = "p.product_id";
        if ("product_name".equals(sortBy)) {
            sortColumn = "p.product_name";
        } else if ("category_name".equals(sortBy)) {
            sortColumn = "c.category_name";
        } else if ("price".equals(sortBy)) {
            sortColumn = "p.price";
        } else if ("on_sale".equals(sortBy)) {
            sortColumn = "p.on_sale";
        } else if ("updated_at".equals(sortBy)) {
            sortColumn = "p.updated_at";
        }

        String sortOrder = "ASC";
        if ("desc".equalsIgnoreCase(order)) {
            sortOrder = "DESC";
        }

        // オフセット値の算出 (例: 2ページ目, 10件ずつの場合は OFFSET 10)
        int offset = (page - 1) * pageSize;

        // SQL文の末尾に LIMIT と OFFSET を付与して必要な分だけ取得（DB負荷軽減）
        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE p.deleted = false "
                + "ORDER BY " + sortColumn + " " + sortOrder + ", p.product_id ASC "
                + "LIMIT ? OFFSET ?";
        
        List<Product> products = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(map(rs));
                }
            }
        }
        return products;
    }
}
