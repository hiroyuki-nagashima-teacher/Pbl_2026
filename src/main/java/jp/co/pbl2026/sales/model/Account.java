package jp.co.pbl2026.sales.model;

import java.time.LocalDateTime;

/**
 * 【模範解答解説: アカウントエンティティクラス (Account)】
 * ログインアカウントおよびその権限（ロール）情報を管理するドメインモデル（JavaBean）です。
 * データベースの `account` テーブルのレコードと1対1で対応します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 定数によるロール名の管理:
 *    文字列リテラルによるスペルミスを防ぐため、ロール名の定数（`ROLE_MANAGER`/`ROLE_STAFF`）を定義しています。
 * 2. ドメインロジックのモデルカプセル化:
 *    「一般スタッフか店長か」という判断ロジックは、コントローラー等でロール文字列の比較を直接書くのではなく、
 *    `isManager()` をモデルのメソッドとして持たせることでモデル自身に判断責任をカプセル化しています。
 * 3. 画面表示用整形メソッドの提供:
 *    `getFormattedUpdatedAt()` のように、画面出力用の日付整形をモデル内に持たせることで、JSP側での日付フォーマット記述を簡潔にします。
 */
public class Account {
    /** 店長（システム管理者）ロール */
    public static final String ROLE_MANAGER = "MANAGER";
    /** 売上登録専用（一般スタッフ）ロール */
    public static final String ROLE_STAFF = "STAFF";

    private int id;
    private String loginId;
    private String staffName;
    private String passwordHash;
    private String role;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 現在のアカウントが店長（MANAGER）権限を持っているかどうかを判定します。
     * 
     * @return 店長権限を持つ場合は true、そうでない場合は false
     */
    public boolean isManager() {
        return ROLE_MANAGER.equals(role);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 更新日時を "yyyy/MM/dd HH:mm:ss" 形式のフォーマット済み文字列として取得します。
     * JSPの一覧画面で直接呼び出して表示する際に使用されます。
     * 
     * @return フォーマット済みの更新日時文字列
     */
    public String getFormattedUpdatedAt() {
        if (updatedAt == null) return "";
        return updatedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    }
}
