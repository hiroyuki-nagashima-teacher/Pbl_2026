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

@WebFilter("/*")
public class LoginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getServletPath();

        // 静的ファイルとログイン処理は未ログインでも通す。
        boolean publicPath = path.equals("/login") || path.equals("/logout")
                || path.startsWith("/assets/") || path.equals("");
        if (publicPath || AuthUtil.currentAccount(req) != null) {
            chain.doFilter(request, response);
            return;
        }
        res.sendRedirect(req.getContextPath() + "/login");
    }

    @Override
    public void init(javax.servlet.FilterConfig filterConfig) throws javax.servlet.ServletException {}

    @Override
    public void destroy() {}
}
