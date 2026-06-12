package jp.co.pbl2026.sales.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jp.co.pbl2026.sales.model.Account;

public final class AuthUtil {
    public static final String LOGIN_ACCOUNT = "loginAccount";
    public static final String FLASH = "flash";

    private AuthUtil() {}

    public static Account currentAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Account) session.getAttribute(LOGIN_ACCOUNT);
    }

    public static boolean isManager(HttpServletRequest request) {
        Account account = currentAccount(request);
        return account != null && account.isManager();
    }

    public static void requireManager(HttpServletRequest request) {
        if (!isManager(request)) {
            throw new ForbiddenException();
        }
    }

    public static void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH, message);
    }

    public static String consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        String message = (String) session.getAttribute(FLASH);
        session.removeAttribute(FLASH);
        return message;
    }
}
