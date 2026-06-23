package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.pbl2026.sales.model.CategorySales;
import jp.co.pbl2026.sales.model.ProductSales;
import jp.co.pbl2026.sales.model.Sale;
import jp.co.pbl2026.sales.model.SaleSearchCondition;

/**
 * 【模範解答解説: 売上トランザクションデータアクセスオブジェクト (SaleDao)】
 * 売上データ（sales_transaction）の登録、検索、削除、およびダッシュボード用の各種統計集計を処理します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 販売時単価のコピー保存（価格の整合性担保）:
 *    要件定義「7.3 売上金額の扱い」に基づき、売上登録（`insert`）の瞬間に商品マスタの価格を `unit_price`（販売時単価）
 *    として売上テーブルへコピーして固定します。これにより、後から商品マスタの価格が改定されても、過去の売上実績の金額は一切変動しない設計にしています。
 * 2. 検索条件に応じた安全な動的SQL構築:
 *    `search` メソッドでは、期間、スタッフ名、金額範囲などの有無に応じて動的にSQL文（WHERE句）を結合します。
 *    SQLインジェクション脆弱性を防ぐため、結合箇所にはプレースホルダ（`?`）を使用し、`bind()` メソッドで安全にバインドします。
 * 3. 算出カラムに対するDB非保持設計:
 *    合計金額はテーブルに保存せず、SQL内で `quantity * unit_price` を計算する、またはJavaモデルのゲッターで算出することで、
 *    データ冗長性を排除しDB容量を最適化しています。
 * 4. ビジネスルールに準拠したメモのみの更新:
 *    要件定義「4.5 売上編集要件」により、登録済みの売上データに対しては `memo` カラムのみが更新可能（`updateMemo`）となっています。
 */
public class SaleDao {

    /**
     * 指定された検索条件に基づいて、アクティブな売上データリストを取得します。
     * 動的WHERE句の組み立てと、ソート指定の適用を行います。
     * 
     * @param condition 検索条件オブジェクト
     * @return 検索結果の売上リスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Sale> search(SaleSearchCondition condition) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, p.product_name, a.staff_name FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN account a ON s.registered_account_id = a.account_id "
                + "WHERE s.deleted = false");
        List<Object> params = new ArrayList<>();

        // 検索条件: 期間From (売上日)
        if (condition.getDateFrom() != null) {
            sql.append(" AND s.sale_date >= ?");
            params.add(Date.valueOf(condition.getDateFrom()));
        }
        // 検索条件: 期間To (売上日)
        if (condition.getDateTo() != null) {
            sql.append(" AND s.sale_date <= ?");
            params.add(Date.valueOf(condition.getDateTo()));
        }
        // 検索条件: 登録したスタッフ名 (部分一致検索)
        if (condition.getStaffName() != null && !condition.getStaffName().isBlank()) {
            sql.append(" AND a.staff_name LIKE ?");
            params.add("%" + condition.getStaffName() + "%");
        }
        // 検索条件: 金額From (販売時単価 * 数量で算出された合計金額が対象)
        if (condition.getAmountFrom() != null) {
            sql.append(" AND (s.unit_price * s.quantity) >= ?");
            params.add(condition.getAmountFrom());
        }
        // 検索条件: 金額To (販売時単価 * 数量で算出された合計金額が対象)
        if (condition.getAmountTo() != null) {
            sql.append(" AND (s.unit_price * s.quantity) <= ?");
            params.add(condition.getAmountTo());
        }
        // 検索条件: 商品ID
        if (condition.getProductId() != null && condition.getProductId() > 0) {
            sql.append(" AND s.product_id = ?");
            params.add(condition.getProductId());
        }

        // ソート列のホワイトリスト判定（SQLインジェクション対策）
        String sortColumn = "s.sale_date";
        if ("product_name".equals(condition.getSortBy())) {
            sortColumn = "p.product_name";
        } else if ("quantity".equals(condition.getSortBy())) {
            sortColumn = "s.quantity";
        } else if ("amount".equals(condition.getSortBy())) {
            sortColumn = "(s.unit_price * s.quantity)";
        } else if ("staff_name".equals(condition.getSortBy())) {
            sortColumn = "a.staff_name";
        }

        String sortOrder = "DESC";
        if ("asc".equalsIgnoreCase(condition.getOrder())) {
            sortOrder = "ASC";
        }
        sql.append(" ORDER BY ").append(sortColumn).append(" ").append(sortOrder).append(", s.sales_id DESC");

        List<Sale> sales = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sales.add(map(rs));
                }
            }
        }
        return sales;
    }

    /**
     * 売上IDをキーにして、アクティブな（未削除の）売上データを1件取得します。
     * 過去に登録したアカウントや商品が削除されていたとしても、JOINによって商品名や登録者スタッフ名は取得可能にしています。
     * 
     * @param id 売上ID
     * @return 売上情報を格納した Optional オブジェクト
     * @throws SQLException データベース処理に失敗した場合
     */
    public Optional<Sale> findActiveById(int id) throws SQLException {
        String sql = "SELECT s.*, p.product_name, a.staff_name FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN account a ON s.registered_account_id = a.account_id "
                + "WHERE s.sales_id = ? AND s.deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * 新しい売上データを登録します。
     * 【重要】引数の `sale` に含まれる `unit_price` は、Servlet側で商品マスタから取得した最新の価格です。
     * これを `unit_price` カラムへコピーしてINSERTすることで、販売時点の価格を恒久的に固定します。
     * 
     * @param sale 登録する売上情報
     * @throws SQLException データベース処理に失敗した場合
     */
    public void insert(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales_transaction "
                + "(sale_date, product_id, quantity, unit_price, memo, registered_account_id, last_updated_account_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(sale.getSaleDate()));
            ps.setInt(2, sale.getProductId());
            ps.setInt(3, sale.getQuantity());
            ps.setInt(4, sale.getUnitPrice());
            ps.setString(5, sale.getMemo());
            ps.setInt(6, sale.getRegisteredAccountId());
            ps.setInt(7, sale.getLastUpdatedAccountId());
            ps.executeUpdate();
        }
    }

