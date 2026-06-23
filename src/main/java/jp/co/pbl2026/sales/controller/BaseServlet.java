package jp.co.pbl2026.sales.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.util.AuthUtil;
import jp.co.pbl2026.sales.util.ForbiddenException;

/**
 * 【模範解答解説: 基底サーブレットクラス (BaseServlet)】
 * 本システムで作成するすべてのサーブレット（ログイン後画面）の親となる抽象クラスです。
 * 
 * ■ 設計・実装のポイント:
 * 1. 共通の例外ハンドリングの統一化:
 *    各子クラスで発生する SQLException や 認可例外（ForbiddenException）を一括でキャッチし、
 *    それぞれ適切なエラーコード（403等）やエラーページ（error/403.jsp）へ遷移させます。これにより、
 *    各Controller内部に冗長な try-catch ブロックを書く必要がなくなります。
 * 2. GET/POSTハンドリングの標準化:
 *    doGet/doPost メソッドを final で閉じ、共通の execute() を通じて handleGet/handlePost に処理を振り分けることで、
 *    リクエスト全体のエンコーディング設定やフラッシュメッセージ（通知機能）の自動割り当てを可能にしています。
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Servlet コンテナ（Tomcat）から呼び出される GET リクエストハンドラ。
     * リクエスト共通実行メソッド `execute` に委譲します。
     */
    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        execute(req, res, false);
    }

    /**
     * Servlet コンテナから呼び出される POST リクエストハンドラ。
     * エンコーディングを UTF-8 に設定した上で `execute` に委譲します。
     */
    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        execute(req, res, true);
    }

    /**
     * 全てのリクエスト（GET/POST）に対する共通の実行制御メソッドです。
     * フラッシュメッセージの取り出し、`handleGet` / `handlePost` への振り分け、
     * および認可エラー（ForbiddenException）や DB例外（SQLException）の一元的な例外ハンドリングを処理します。
     */
    private void execute(HttpServletRequest req, HttpServletResponse res, boolean post)
            throws ServletException, IOException {
        try {
            // 一覧画面などに一度だけ表示する完了メッセージを、各Controller共通で取り出す。
            req.setAttribute("flash", AuthUtil.consumeFlash(req));
            if (post) {
                handlePost(req, res);
            } else {
                handleGet(req, res);
            }
        } catch (ForbiddenException e) {
            // 権限エラー時は HTTP 403 ステータスを設定し、エラーページへフォワード
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            forward(req, res, "error/403.jsp");
        } catch (SQLException e) {
            // データベースエラー時は ServletException にラップして上位（Webコンテナ）へ通知
            throw new ServletException("データベース処理に失敗しました。", e);
        }
    }

    /**
     * 子クラスでオーバーライドして実装する GET リクエスト個別ハンドラ。
     * デフォルトでは 404 Not Found を返します。
     */
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 子クラスでオーバーライドして実装する POST リクエスト個別ハンドラ。
     * デフォルトでは 404 Not Found を返します。
     */
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 指定されたJSPファイル（`/WEB-INF/jsp/` 配下）にリクエストをフォワードします。
     * URLの隠蔽と、Servletからの安全な画面遷移を実現します。
     * 
     * @param jsp JSPファイルの相対パス（例: "login.jsp", "product/list.jsp"）
     */
    protected void forward(HttpServletRequest req, HttpServletResponse res, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/" + jsp).forward(req, res);
    }

    /**
     * リクエストパラメータから "id" の値を取得し、数値（int）にキャストして返します。
     * 主に編集・削除時の主キー特定に使用されます。
     */
    protected int id(HttpServletRequest req) {
        return Integer.parseInt(req.getParameter("id"));
    }

    /**
     * クライアントへ指定された相対パスへのリダイレクト（302）を指示します。
     * コンテキストパスを自動的に付与してリダイレクトします。
     * 
     * @param path コンテキストルートからの相対パス（例: "/products"）
     */
    protected void redirect(HttpServletRequest req, HttpServletResponse res, String path)
            throws IOException {
        res.sendRedirect(req.getContextPath() + path);
    }
}
