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

@WebServlet(urlPatterns = {"/login", "/logout"})
public class AuthServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final AccountDao accountDao = new AccountDao();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        if ("/logout".equals(req.getServletPath())) {
            req.getSession().invalidate();
            redirect(req, res, "/login");
            return;
        }
        forward(req, res, "login.jsp");
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String loginId = trim(req.getParameter("loginId"));
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        Optional<Account> account = accountDao.findActiveByLoginId(loginId);
        if (account.isPresent() && PasswordUtil.matches(password, account.get().getPasswordHash())) {
            req.getSession().setAttribute(AuthUtil.LOGIN_ACCOUNT, account.get());
            redirect(req, res, "/dashboard");
            return;
        }
        req.setAttribute("error", "ログインIDまたはパスワードが正しくありません。");
        req.setAttribute("loginId", loginId);
        forward(req, res, "login.jsp");
    }
}