    /**
     * 売上データの「メモ（memo）」のみを更新し、同時に最終更新者を更新します。
     * 「4.5 売上編集要件（商品、数量、売上日、販売時単価は編集不可）」を保証するための実装です。
     * 
     * @param id 売上ID
     * @param memo 新しいメモの内容
     * @param accountId 更新を行ったアカウントID
     * @throws SQLException データベース処理に失敗した場合
     */
    public void updateMemo(int id, String memo, int accountId) throws SQLException {
        String sql = "UPDATE sales_transaction SET memo = ?, last_updated_account_id = ? "
                + "WHERE sales_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, memo);
            ps.setInt(2, accountId);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * 売上データを論理削除（deleted = true）します。
     * 
     * @param id 削除する売上ID
     * @param accountId 削除操作を行ったアカウントID
     * @throws SQLException データベース処理に失敗した場合
     */
    public void softDelete(int id, int accountId) throws SQLException {
        String sql = "UPDATE sales_transaction SET deleted = true, last_updated_account_id = ? WHERE sales_id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * 当日の売上合計金額を取得します（ダッシュボード表示用）。
     * 削除済みのデータを除外し、`CURDATE()` 関数を用いてデータベースの現在日付と一致するレコードを集計します。
     * 
     * @return 本日の売上合計額
     * @throws SQLException データベース処理に失敗した場合
     */
    public int getTodaySalesAmount() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM sales_transaction "
                + "WHERE sale_date = CURDATE() AND deleted = false";
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * 今月（当月1日〜今日）の売上合計金額を取得します（ダッシュボード表示用）。
     * `DATE_FORMAT(CURDATE(), '%Y-%m-01')` で当月の初日を取得し、それ以降の売上を集計します。
     * 
     * @return 今月の売上合計額
     * @throws SQLException データベース処理に失敗した場合
     */
    public int getMonthSalesAmount() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM sales_transaction "
                + "WHERE sale_date >= DATE_FORMAT(CURDATE(), '%Y-%m-01') AND deleted = false";
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * 直近7日間（今日を含む過去7日間）の、日別の売上金額を取得します（ダッシュボードの折れ線グラフ用）。
     * 売上が存在しない日付に対しても「0円」として描画できるよう、Java側であらかじめ7日分の枠を空データ（0円）で初期化し、
     * DBから取得できた実績金額をそこに上書き充当する設計となっています。
     * 
     * @return 直近7日間の日別売上推移リスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Sale> getRecent7DaysSales() throws SQLException {
        java.time.LocalDate today = java.time.LocalDate.now();
        List<Sale> recentSales = new ArrayList<>();
        // 過去6日前から本日までの7日分を、売上額0円として初期設定
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            Sale s = new Sale();
            s.setSaleDate(d);
            s.setUnitPrice(0); // 金額の格納用として一時的に使用
            s.setQuantity(1);  // 数量1として掛け合わせて0になるように
            recentSales.add(s);
        }

        String sql = "SELECT sale_date, SUM(quantity * unit_price) AS daily_amount "
                + "FROM sales_transaction "
                + "WHERE sale_date >= ? AND sale_date <= ? AND deleted = false "
                + "GROUP BY sale_date";

        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today.minusDays(6)));
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate sd = rs.getDate("sale_date").toLocalDate();
                    int amount = rs.getInt("daily_amount");
                    // 初期設定リストの日付と合致する箇所に、DBから得られた金額をセット
                    for (Sale s : recentSales) {
                        if (s.getSaleDate().equals(sd)) {
                            s.setUnitPrice(amount); // unit_price メンバ変数を日別売上額の退避用として再利用
                            break;
                        }
                    }
                }
            }
        }
        return recentSales;
    }

    /**
     * PreparedStatement に対する、動的な引数リストの一括バインド処理。
     */
    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /**
     * ResultSet から Sale モデルへのマッピング。
     */
    private Sale map(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("sales_id"));
        s.setSaleDate(rs.getDate("sale_date").toLocalDate());
        s.setProductId(rs.getInt("product_id"));
        s.setProductName(rs.getString("product_name"));
        s.setQuantity(rs.getInt("quantity"));
        s.setUnitPrice(rs.getInt("unit_price"));
        s.setMemo(rs.getString("memo"));
        s.setRegisteredAccountId(rs.getInt("registered_account_id"));
        s.setRegisteredStaffName(rs.getString("staff_name"));
        s.setLastUpdatedAccountId(rs.getInt("last_updated_account_id"));
        s.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return s;
    }

    /**
     * 指定された検索条件に合致する、アクティブな売上データの総件数を取得します（ページング用）。
     * 
     * @param condition 検索条件オブジェクト
     * @return 該当件数
     * @throws SQLException データベース処理に失敗した場合
     */
    public int count(SaleSearchCondition condition) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN account a ON s.registered_account_id = a.account_id "
                + "WHERE s.deleted = false");
        List<Object> params = new ArrayList<>();

        if (condition.getDateFrom() != null) {
            sql.append(" AND s.sale_date >= ?");
            params.add(Date.valueOf(condition.getDateFrom()));
        }
        if (condition.getDateTo() != null) {
            sql.append(" AND s.sale_date <= ?");
            params.add(Date.valueOf(condition.getDateTo()));
        }
        if (condition.getStaffName() != null && !condition.getStaffName().isBlank()) {
            sql.append(" AND a.staff_name LIKE ?");
            params.add("%" + condition.getStaffName() + "%");
        }
        if (condition.getAmountFrom() != null) {
            sql.append(" AND (s.unit_price * s.quantity) >= ?");
            params.add(condition.getAmountFrom());
        }
        if (condition.getAmountTo() != null) {
            sql.append(" AND (s.unit_price * s.quantity) <= ?");
            params.add(condition.getAmountTo());
        }
        if (condition.getProductId() != null && condition.getProductId() > 0) {
            sql.append(" AND s.product_id = ?");
            params.add(condition.getProductId());
        }

        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * ページング指定付きで、指定された検索条件に合致する売上データリストを取得します。
     * 
     * @param condition 検索条件オブジェクト
     * @param page 取得対象ページ（1始まり）
     * @param pageSize 1ページあたりの件数
     * @return 該当ページの売上リスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Sale> search(SaleSearchCondition condition, int page, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, p.product_name, a.staff_name FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN account a ON s.registered_account_id = a.account_id "
                + "WHERE s.deleted = false");
        List<Object> params = new ArrayList<>();

        if (condition.getDateFrom() != null) {
            sql.append(" AND s.sale_date >= ?");
            params.add(Date.valueOf(condition.getDateFrom()));
        }
        if (condition.getDateTo() != null) {
            sql.append(" AND s.sale_date <= ?");
            params.add(Date.valueOf(condition.getDateTo()));
        }
        if (condition.getStaffName() != null && !condition.getStaffName().isBlank()) {
            sql.append(" AND a.staff_name LIKE ?");
            params.add("%" + condition.getStaffName() + "%");
        }
        if (condition.getAmountFrom() != null) {
            sql.append(" AND (s.unit_price * s.quantity) >= ?");
            params.add(condition.getAmountFrom());
        }
        if (condition.getAmountTo() != null) {
            sql.append(" AND (s.unit_price * s.quantity) <= ?");
            params.add(condition.getAmountTo());
        }
        if (condition.getProductId() != null && condition.getProductId() > 0) {
            sql.append(" AND s.product_id = ?");
            params.add(condition.getProductId());
        }

        String sortColumn = "s.sale_date";
        if ("product_name".equals(condition.getSortBy())) {
            sortColumn = "p.product_name";
        } else if ("quantity".equals(condition.getSortBy())) {
            sortColumn = "s.quantity";
        } else if ("amount".equals(condition.getSortBy())) {
            sortColumn = "(s.unit_price * s.quantity)";
        } else if ("staff_name".equals(condition.getSortBy())) {
            sortColumn = "a.staff_name";
        }

        String sortOrder = "DESC";
        if ("asc".equalsIgnoreCase(condition.getOrder())) {
            sortOrder = "ASC";
        }
        sql.append(" ORDER BY ").append(sortColumn).append(" ").append(sortOrder).append(", s.sales_id DESC");
        
        sql.append(" LIMIT ? OFFSET ?");
        int offset = (page - 1) * pageSize;
        params.add(pageSize);
        params.add(offset);

        List<Sale> sales = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sales.add(map(rs));
                }
            }
        }
        return sales;
    }

    /**
     * カテゴリ別の売上集計データを取得します（ダッシュボードの円グラフ用）。
     * `SUM(quantity * unit_price)` を用い、売上高が大きい順にソートします。
     * 
     * @return カテゴリごとの売上合計リスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<CategorySales> getCategorySales() throws SQLException {
        String sql = "SELECT c.category_name, COALESCE(SUM(s.quantity * s.unit_price), 0) AS total_amount "
                + "FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE s.deleted = false "
                + "GROUP BY c.category_id, c.category_name "
                + "ORDER BY total_amount DESC";
        List<CategorySales> list = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CategorySales(
                    rs.getString("category_name"),
                    rs.getLong("total_amount")
                ));
            }
        }
        return list;
    }

    /**
     * 売上金額の高い商品を上位から指定件数取得します（ダッシュボードの売れ筋ランキング用）。
     * 
     * @param limit 取得件数上限
     * @return 商品売上ランキングのリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<ProductSales> getTopProductSales(int limit) throws SQLException {
        String sql = "SELECT p.product_name, COALESCE(SUM(s.quantity * s.unit_price), 0) AS total_amount "
                + "FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "WHERE s.deleted = false "
                + "GROUP BY p.product_id, p.product_name "
                + "ORDER BY total_amount DESC "
                + "LIMIT ?";
        List<ProductSales> list = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProductSales(
                        rs.getString("product_name"),
                        rs.getLong("total_amount")
                    ));
                }
            }
        }
        return list;
    }
}

