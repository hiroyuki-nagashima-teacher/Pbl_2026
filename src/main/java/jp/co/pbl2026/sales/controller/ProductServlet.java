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

/**
 * 【模範解答解説: 商品管理コントローラー (ProductServlet)】
 * 本サーブレットは、要件定義書「4.1 権限要件」および「4.6 入力チェック要件」に準拠した、
 * 商品マスタの一覧表示・登録（確認画面含む）・編集・削除、およびCSVエクスポートを制御します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 権限制御による操作範囲の制限:
 *    一般スタッフ（売上登録専用ロール）による商品の新規追加、編集、削除を防ぐため、
 *    `/new`, `/edit`, `/delete` などの各処理に入る手前で AuthUtil.requireManager(req) を実行し、
 *    権限のないアクセスをサーバー側で厳格に拒否しています。
 * 2. 商品の論理削除:
 *    過去の売上実績データの整合性（過去の売上に紐づく商品は、商品一覧から消えても売上履歴上では参照できる）を
 *    保つために、物理削除は行わず、softDelete() による論理削除を採用しています。
 * 3. ページネーションとソートのサポート:
 *    商品データ件数が多くなった場合でも、一覧表示が3秒以内に表示される（5.1性能要件）ようにするため、
 *    findAllActive() メソッドにてページネーションと動的ソートをサポートしています。
 */
@WebServlet(urlPatterns = {"/products", "/products/csv", "/products/new", "/products/confirm",
        "/products/create", "/products/edit", "/products/delete"})
