package jp.co.pbl2026.sales.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jp.co.pbl2026.sales.model.Account;

/**
 * 【模範解答解説: 認証・認可ユーティリティ (AuthUtil)】
 * セッション情報のアクセス（ログインアカウントの取得、ロール判定）および、
 * 画面遷移間で一度だけメッセージを表示するための「フラッシュメッセージ」の管理を一元化します。
 * 
 * ■ 設計・実装のポイント:
 * 1. セッションアクセスのカプセル化（保守性・安全性の向上）:
 *    セッションのキー文字列（`"loginAccount"` 等）や、セッションの生成/取得処理（`request.getSession(false)` 等）
 *    をこのユーティリティ内に隠蔽することで、呼び出し元コードの記述ミスを防止し、簡潔に保ちます。
 * 2. 認可チェックの厳格化（セキュリティ非機能要件）:
 *    `requireManager` で店長権限がない場合に `ForbiddenException` をスローします。この例外は `BaseServlet` で一括キャッチされ、
 *    自動的に403エラー画面へ遷移します。
 * 3. フラッシュメッセージによる通知の実現（UX向上）:
 *    セッションを利用してメッセージを一時保存し、一度画面に表示したら即時にセッションから削除（`consumeFlash`）することで、
 *    再読み込み（F5）時に同じ完了メッセージが二重に表示されるのを防ぎます。
 */
public final class AuthUtil {
    /** セッションに格納するログイン済みアカウントオブジェクトのキー名 */
    public static final String LOGIN_ACCOUNT = "loginAccount";
    /** セッションに格納するフラッシュメッセージ（通知情報）のキー名 */
    public static final String FLASH = "flash";

    /** インスタンス化を禁止するプライベートコンストラクタ */
    private AuthUtil() {}

    /**
     * 現在セッションにログインしているアカウント情報を取得します。
     * 未ログインの場合は null を返します。
     * 
     * @param request HTTPリクエストオブジェクト
     * @return ログイン済みのアカウント情報、未ログインの場合は null
     */
    public static Account currentAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // 新規セッションを作らず、既存セッションのみを取得
        return session == null ? null : (Account) session.getAttribute(LOGIN_ACCOUNT);
    }

    /**
     * 現在ログインしているユーザーが「店長 (MANAGER)」ロールであるかを判定します。
     * 
     * @param request HTTPリクエストオブジェクト
     * @return 店長の場合は true、それ以外（一般スタッフまたは未ログイン）の場合は false
     */
    public static boolean isManager(HttpServletRequest request) {
        Account account = currentAccount(request);
        return account != null && account.isManager();
    }

    /**
     * 「店長 (MANAGER)」権限を必須チェックし、権限がない場合は ForbiddenException（403エラー用ランタイム例外）を発生させます。
     * 店長限定の操作を行うコントローラー（アカウント管理、商品登録・編集・削除など）の処理開始時に呼び出されます。
     * 
     * @param request HTTPリクエストオブジェクト
     * @throws ForbiddenException 操作アカウントに店長権限がない場合
     */
    public static void requireManager(HttpServletRequest request) {
        if (!isManager(request)) {
            throw new ForbiddenException();
        }
    }

    /**
     * 画面遷移間で引き継ぐメッセージ（登録・更新・削除完了等のフラッシュメッセージ）をセッションに一時保存します。
     * 
     * @param request HTTPリクエストオブジェクト
     * @param message 画面に表示するメッセージ
     */
    public static void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH, message);
    }

    /**
     * セッションに格納された一時メッセージを取得し、取得と同時にセッションから破棄（消費）します。
     * `BaseServlet` 内でリクエストごとに自動処理され、画面側（JSP）へ連携されます。
     * 
     * @param request HTTPリクエストオブジェクト
     * @return 一時メッセージ、格納されていない場合は null
     */
    public static String consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        String message = (String) session.getAttribute(FLASH);
        session.removeAttribute(FLASH); // 一度取り出したら二重表示を防ぐために削除
        return message;
    }
}
