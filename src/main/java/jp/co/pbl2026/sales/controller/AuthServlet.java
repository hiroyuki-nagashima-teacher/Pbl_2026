package jp.co.pbl2026.sales.controller;

import static jp.co.pbl2026.sales.util.ValidationUtil.trim;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.dao.AccountDao;
import jp.co.pbl2026.sales.model.Account;
import jp.co.pbl2026.sales.util.AuthUtil;
import jp.co.pbl2026.sales.util.PasswordUtil;

/**
 * 【模範解答解説: 認証管理コントローラー (AuthServlet)】
 * 本サーブレットは、要件定義書「4.2 ログイン要件」に基づく、セッション管理とログイン・ログアウト処理を行います。
 * 
 * ■ 設計・実装のポイント:
 * 1. BaseServletによる共通基盤化:
 *    BaseServletを継承することで、各エンドポイントでの例外処理やフォワード等の共通処理をカプセル化しています。
 * 2. 削除済みアカウントのログイン拒否:
 *    AccountDao.findActiveByLoginId() により、論理削除されていない(deleted=FALSE)アクティブなアカウントのみを
 *    DBから取得するため、「4.2.3 削除済みアカウントはログイン不可」の要件をSQLの取得段階で保証しています。
 * 3. パスワードハッシュ照合によるセキュリティ保護:
 *    平文パスワードのDB保存を避け、入力値をハッシュ化（SHA-256）した上で、既存のパスワードハッシュと安全に照合しています。
 */
@WebServlet(urlPatterns = {"/login", "/logout"})
public class AuthServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    /** アカウントのデータ操作を担当するデータアクセスオブジェクト */
    private final AccountDao accountDao = new AccountDao();

    /**
     * HTTP GETリクエストを処理します。
     * ログアウト処理（セッション無効化）、またはログイン画面表示への遷移を制御します。
     */
    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        // ログアウト処理の制御
        if ("/logout".equals(req.getServletPath())) {
            // セッション破棄による確実なログアウト処理
            req.getSession().invalidate();
            redirect(req, res, "/login");
            return;
        }
        // ログイン画面の表示
        forward(req, res, "login.jsp");
    }

    /**
     * HTTP POSTリクエストを処理します。
     * 入力されたログインIDとパスワードの検証を行い、認証成功時はセッションへ情報を格納してダッシュボードへリダイレクトします。
     */
    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        // 入力値の受け取り（ログインIDは前後の空白文字をトリムして不整合を防ぐ）
        String loginId = trim(req.getParameter("loginId"));
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        
        // 削除されていないアクティブなユーザーをDBから検索
        Optional<Account> account = accountDao.findActiveByLoginId(loginId);
        
        // アカウントの存在確認およびパスワードハッシュ照合（セキュリティ要件）
        if (account.isPresent() && PasswordUtil.matches(password, account.get().getPasswordHash())) {
            // セッションへのアカウント情報格納（ログインフィルターでの認証チェック用）
            req.getSession().setAttribute(AuthUtil.LOGIN_ACCOUNT, account.get());
            redirect(req, res, "/dashboard");
            return;
        }
        
        // 認証失敗時のエラー処理
        req.setAttribute("error", "ログインIDまたはパスワードが正しくありません。");
        req.setAttribute("loginId", loginId);
        forward(req, res, "login.jsp");
    }
}
