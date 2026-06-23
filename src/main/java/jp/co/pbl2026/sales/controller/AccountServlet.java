package jp.co.pbl2026.sales.controller;

import static jp.co.pbl2026.sales.util.ValidationUtil.errors;
import static jp.co.pbl2026.sales.util.ValidationUtil.trim;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.dao.AccountDao;
import jp.co.pbl2026.sales.model.Account;
import jp.co.pbl2026.sales.util.AuthUtil;
import jp.co.pbl2026.sales.util.ForbiddenException;
import jp.co.pbl2026.sales.util.PasswordUtil;

/**
 * 【模範解答解説: アカウント管理コントローラー (AccountServlet)】
 * 本サーブレットは、要件定義書「4.1 権限要件」および「4.7 エラー・例外要件」に準拠した、
 * アカウントの登録・編集・削除処理を制御します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 店長のみへの厳格なアクセス制御:
 *    AuthUtil.requireManager(req) を handleGet/handlePost の最初で呼び出し、
 *    店長以外のロールのユーザーがURL直接入力等でアクセスした際に 403 Forbidden になるよう制限しています。
 * 2. 単一「店長」アカウント保護 of ビジネスルール:
 *    「7.4 アカウント削除の詳細」にある、店長アカウントが1件しかない場合には、
 *    自分自身や唯一の店長アカウントを削除・ロール格下げ（一般スタッフ化）できないように二重のチェックを実装しています。
 * 3. ユーザー情報の論理削除(Soft Delete):
 *    過去の売上実績データで登録者を辿れるよう、物理削除ではなく accountDao.softDelete() による
 *    論理削除（deletedフラグの更新）を採用しています。
 */
