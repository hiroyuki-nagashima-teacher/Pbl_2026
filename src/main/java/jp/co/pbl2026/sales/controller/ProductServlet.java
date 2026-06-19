package jp.co.pbl2026.sales.controller;

import static jp.co.pbl2026.sales.util.ValidationUtil.errors;
import static jp.co.pbl2026.sales.util.ValidationUtil.parseInteger;
import static jp.co.pbl2026.sales.util.ValidationUtil.trim;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.dao.CategoryDao;
import jp.co.pbl2026.sales.dao.ProductDao;
import jp.co.pbl2026.sales.model.Product;
import jp.co.pbl2026.sales.util.AuthUtil;
import jp.co.pbl2026.sales.util.ForbiddenException;

@WebServlet(urlPatterns = {"/products", "/products/csv", "/products/new", "/products/confirm",
        "/products/create", "/products/edit", "/products/delete"})
public class ProductServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final CategoryDao categoryDao = new CategoryDao();
    private final ProductDao productDao = new ProductDao();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/products".equals(path)) {
            req.setAttribute("products", productDao.findAllActive());
            forward(req, res, "product/list.jsp");
        } else if ("/products/csv".equals(path)) {
            exportCsv(req, res);
        } else if ("/products/new".equals(path)) {
            AuthUtil.requireManager(req);
            showForm(req, res, new Product(), errors(), false);
        } else if ("/products/edit".equals(path)) {
            AuthUtil.requireManager(req);
            Product product = productDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            showForm(req, res, product, errors(), true);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/products/confirm".equals(path)) {
            confirm(req, res);
        } else if ("/products/create".equals(path)) {
            create(req, res);
        } else if ("/products/edit".equals(path)) {
            update(req, res);
        } else if ("/products/delete".equals(path)) {
            delete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void confirm(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        Product product = productFromRequest(req);
        Map<String, String> errors = validateProduct(product);
        if (!errors.isEmpty()) {
            showForm(req, res, product, errors, false);
            return;
        }
        req.setAttribute("product", product);
        req.setAttribute("categoryName", categoryName(product.getCategoryId()));
        forward(req, res, "product/confirm.jsp");
    }

    private void create(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        Product product = productFromRequest(req);
        Map<String, String> errors = validateProduct(product);
        if (!errors.isEmpty()) {
            showForm(req, res, product, errors, false);
            return;
        }
        product.setLastUpdatedAccountId(AuthUtil.currentAccount(req).getId());
        productDao.insert(product);
        AuthUtil.flash(req, "商品を追加しました。");
        redirect(req, res, "/products");
    }

    private void update(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        Product before = productDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
        Product input = productFromRequest(req);
        input.setId(before.getId());
        input.setLastUpdatedAccountId(AuthUtil.currentAccount(req).getId());
        Map<String, String> errors = validateProduct(input);
        if (!errors.isEmpty()) {
            showForm(req, res, input, errors, true);
            return;
        }
        productDao.update(input);
        AuthUtil.flash(req, "商品を更新しました。");
        redirect(req, res, "/products");
    }

    private void delete(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        AuthUtil.requireManager(req);
        productDao.softDelete(id(req), AuthUtil.currentAccount(req).getId());
        AuthUtil.flash(req, "商品を削除しました。");
        redirect(req, res, "/products");
    }

    private Product productFromRequest(HttpServletRequest req) {
        Product product = new Product();
        Integer categoryId = parseInteger(req.getParameter("categoryId"), "categoryId", errors());
        Integer price = parseInteger(req.getParameter("price"), "price", errors());
        product.setCategoryId(categoryId == null ? 0 : categoryId);
        product.setName(trim(req.getParameter("name")));
        product.setPrice(price == null ? -1 : price);
        product.setOnSale("true".equals(req.getParameter("onSale")));
        return product;
    }

    private Map<String, String> validateProduct(Product product) throws SQLException {
        Map<String, String> errors = errors();
        if (product.getName().isEmpty() || product.getName().length() > 100) {
            errors.put("name", "商品名は1〜100文字で入力してください。");
        }
        if (product.getCategoryId() <= 0 || !categoryDao.existsActive(product.getCategoryId())) {
            errors.put("categoryId", "カテゴリーを選択してください。");
        }
        if (product.getPrice() < 0) {
            errors.put("price", "価格は0以上の整数で入力してください。");
        }
        return errors;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse res, Product product,
            Map<String, String> errors, boolean edit) throws SQLException, ServletException, IOException {
        req.setAttribute("product", product);
        req.setAttribute("categories", categoryDao.findAllActive());
        req.setAttribute("errors", errors);
        req.setAttribute("edit", edit);
        forward(req, res, "product/form.jsp");
    }

    private String categoryName(int categoryId) throws SQLException {
        return categoryDao.findAllActive().stream()
                .filter(c -> c.getId() == categoryId)
                .map(c -> c.getName())
                .findFirst()
                .orElse("");
    }

    private void exportCsv(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        java.util.List<Product> products = productDao.findAllActive();

        String filename = "products_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        res.setContentType("text/csv; charset=UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=" + filename);

        try (java.io.OutputStream os = res.getOutputStream();
             java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8);
             java.io.PrintWriter writer = new java.io.PrintWriter(osw)) {
            
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            os.flush();
            
            writer.println("商品ID,カテゴリーID,カテゴリー名,商品名,価格,販売状況");
            
            for (Product product : products) {
                String catName = categoryName(product.getCategoryId());
                String prodName = product.getName();
                writer.printf("%d,%d,\"%s\",\"%s\",%d,\"%s\"\n",
                    product.getId(),
                    product.getCategoryId(),
                    (catName != null ? catName.replace("\"", "\"\"") : ""),
                    (prodName != null ? prodName.replace("\"", "\"\"") : ""),
                    product.getPrice(),
                    product.isOnSale() ? "販売中" : "販売停止"
                );
            }
            writer.flush();
        }
    }
}
