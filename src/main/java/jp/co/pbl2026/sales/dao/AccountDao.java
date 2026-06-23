package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.pbl2026.sales.model.Account;

/**
 * 【模範解答解説: アカウントデータアクセスオブジェクト (AccountDao)】
 * アカウントテーブル（account）に対するCRUD処理およびビジネスルール検証用データ取得を担います。
 * 
 * ■ 設計・実装のポイント:
 * 1. 物理削除の排除（論理削除）:
 *    「7.4 削除方針」に基づき、アカウントの削除要求に対しては `deleted` フラグを `true` に更新する論理削除（`softDelete`）を行います。
 * 2. SQLインジェクションの防止:
 *    すべてのクエリにおいて、動的パラメータはプレースホルダ（`?`）を使用し、`PreparedStatement` で安全にバインドします。
 * 3. 単一「店長」制限のための集計機能:
 *    唯一の店長アカウントが削除または格下げされないように、店長の生存数を把握するための `activeManagerCount()` メソッドを定義しています。
 */
public class AccountDao {

    /**
     * ログインIDをキーにして、論理削除されていない（アクティブな）アカウント情報を取得します。
     * 「4.2 ログイン要件」における認証処理の初期フェーズで呼び出されます。
     * 
     * @param loginId ログインID
     * @return アカウント情報を格納した Optional オブジェクト（存在しない場合は empty）
     * @throws SQLException データベース処理に失敗しました。
     */
    public Optional<Account> findActiveByLoginId(String loginId) throws SQLException {
        String sql = "SELECT * FROM account WHERE login_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * アカウントIDをキーにして、論理削除されていないアカウント情報を取得します。
     * アカウント編集画面の初期表示処理などで呼び出されます。
     * 
     * @param id アカウントID
     * @return アカウント情報を格納した Optional オブジェクト
     * @throws SQLException データベース処理に失敗した場合
     */
    public Optional<Account> findActiveById(int id) throws SQLException {
        String sql = "SELECT * FROM account WHERE account_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * 論理削除されていないすべてのアクティブなアカウント情報を昇順で取得します。
     * 「S-012 アカウント一覧画面」の表示処理で呼び出されます。
     * 
     * @return アクティブなアカウントのリスト
     * @throws SQLException データベース処理に失敗した場合
     */
    public List<Account> findAllActive() throws SQLException {
        String sql = "SELECT * FROM account WHERE deleted = false ORDER BY account_id";
        List<Account> accounts = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(map(rs));
            }
        }
        return accounts;
    }

    /**
     * 指定されたログインIDがDB内に既に存在するかどうかを確認します（一意性検証）。
     * 自分自身のIDは除外してチェックできるように `exceptId` を指定できます。
     * 
     * @param loginId 検証するログインID
     * @param exceptId 重複チェックから除外する自身のアカウントID（新規登録時は null）
     * @return 既に存在する場合は true、存在しない場合は false
     * @throws SQLException データベース処理に失敗した場合
     */
    public boolean existsLoginId(String loginId, Integer exceptId) throws SQLException {
        return exists("login_id", loginId, exceptId);
    }

    /**
     * 指定されたスタッフ名がDB内に既に存在するかどうかを確認します（一意性検証）。
     * ログインIDと同様に `exceptId` を用いた除外チェックが可能です。
     * 
     * @param staffName 検証するスタッフ名
     * @param exceptId 重複チェックから除外する自身のアカウントID（新規登録時は null）
     * @return 既に存在する場合は true、存在しない場合は false
     * @throws SQLException データベース処理に失敗した場合
     */
    public boolean existsStaffName(String staffName, Integer exceptId) throws SQLException {
        return exists("staff_name", staffName, exceptId);
    }

    /**
     * 新しいアカウントをデータベースにインサートします。
     * 
     * @param account 登録するアカウント情報
     * @throws SQLException データベース処理に失敗した場合
     */
    public void insert(Account account) throws SQLException {
        String sql = "INSERT INTO account (login_id, staff_name, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, account.getLoginId());
            ps.setString(2, account.getStaffName());
            ps.setString(3, account.getPasswordHash());
            ps.setString(4, account.getRole());
            ps.executeUpdate();
        }
    }

    /**
     * アカウント情報を更新します。
     * パスワードが入力されている場合とそうでない場合（更新なし）でSQLを切り替えます。
     * 
     * @param account 更新するアカウント情報
     * @param updatePassword パスワードを更新する場合は true
     * @throws SQLException データベース処理に失敗した場合
     */
    public void update(Account account, boolean updatePassword) throws SQLException {
        String sql = updatePassword
                ? "UPDATE account SET login_id = ?, staff_name = ?, password_hash = ?, role = ? WHERE account_id = ? AND deleted = false"
                : "UPDATE account SET login_id = ?, staff_name = ?, role = ? WHERE account_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, account.getLoginId());
            ps.setString(2, account.getStaffName());
            if (updatePassword) {
                ps.setString(3, account.getPasswordHash());
                ps.setString(4, account.getRole());
                ps.setInt(5, account.getId());
            } else {
                ps.setString(3, account.getRole());
                ps.setInt(4, account.getId());
            }
            ps.executeUpdate();
        }
    }

    /**
     * 指定されたアカウントIDのアカウントを論理削除（deleted = true）します。
     * 物理削除を行わないことで、過去に登録された売上データ（登録者・最終更新者）の整合性を維持します。
     * 
     * @param id 削除するアカウントID
     * @throws SQLException データベース処理に失敗した場合
     */
    public void softDelete(int id) throws SQLException {
        String sql = "UPDATE account SET deleted = true WHERE account_id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * 論理削除されていないアクティブな「店長（MANAGER）」アカウントの数を返します。
     * 最後の1件の店長アカウントが削除または格下げされるのをチェックする際に呼び出されます。
     * 
     * @return アクティブな店長アカウントの数
     * @throws SQLException データベース処理に失敗した場合
     */
    public int activeManagerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM account WHERE deleted = false AND role = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Account.ROLE_MANAGER);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * 重複チェック処理の共通ロジック。
     * 指定されたカラムに対して、指定された値を持つアクティブなレコードが存在するかを調べます。
     */
    private boolean exists(String column, String value, Integer exceptId) throws SQLException {
        // exceptId が指定されている場合は、自分自身（該当ID）のレコードを除外してカウントします。
        String sql = "SELECT COUNT(*) FROM account WHERE " + column + " = ? AND deleted = false"
                + (exceptId == null ? "" : " AND account_id <> ?");
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            if (exceptId != null) {
                ps.setInt(2, exceptId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * ResultSet の現在行のデータを取り出し、Account モデルオブジェクトにマッピングして返します。
     */
    private Account map(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("account_id"));
        a.setLoginId(rs.getString("login_id"));
        a.setStaffName(rs.getString("staff_name"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setRole(rs.getString("role"));
        a.setDeleted(rs.getBoolean("deleted"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return a;
    }
}
