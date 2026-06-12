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

@WebServlet(urlPatterns = {"/accounts", "/accounts/new", "/accounts/edit", "/accounts/delete"})
public class AccountServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final AccountDao accountDao = new AccountDao();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        String path = req.getServletPath();
        if ("/accounts".equals(path)) {
            req.setAttribute("accounts", accountDao.findAllActive());
            forward(req, res, "account/list.jsp");
        } else if ("/accounts/new".equals(path)) {
            showForm(req, res, new Account(), errors(), false);
        } else if ("/accounts/edit".equals(path)) {
            Account account = accountDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            showForm(req, res, account, errors(), true);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        String path = req.getServletPath();
        if ("/accounts/new".equals(path)) {
            save(req, res, null);
        } else if ("/accounts/edit".equals(path)) {
            Account account = accountDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            save(req, res, account.getId());
        } else if ("/accounts/delete".equals(path)) {
            delete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse res, Integer accountId)
            throws SQLException, ServletException, IOException {
        Account account = accountFromRequest(req);
        if (accountId != null) {
            account.setId(accountId);
        }
        boolean edit = accountId != null;
        Map<String, String> errors = validateAccount(req, account, edit);
        boolean updatePassword = !trim(req.getParameter("password")).isEmpty();
        if (edit) {
            Account before = accountDao.findActiveById(accountId).orElseThrow(ForbiddenException::new);
            if (before.isManager() && Account.ROLE_STAFF.equals(account.getRole())
                    && accountDao.activeManagerCount() <= 1) {
                errors.put("role", "店長アカウントが1件しかないため、売上登録専用へ変更できません。");
            }
        }
        if (!errors.isEmpty()) {
            showForm(req, res, account, errors, edit);
            return;
        }
        if (!edit || updatePassword) {
            account.setPasswordHash(PasswordUtil.hash(req.getParameter("password")));
        }
        if (edit) {
            accountDao.update(account, updatePassword);
            AuthUtil.flash(req, "アカウントを更新しました。");
        } else {
            accountDao.insert(account);
            AuthUtil.flash(req, "アカウントを追加しました。");
        }
        redirect(req, res, "/accounts");
    }

    private void delete(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        int accountId = id(req);
        Account target = accountDao.findActiveById(accountId).orElseThrow(ForbiddenException::new);
        if (target.getId() == AuthUtil.currentAccount(req).getId()) {
            AuthUtil.flash(req, "自分自身のアカウントは削除できません。");
        } else if (target.isManager() && accountDao.activeManagerCount() <= 1) {
            AuthUtil.flash(req, "店長アカウントが1件しかないため削除できません。");
        } else {
            accountDao.softDelete(accountId);
            AuthUtil.flash(req, "アカウントを削除しました。");
        }
        redirect(req, res, "/accounts");
    }

    private Account accountFromRequest(HttpServletRequest req) {
        Account account = new Account();
        account.setLoginId(trim(req.getParameter("loginId")));
        account.setStaffName(trim(req.getParameter("staffName")));
        account.setRole(trim(req.getParameter("role")));
        return account;
    }

    private Map<String, String> validateAccount(HttpServletRequest req, Account account, boolean edit)
            throws SQLException {
        Map<String, String> errors = errors();
        Integer exceptId = edit ? account.getId() : null;
        String password = trim(req.getParameter("password"));
        if (account.getLoginId().length() < 4 || account.getLoginId().length() > 50) {
            errors.put("loginId", "ログインIDは4〜50文字で入力してください。");
        } else if (accountDao.existsLoginId(account.getLoginId(), exceptId)) {
            errors.put("loginId", "このログインIDは既に使われています。");
        }
        if (account.getStaffName().isEmpty() || account.getStaffName().length() > 50) {
            errors.put("staffName", "スタッフ名は1〜50文字で入力してください。");
        } else if (accountDao.existsStaffName(account.getStaffName(), exceptId)) {
            errors.put("staffName", "このスタッフ名は既に使われています。");
        }
        if ((!edit || !password.isEmpty()) && password.length() < 8) {
            errors.put("password", "パスワードは8文字以上で入力してください。");
        }
        if (!Account.ROLE_MANAGER.equals(account.getRole()) && !Account.ROLE_STAFF.equals(account.getRole())) {
            errors.put("role", "ロールを選択してください。");
        }
        return errors;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse res, Account account,
            Map<String, String> errors, boolean edit) throws ServletException, IOException {
        req.setAttribute("account", account);
        req.setAttribute("errors", errors);
        req.setAttribute("edit", edit);
        forward(req, res, "account/form.jsp");
    }
}
