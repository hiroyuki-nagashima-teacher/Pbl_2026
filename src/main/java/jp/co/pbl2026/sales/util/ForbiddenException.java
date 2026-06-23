package jp.co.pbl2026.sales.util;

/**
 * 【模範解答解説: 認可エラー（403 Forbidden）用カスタム例外クラス】
 * ログインは完了しているものの、アクセスしようとした画面や操作に対して必要な権限（例: 店長権限）
 * を持たない場合にスローされる非チェック例外（RuntimeException）です。
 * 
 * 基底コントローラーである `BaseServlet.execute` で一元的にキャッチされ、
 * HTTPステータス 403 (SC_FORBIDDEN) の設定および `error/403.jsp` への画面遷移をトリガーします。
 */
public class ForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
