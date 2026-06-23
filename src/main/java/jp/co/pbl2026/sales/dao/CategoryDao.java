package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jp.co.pbl2026.sales.model.Category;

/**
 * 【模範解答解説: カテゴリーマスタデータアクセスオブジェクト (CategoryDao)】
 * カテゴリーマスタ（category_master）へのデータ操作を担当するクラスです。
 * 
 * ■ 設計・実装のポイント:
 * 1. 論理削除（deleted = false）の一貫したフィルタリング:
 *    要件定義書「7.4 削除方針」に基づき、アプリケーション側から削除されたとみなされるカテゴリーデータを除外して検索を行います。
 * 2. 表示順（display_order）による並び替え:
 *    マスタ参照時に画面上の順序を制御するため、SQLで明示的に `display_order` および `category_id` を昇順ソートします。
 */
public class CategoryDao {

    /**
     * 論理削除されていないすべてのアクティブなカテゴリーを、指定された表示順で取得します。
     * 商品の登録・編集画面等で、カテゴリー選択肢の生成に利用されます。
     * 
     * @return アクティブなカテゴリーのリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Category> findAllActive() throws SQLException {
        // 論理削除（deleted = true）されたデータを除外するSQL文
        String sql = "SELECT * FROM category_master WHERE deleted = false ORDER BY display_order, category_id";
        List<Category> categories = new ArrayList<>();
        
        // try-with-resources 構文により、Connection, PreparedStatement, ResultSet を自動的かつ確実にクローズします（リソースリークの防止）。
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("category_id"));
                c.setName(rs.getString("category_name"));
                c.setDisplayOrder(rs.getInt("display_order"));
                categories.add(c);
            }
        }
        return categories;
    }

    /**
     * 指定されたカテゴリーIDを持つアクティブなカテゴリーが存在するかどうかを確認します。
     * 商品追加・編集時のバリデーション処理で、実在しない・あるいは削除済みのカテゴリーIDが指定されるのを防ぐために使用されます。
     * 
     * @param id カテゴリーID
     * @return 存在する場合は true、存在しない、または論理削除済みの場合は false
     * @throws SQLException データベース処理に失敗した場合
     */
    public boolean existsActive(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM category_master WHERE category_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                // 1件以上見つかれば存在する（アクティブ）と判定
                return rs.getInt(1) > 0;
            }
        }
    }
}