public class ProductServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    /** カテゴリーマスタのデータ操作用DAO */
    private final CategoryDao categoryDao = new CategoryDao();
    /** 商品マスタのデータ操作用DAO */
    private final ProductDao productDao = new ProductDao();

    /**
     * HTTP GETリクエストを処理します。
     * 商品一覧の取得（ページング、ソート条件の解析）や、新規・編集フォームの画面遷移を制御します。
     */
    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/products".equals(path)) {
            // ソートキーと順序の取得
            String sortBy = req.getParameter("sortBy");
            String order = req.getParameter("order");
            
            // ページングパラメータの解析（デフォルトは1ページ目）
            int page = 1;
            String pageParam = req.getParameter("page");
            if (pageParam != null && !pageParam.isBlank()) {
                try {
                    page = Integer.parseInt(pageParam);
                    if (page < 1) {
                        page = 1;
                    }
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }
            
            // 1ページあたりの表示件数設定
            int pageSize = 100;
            // 総件数と総ページ数の算出
            int totalCount = productDao.countAllActive();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            if (totalPages < 1) {
                totalPages = 1;
            }
            if (page > totalPages) {
                page = totalPages;
            }
            
            // 画面表示用のデータを設定
            req.setAttribute("products", productDao.findAllActive(sortBy, order, page, pageSize));
            req.setAttribute("currentPage", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("totalCount", totalCount);
            req.setAttribute("sortBy", sortBy);
            req.setAttribute("order", order);
            forward(req, res, "product/list.jsp");
        } else if ("/products/csv".equals(path)) {
            // 商品一覧のCSV出力処理
            exportCsv(req, res);
        } else if ("/products/new".equals(path)) {
            // 新規追加画面の表示（店長権限必須）
            AuthUtil.requireManager(req);
            showForm(req, res, new Product(), errors(), false);
        } else if ("/products/edit".equals(path)) {
            // 編集画面の表示（店長権限必須）
            AuthUtil.requireManager(req);
            Product product = productDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            showForm(req, res, product, errors(), true);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * HTTP POSTリクエストを処理します。
     * 新規登録前の確認画面表示、確定登録、既存情報の更新、および論理削除の制御を行います。
     */
    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/products/confirm".equals(path)) {
            // 登録確認画面の表示
            confirm(req, res);
        } else if ("/products/create".equals(path)) {
            // 確定登録処理
            create(req, res);
        } else if ("/products/edit".equals(path)) {
            // 更新処理
            update(req, res);
        } else if ("/products/delete".equals(path)) {
            // 削除処理
            delete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 入力内容をバリデーションし、問題がなければ「商品追加確認画面」を表示します。
     * 店長権限が必要です。
     */
    private void confirm(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        // リクエスト値から商品データを生成
        Product product = productFromRequest(req);
        // バリデーション実行
        Map<String, String> errors = validateProduct(product);
        if (!errors.isEmpty()) {
            // エラー時はフォームに戻る
            showForm(req, res, product, errors, false);
            return;
        }
        req.setAttribute("product", product);
        // 画面確認用にカテゴリーIDからカテゴリー名を引き出す
        req.setAttribute("categoryName", categoryName(product.getCategoryId()));
        forward(req, res, "product/confirm.jsp");
    }

    /**
     * 入力内容を確定登録します。
     * 店長権限が必要です。最終更新者として操作者のIDを記録します。
     */
    private void create(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        Product product = productFromRequest(req);
        Map<String, String> errors = validateProduct(product);
        if (!errors.isEmpty()) {
            showForm(req, res, product, errors, false);
            return;
        }
        // 操作ユーザーのアカウントIDを最終更新者として記録
        product.setLastUpdatedAccountId(AuthUtil.currentAccount(req).getId());
        productDao.insert(product);
        AuthUtil.flash(req, "商品を追加しました。");
        redirect(req, res, "/products");
    }

    /**
     * 既存の商品情報を更新保存します。
     * 店長権限が必要です。
     */
    private void update(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        AuthUtil.requireManager(req);
        // 更新前の商品が存在するか確認
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

    /**
     * 商品を論理削除（deleted=true）します。
     * 店長権限が必要です。
     */
    private void delete(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        AuthUtil.requireManager(req);
        // 商品を論理削除し、誰が削除したかを last_updated_account_id に記録
        productDao.softDelete(id(req), AuthUtil.currentAccount(req).getId());
        AuthUtil.flash(req, "商品を削除しました。");
        redirect(req, res, "/products");
    }

    /**
     * HTTPリクエストから商品モデルオブジェクト（Product）への詰め替えを行います。
     * 入力値の数値変換エラー（パースエラー）は ValidationUtil で処理します。
     */
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

    /**
     * 商品の入力値バリデーションを実行します。
     * 要件定義「4.6 商品」の必須・文字数・範囲チェックに対応します。
     */
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

    /**
     * 商品フォーム表示に必要な属性（カテゴリーマスタの全リスト等）を設定し、form.jsp へフォワードします。
     */
    private void showForm(HttpServletRequest req, HttpServletResponse res, Product product,
            Map<String, String> errors, boolean edit) throws SQLException, ServletException, IOException {
        req.setAttribute("product", product);
        req.setAttribute("categories", categoryDao.findAllActive());
        req.setAttribute("errors", errors);
        req.setAttribute("edit", edit);
        forward(req, res, "product/form.jsp");
    }

    /**
     * カテゴリーIDからカテゴリー名を取得するヘルパーメソッド。
     */
    private String categoryName(int categoryId) throws SQLException {
        return categoryDao.findAllActive().stream()
                .filter(c -> c.getId() == categoryId)
                .map(c -> c.getName())
                .findFirst()
                .orElse("");
    }

    /**
     * 商品マスタデータをBOM付きUTF-8のCSVファイルとしてストリーム出力します（Excelでの日本語文字化けを防止）。
     */
    private void exportCsv(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        java.util.List<Product> products = productDao.findAllActive();

        String filename = "products_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        res.setContentType("text/csv; charset=UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=" + filename);

        try (java.io.OutputStream os = res.getOutputStream();
             java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8);
             java.io.PrintWriter writer = new java.io.PrintWriter(osw)) {
            
            // BOM（Byte Order Mark）の書き込みにより、Excel等の主要ソフトでの文字化けを防ぐ
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            os.flush();
            
            writer.println("商品ID,カテゴリーID,カテゴリー名,商品名,価格,販売状況");
            
            for (Product product : products) {
                String catName = categoryName(product.getCategoryId());
                String prodName = product.getName();
                // CSVフォーマット規則（カンマ、ダブルクォーテーションのエスケープ）を適用して出力
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