@WebServlet(urlPatterns = {"/accounts", "/accounts/new", "/accounts/edit", "/accounts/delete"})
public class AccountServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    /** アカウントのデータ操作を担当するデータアクセスオブジェクト */
    private final AccountDao accountDao = new AccountDao();

    /**
     * HTTP GETリクエストを処理します。
     * ルーティングを行い、アカウント一覧表示、新規登録フォーム表示、編集フォーム表示の制御を行います。
     * 各処理の実行前に、店長権限が必須であることを検証します。
     */
    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        // セキュリティ: 店長権限チェックを一元実行
        AuthUtil.requireManager(req);
        String path = req.getServletPath();
        if ("/accounts".equals(path)) {
            // アカウント一覧表示処理
            req.setAttribute("accounts", accountDao.findAllActive());
            forward(req, res, "account/list.jsp");
        } else if ("/accounts/new".equals(path)) {
            // 新規作成用の空オブジェクトと空エラーマップを渡し、追加画面を表示
            showForm(req, res, new Account(), errors(), false);
        } else if ("/accounts/edit".equals(path)) {
            // 編集対象のアカウント情報をIDから引き出し、編集画面を表示
            Account account = accountDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            showForm(req, res, account, errors(), true);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * HTTP POSTリクエストを処理します。
     * アカウントの保存（登録・更新）および削除処理へのルーティングを行います。
     */
    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        // セキュリティ: 店長権限チェックを一元実行
        AuthUtil.requireManager(req);
        String path = req.getServletPath();
        if ("/accounts/new".equals(path)) {
            // 新規保存処理
            save(req, res, null);
        } else if ("/accounts/edit".equals(path)) {
            // 編集対象の存在を確認した上で更新保存処理を実行
            Account account = accountDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            save(req, res, account.getId());
        } else if ("/accounts/delete".equals(path)) {
            // アカウント論理削除処理
            delete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * アカウントの登録または更新を行います。
     * バリデーション、パスワードハッシュ化、店長アカウント生存制限のチェックを担当します。
     * 
     * @param accountId 編集時のアカウントID（新規登録時は null）
     */
    private void save(HttpServletRequest req, HttpServletResponse res, Integer accountId)
            throws SQLException, ServletException, IOException {
        // リクエストから入力値をマッピングしたオブジェクトを生成
        Account account = accountFromRequest(req);
        if (accountId != null) {
            account.setId(accountId);
        }
        boolean edit = accountId != null;
        
        // バリデーションチェックの実行
        Map<String, String> errors = validateAccount(req, account, edit);
        boolean updatePassword = !trim(req.getParameter("password")).isEmpty();
        
        // ビジネスルール検証: 編集時に唯一の店長を一般スタッフへロール変更（格下げ）しようとしていないかチェック
        if (edit) {
            Account before = accountDao.findActiveById(accountId).orElseThrow(ForbiddenException::new);
            if (before.isManager() && Account.ROLE_STAFF.equals(account.getRole())
                    && accountDao.activeManagerCount() <= 1) {
                errors.put("role", "店長アカウントが1件しかないため、売上登録専用へ変更できません。");
            }
        }
        
        // バリデーションエラーがある場合は入力フォームに戻す
        if (!errors.isEmpty()) {
            showForm(req, res, account, errors, edit);
            return;
        }
        
        // 新規登録、またはパスワード入力がある場合は平文パスワードをハッシュ化（セキュリティ保護）
        if (!edit || updatePassword) {
            account.setPasswordHash(PasswordUtil.hash(req.getParameter("password")));
        }
        
        // 登録・更新の切り分け実行
        if (edit) {
            accountDao.update(account, updatePassword);
            AuthUtil.flash(req, "アカウントを更新しました。");
        } else {
            accountDao.insert(account);
            AuthUtil.flash(req, "アカウントを追加しました。");
        }
        redirect(req, res, "/accounts");
    }

    /**
     * アカウントを論理削除します。自分自身の削除防止、店長最低1件維持ルールを検証します。
     */
    private void delete(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        int accountId = id(req);
        Account target = accountDao.findActiveById(accountId).orElseThrow(ForbiddenException::new);
        
        // ビジネスルール検証: 自分自身のアカウント削除防止
        if (target.getId() == AuthUtil.currentAccount(req).getId()) {
            AuthUtil.flash(req, "自分自身のアカウントは削除できません。");
        } 
        // ビジネスルール検証: 唯一の店長アカウント削除防止（システム管理不能状態を防ぐ）
        else if (target.isManager() && accountDao.activeManagerCount() <= 1) {
            AuthUtil.flash(req, "店長アカウントが1件しかないため削除できません。");
        } 
        // ルール適合時は論理削除を実行
        else {
            accountDao.softDelete(accountId);
            AuthUtil.flash(req, "アカウントを削除しました。");
        }
        redirect(req, res, "/accounts");
    }

    /**
     * リクエストパラメータを Account モデルオブジェクトへ詰め替えます。
     * 前後の不要な空白はトリムして不正な入力を防ぎます。
     */
    private Account accountFromRequest(HttpServletRequest req) {
        Account account = new Account();
        account.setLoginId(trim(req.getParameter("loginId")));
        account.setStaffName(trim(req.getParameter("staffName")));
        account.setRole(trim(req.getParameter("role")));
        return account;
    }

    /**
     * アカウント入力項目のバリデーションチェック（文字数、重複登録など）を行います。
     * 要件定義「4.6 アカウント入力チェック要件」に対応します。
     * 
     * @param edit 編集画面での検証時には true、新規作成時は false
     */
    private Map<String, String> validateAccount(HttpServletRequest req, Account account, boolean edit)
            throws SQLException {
        Map<String, String> errors = errors();
        Integer exceptId = edit ? account.getId() : null;
        String password = trim(req.getParameter("password"));
        
        // ログインIDチェック: 文字数検証および一意性（重複）チェック
        if (account.getLoginId().length() < 4 || account.getLoginId().length() > 50) {
            errors.put("loginId", "ログインIDは4〜50文字で入力してください。");
        } else if (accountDao.existsLoginId(account.getLoginId(), exceptId)) {
            errors.put("loginId", "このログインIDは既に使われています。");
        }
        
        // スタッフ名チェック: 文字数検証および一意性チェック
        if (account.getStaffName().isEmpty() || account.getStaffName().length() > 50) {
            errors.put("staffName", "スタッフ名は1〜50文字で入力してください。");
        } else if (accountDao.existsStaffName(account.getStaffName(), exceptId)) {
            errors.put("staffName", "このスタッフ名は既に使われています。");
        }
        
        // パスワードチェック: 新規登録時は必須かつ8文字以上。編集時は空なら更新なしとし、入力された場合は8文字以上
        if ((!edit || !password.isEmpty()) && password.length() < 8) {
            errors.put("password", "パスワードは8文字以上で入力してください。");
        }
        
        // ロール選択の検証
        if (!Account.ROLE_MANAGER.equals(account.getRole()) && !Account.ROLE_STAFF.equals(account.getRole())) {
            errors.put("role", "ロールを選択してください。");
        }
        return errors;
    }

    /**
     * 入力オブジェクトやエラー情報をリクエストスコープに詰め、共通のアカウントフォームJSPへフォワードします。
     */
    private void showForm(HttpServletRequest req, HttpServletResponse res, Account account,
            Map<String, String> errors, boolean edit) throws ServletException, IOException {
        req.setAttribute("account", account);
        req.setAttribute("errors", errors);
        req.setAttribute("edit", edit);
        forward(req, res, "account/form.jsp");
    }
}
