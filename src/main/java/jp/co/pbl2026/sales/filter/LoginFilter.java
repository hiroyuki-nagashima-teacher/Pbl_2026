package jp.co.pbl2026.sales.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.util.AuthUtil;

/**
 * 【模範解答解説: ログイン認証フィルター (LoginFilter)】
 * 全てのリクエスト（"/*"）に対して割り込み処理を行い、セッション上の認証状態をチェックします。
 * 要件定義書「5.2 ログイン後の画面は未ログイン状態ではアクセス不可とする（セッション認証）」を実現するためのセキュリティ基盤です。
 * 
 * ■ 設計・実装のポイント:
 * 1. 認証チェックの一元管理:
 *    各Servletに認証チェックを記述するのではなく、Filterで一元化することで、認証漏れバグを防ぎ、保守性を高めます。
 * 2. 公開パス（publicPath）の除外設定:
 *    ログイン画面自体、ログアウト処理、およびCSSや画像等の静的リソース（`/assets/*`）は、
 *    未ログイン状態でもアクセスを許可（`chain.doFilter` でスルー）する必要があります。
 */
@WebFilter("/*")
public class LoginFilter implements Filter {

    /**
     * リクエストごとのフィルタリング処理を実行します。
     * 未ログイン状態で非公開パスにアクセスした場合は、ログイン画面（`/login`）へリダイレクトさせます。
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getServletPath();

        // 未ログイン状態でもアクセスを許可する公開パスの判定
        boolean publicPath = path.equals("/login") || path.equals("/logout")
                || path.startsWith("/assets/") || path.equals("");
                
        // 公開パスへのアクセスであるか、またはセッションに認証済みアカウントが存在する場合のみ、次の処理へ進む
        if (publicPath || AuthUtil.currentAccount(req) != null) {
            chain.doFilter(request, response);
            return;
        }
        
        // 認証されていない場合はログイン画面へリダイレクト（セキュリティ制御）
        res.sendRedirect(req.getContextPath() + "/login");
    }

    /**
     * フィルターの初期化処理。今回は特に行う処理はありませんが、インターフェースの規定に従って実装します。
     */
    @Override
    public void init(javax.servlet.FilterConfig filterConfig) throws javax.servlet.ServletException {}

    /**
     * フィルターの破棄処理。
     */
    @Override
    public void destroy() {}
}
