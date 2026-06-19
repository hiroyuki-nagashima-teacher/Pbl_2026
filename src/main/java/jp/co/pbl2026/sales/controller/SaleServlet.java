package jp.co.pbl2026.sales.controller;

import static jp.co.pbl2026.sales.util.ValidationUtil.errors;
import static jp.co.pbl2026.sales.util.ValidationUtil.parseDate;
import static jp.co.pbl2026.sales.util.ValidationUtil.parseInteger;
import static jp.co.pbl2026.sales.util.ValidationUtil.trim;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.dao.AccountDao;
import jp.co.pbl2026.sales.dao.ProductDao;
import jp.co.pbl2026.sales.dao.SaleDao;
import jp.co.pbl2026.sales.model.Account;
import jp.co.pbl2026.sales.model.Product;
import jp.co.pbl2026.sales.model.Sale;
import jp.co.pbl2026.sales.model.SaleSearchCondition;
import jp.co.pbl2026.sales.util.AuthUtil;
import jp.co.pbl2026.sales.util.ForbiddenException;

@WebServlet(urlPatterns = {"/sales/search", "/sales", "/sales/csv", "/sales/new", "/sales/confirm",
        "/sales/create", "/sales/edit", "/sales/delete"})
public class SaleServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final AccountDao accountDao = new AccountDao();
    private final ProductDao productDao = new ProductDao();
    private final SaleDao saleDao = new SaleDao();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/sales/search".equals(path)) {
            showSearch(req, res);
        } else if ("/sales".equals(path)) {
            search(req, res);
        } else if ("/sales/csv".equals(path)) {
            exportCsv(req, res);
        } else if ("/sales/new".equals(path)) {
            showForm(req, res, new Sale(), errors());
        } else if ("/sales/edit".equals(path)) {
            Sale sale = saleDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            requireSaleOwnerOrManager(req, sale);
            req.setAttribute("sale", sale);
            forward(req, res, "sale/edit.jsp");
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/sales/confirm".equals(path)) {
            confirm(req, res);
        } else if ("/sales/create".equals(path)) {
            create(req, res);
        } else if ("/sales/edit".equals(path)) {
            updateMemo(req, res);
        } else if ("/sales/delete".equals(path)) {
            delete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void search(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = errors();
        SaleSearchCondition condition = searchConditionFromRequest(req, errors);
        errors.putAll(validateSearch(condition));
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("condition", condition);
            showSearch(req, res);
            return;
        }
        req.setAttribute("condition", condition);
        req.setAttribute("sales", saleDao.search(condition));
        forward(req, res, "sale/list.jsp");
    }

    private void confirm(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Sale sale = saleFromRequest(req);
        Map<String, String> errors = validateSale(sale);
        Optional<Product> product = sale.getProductId() == 0
                ? Optional.empty()
                : productDao.findSellableById(sale.getProductId());
        if (product.isEmpty()) {
            errors.put("productId", "販売中の商品を選択してください。");
        }
        if (!errors.isEmpty()) {
            showForm(req, res, sale, errors);
            return;
        }

        // 販売時単価は登録時点の商品価格をコピーし、商品価格変更後も過去売上を保護する。
        sale.setProductName(product.get().getName());
        sale.setUnitPrice(product.get().getPrice());
        req.setAttribute("sale", sale);
        forward(req, res, "sale/confirm.jsp");
    }

    private void create(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Sale sale = saleFromRequest(req);
        Map<String, String> errors = validateSale(sale);
        if (!errors.isEmpty()) {
            showForm(req, res, sale, errors);
            return;
        }
        Product product = productDao.findSellableById(sale.getProductId()).orElseThrow(ForbiddenException::new);
        Account current = AuthUtil.currentAccount(req);
        sale.setUnitPrice(product.getPrice());
        sale.setProductName(product.getName());
        sale.setRegisteredAccountId(current.getId());
        sale.setLastUpdatedAccountId(current.getId());
        saleDao.insert(sale);
        
        // 模擬メール通知
        try {
            jp.co.pbl2026.sales.util.MailUtil.sendSaleNotification(sale, current.getStaffName());
        } catch (Exception e) {
            System.err.println("模擬メール送信に失敗しました: " + e.getMessage());
        }

        AuthUtil.flash(req, "売上を追加しました。");
        redirect(req, res, "/sales");
    }

    private void updateMemo(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Sale sale = saleDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
        requireSaleOwnerOrManager(req, sale);
        String memo = trim(req.getParameter("memo"));
        Map<String, String> errors = errors();
        if (memo.length() > 500) {
            errors.put("memo", "メモは500文字以内で入力してください。");
        }
        if (!errors.isEmpty()) {
            sale.setMemo(memo);
            req.setAttribute("sale", sale);
            req.setAttribute("errors", errors);
            forward(req, res, "sale/edit.jsp");
            return;
        }
        saleDao.updateMemo(sale.getId(), memo, AuthUtil.currentAccount(req).getId());
        AuthUtil.flash(req, "売上メモを更新しました。");
        redirect(req, res, "/sales");
    }

    private void delete(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        Sale sale = saleDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
        requireSaleOwnerOrManager(req, sale);
        saleDao.softDelete(sale.getId(), AuthUtil.currentAccount(req).getId());
        AuthUtil.flash(req, "売上を削除しました。");
        redirect(req, res, "/sales");
    }

    private Sale saleFromRequest(HttpServletRequest req) {
        Map<String, String> ignore = errors();
        Sale sale = new Sale();
        Integer productId = parseInteger(req.getParameter("productId"), "productId", ignore);
        Integer quantity = parseInteger(req.getParameter("quantity"), "quantity", ignore);
        sale.setSaleDate(parseDate(req.getParameter("saleDate"), "saleDate", ignore));
        sale.setProductId(productId == null ? 0 : productId);
        sale.setQuantity(quantity == null ? 0 : quantity);
        sale.setMemo(trim(req.getParameter("memo")));
        return sale;
    }

    private SaleSearchCondition searchConditionFromRequest(HttpServletRequest req, Map<String, String> errors) {
        SaleSearchCondition condition = new SaleSearchCondition();
        condition.setDateFrom(parseDate(req.getParameter("dateFrom"), "dateFrom", errors));
        condition.setDateTo(parseDate(req.getParameter("dateTo"), "dateTo", errors));
        condition.setStaffName(trim(req.getParameter("staffName")));
        condition.setAmountFrom(parseInteger(req.getParameter("amountFrom"), "amountFrom", errors));
        condition.setAmountTo(parseInteger(req.getParameter("amountTo"), "amountTo", errors));
        
        Map<String, String> dummyErrors = errors();
        condition.setProductId(parseInteger(req.getParameter("productId"), "productId", dummyErrors));
        condition.setSortBy(trim(req.getParameter("sortBy")));
        condition.setOrder(trim(req.getParameter("order")));
        return condition;
    }

    private void showSearch(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        // スタッフ名は入力ミスを避けられるよう、現在有効なアカウントから選択肢を作る。
        req.setAttribute("accounts", accountDao.findAllActive());
        req.setAttribute("products", productDao.findAllActive());
        if (req.getAttribute("condition") == null) {
            req.setAttribute("condition", new SaleSearchCondition());
        }
        forward(req, res, "sale/search.jsp");
    }

    private Map<String, String> validateSale(Sale sale) {
        Map<String, String> errors = errors();
        if (sale.getSaleDate() == null) {
            errors.put("saleDate", "売上日を入力してください。");
        }
        if (sale.getQuantity() < 1) {
            errors.put("quantity", "数量は1以上の整数で入力してください。");
        }
        if (sale.getMemo() != null && sale.getMemo().length() > 500) {
            errors.put("memo", "メモは500文字以内で入力してください。");
        }
        return errors;
    }

    private Map<String, String> validateSearch(SaleSearchCondition condition) {
        Map<String, String> errors = errors();
        if (condition.getDateFrom() != null && condition.getDateTo() != null
                && condition.getDateFrom().isAfter(condition.getDateTo())) {
            errors.put("dateFrom", "売上日の開始日は終了日以前の日付を入力してください。");
        }
        if (condition.getAmountFrom() != null && condition.getAmountTo() != null
                && condition.getAmountFrom() > condition.getAmountTo()) {
            errors.put("amountFrom", "合計金額の下限は上限以下で入力してください。");
        }
        return errors;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse res, Sale sale,
            Map<String, String> errors) throws SQLException, ServletException, IOException {
        req.setAttribute("sale", sale);
        req.setAttribute("products", productDao.findSellable());
        req.setAttribute("errors", errors);
        forward(req, res, "sale/form.jsp");
    }

    private void requireSaleOwnerOrManager(HttpServletRequest req, Sale sale) {
        Account current = AuthUtil.currentAccount(req);
        if (!current.isManager() && sale.getRegisteredAccountId() != current.getId()) {
            throw new ForbiddenException();
        }
    }

    private void exportCsv(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        Map<String, String> errors = errors();
        SaleSearchCondition condition = searchConditionFromRequest(req, errors);
        java.util.List<Sale> sales = saleDao.search(condition);

        res.setContentType("text/csv; charset=UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=\"sales_" + System.currentTimeMillis() + ".csv\"");

        try (java.io.OutputStream os = res.getOutputStream()) {
            // Excelで文字化けしないようにUTF-8のBOMを出力
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8));
            writer.println("売上ID,売上日,商品ID,商品名,数量,単価,合計金額,メモ,登録スタッフ名");
            
            for (Sale sale : sales) {
                writer.printf("%d,%s,%d,\"%s\",%d,%d,%d,\"%s\",\"%s\"\n",
                    sale.getId(),
                    sale.getSaleDate(),
                    sale.getProductId(),
                    sale.getProductName().replace("\"", "\"\""),
                    sale.getQuantity(),
                    sale.getUnitPrice(),
                    sale.getTotalAmount(),
                    (sale.getMemo() != null ? sale.getMemo().replace("\"", "\"\"") : ""),
                    sale.getRegisteredStaffName().replace("\"", "\"\"")
                );
            }
            writer.flush();
        }
    }
}
