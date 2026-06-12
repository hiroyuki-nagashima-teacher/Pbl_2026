package jp.co.pbl2026.sales.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.util.AuthUtil;
import jp.co.pbl2026.sales.util.ForbiddenException;

public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        execute(req, res, false);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        execute(req, res, true);
    }

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
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            forward(req, res, "error/403.jsp");
        } catch (SQLException e) {
            throw new ServletException("データベース処理に失敗しました。", e);
        }
    }

    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void forward(HttpServletRequest req, HttpServletResponse res, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/" + jsp).forward(req, res);
    }

    protected int id(HttpServletRequest req) {
        return Integer.parseInt(req.getParameter("id"));
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse res, String path)
            throws IOException {
        res.sendRedirect(req.getContextPath() + path);
    }
}
