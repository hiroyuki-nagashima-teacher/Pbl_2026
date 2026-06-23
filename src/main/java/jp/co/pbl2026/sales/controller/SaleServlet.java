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

/**
 * 【模範解答解説: 売上管理コントローラー (SaleServlet)】
 * 本サーブレットは、要件定義書「4.4 売上検索要件」「4.5 売上編集要件」および「4.1 権限要件」に準拠した、
 * 売上の検索・一覧表示・追加（確認画面含む）・編集（メモのみ）・削除、およびCSVエクスポートを制御します。
 * 
 * ■ 設計・実装のポイント:
 * 1. 販売時単価のコピーと整合性保持 (要件定義 7.3):
 *    売上追加確認(/confirm)や作成(/create)の際、登録時点の「商品マスタの価格」を取得して
 *    売上データの `unit_price`（販売時単価）にコピーして保存します。これにより、
 *    将来的に商品マスタの価格が変更・改定された場合でも、過去の売上実績データの金額が変動しないように整合性を保ちます。
 * 2. 厳格な行レベルの権限チェック (要件定義 4.1):
 *    一般スタッフ（売上登録専用ロール）は、「自分が登録した売上のみ編集・削除ができる」という仕様です。
 *    これを実現するために、編集(/edit, ポストでの更新)や削除(/delete)の処理の内部で
 *    requireSaleOwnerOrManager() メソッドを呼び出し、店長ではない場合に、
 *    ログインユーザーIDと売上データの登録者ID（registered_account_id）が一致しているかを検証しています。
 *    不一致の場合は ForbiddenException を投げ、サーバー側で操作を完全に拒否します。
 * 3. 検索条件の厳密な検証 (要件定義 4.4):
 *    期間のFrom-To逆転や、金額のFrom-To逆転などの論理的な入力エラーを検出し、
 *    画面へ適切なエラーメッセージを返却するバリデーションを実装しています。
 */
@WebServlet(urlPatterns = {"/sales/search", "/sales", "/sales/csv", "/sales/new", "/sales/confirm",
        "/sales/create", "/sales/edit", "/sales/delete"})
public class SaleServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    /** アカウントマスタデータアクセス用DAO */
    private final AccountDao accountDao = new AccountDao();
    /** 商品マスタデータアクセス用DAO */
    private final ProductDao productDao = new ProductDao();
    /** 売上データアクセス用DAO */
    private final SaleDao saleDao = new SaleDao();

    /**
     * HTTP GETリクエストを処理します。
     * ルーティングを行い、売上検索フォーム表示、売上一覧表示、CSVエクスポート、
     * 新規追加・編集（メモのみ）画面表示の制御を行います。
     */
    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/sales/search".equals(path)) {
            // 売上検索画面を表示
            showSearch(req, res);
        } else if ("/sales".equals(path)) {
            // 検索実行および一覧画面表示
            search(req, res);
        } else if ("/sales/csv".equals(path)) {
            // 検索結果のCSV出力
            exportCsv(req, res);
        } else if ("/sales/new".equals(path)) {
            // 新規売上追加画面の表示（空のモデルオブジェクトを生成）
            showForm(req, res, new Sale(), errors());
        } else if ("/sales/edit".equals(path)) {
            // 売上メモ編集画面の表示（該当売上の存在確認および編集権限の検証）
            Sale sale = saleDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
            requireSaleOwnerOrManager(req, sale);
            req.setAttribute("sale", sale);
            forward(req, res, "sale/edit.jsp");
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * HTTP POSTリクエストを処理します。
     * 売上登録前の確認画面表示、確定登録、メモの保存、売上の論理削除の制御を行います。
     */
    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        String path = req.getServletPath();
        if ("/sales/confirm".equals(path)) {
            // 売上追加の入力確認
            confirm(req, res);
        } else if ("/sales/create".equals(path)) {
            // 売上追加の確定登録
            create(req, res);
        } else if ("/sales/edit".equals(path)) {
            // メモの更新保存
            updateMemo(req, res);
        } else if ("/sales/delete".equals(path)) {
            // 売上の論理削除
            delete(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 入力条件に沿って売上トランザクションデータを検索し、一覧画面を表示します。
     * ページングおよびソート情報もここで解析し、SaleDao.search() へ引き渡します。
     */
    private void search(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = errors();
        // リクエストから検索条件を取得し、パースエラーを検出
        SaleSearchCondition condition = searchConditionFromRequest(req, errors);
        // From/Toの論理的整合性（逆転チェック）などを実行
        errors.putAll(validateSearch(condition));
        
        // バリデーションエラーがある場合は検索画面に戻る
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("condition", condition);
            showSearch(req, res);
            return;
        }
        
        // ページパラメータの取得および検証
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
        
        int pageSize = 100;
        int totalCount = saleDao.count(condition);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }
        
        req.setAttribute("condition", condition);
        req.setAttribute("sales", saleDao.search(condition, page, pageSize));
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("totalCount", totalCount);
        forward(req, res, "sale/list.jsp");
    }

    /**
     * 新規売上登録の入力内容を検証し、確認画面を表示します。
     * 選択された商品が「アクティブかつ販売中」であるかを厳格に二重確認し、
     * 登録時点の商品単価を売上データ（unit_price）に一時退避します。
     */
    private void confirm(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Sale sale = saleFromRequest(req);
        Map<String, String> errors = validateSale(sale);
        
        // 選択された商品が論理削除されておらず、現在販売中であるかを検証
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

        // 重要: 販売時点での商品単価と商品名を売上データにセット（マスタ改定の影響を受けないようにコピー保存）
        sale.setProductName(product.get().getName());
        sale.setUnitPrice(product.get().getPrice());
        req.setAttribute("sale", sale);
        forward(req, res, "sale/confirm.jsp");
    }

    /**
     * 売上データをデータベースに確定登録します。
     */
    private void create(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Sale sale = saleFromRequest(req);
        Map<String, String> errors = validateSale(sale);
        if (!errors.isEmpty()) {
            showForm(req, res, sale, errors);
            return;
        }
        
        // 登録直前に、販売可能商品であることを再度検証
        Product product = productDao.findSellableById(sale.getProductId()).orElseThrow(ForbiddenException::new);
        Account current = AuthUtil.currentAccount(req);
        
        // DB登録用の販売時単価、商品名、登録者および最終更新者の設定
        sale.setUnitPrice(product.getPrice());
        sale.setProductName(product.getName());
        sale.setRegisteredAccountId(current.getId());
        sale.setLastUpdatedAccountId(current.getId());
        
        saleDao.insert(sale);

        AuthUtil.flash(req, "売上を追加しました。");
        redirect(req, res, "/sales");
    }

    /**
     * 売上データの「メモ（memo）」のみを更新します（商品や数量、日付は更新不可）。
     * 更新前に、操作ユーザーが「店長」または「登録者本人」であるかを厳格に検証（認可チェック）します。
     */
    private void updateMemo(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        Sale sale = saleDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
        // セキュリティ認可: 行レベルの権限チェックを実行
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
        // メモの更新と同時に最終更新者アカウントIDを上書き
        saleDao.updateMemo(sale.getId(), memo, AuthUtil.currentAccount(req).getId());
        AuthUtil.flash(req, "売上メモを更新しました。");
        redirect(req, res, "/sales");
    }

    /**
     * 売上データを論理削除（deleted=true）します。
     * 更新時と同様に、操作者が「店長」または「売上の登録者本人」であるかを検証します。
     */
    private void delete(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        Sale sale = saleDao.findActiveById(id(req)).orElseThrow(ForbiddenException::new);
        // セキュリティ認可: 行レベルの権限チェックを実行
        requireSaleOwnerOrManager(req, sale);
        
        // 論理削除を実行（deletedフラグを更新し、最終更新者を記録）
        saleDao.softDelete(sale.getId(), AuthUtil.currentAccount(req).getId());
        AuthUtil.flash(req, "売上を削除しました。");
        redirect(req, res, "/sales");
    }

    /**
     * HTTPリクエストパラメータを売上（Sale）モデルオブジェクトへ詰め替えます。
     */
    private Sale saleFromRequest(HttpServletRequest req) {
        Map<String, String> ignore = errors();
        Sale sale = new Sale();
        Integer productId = parseInteger(req.getParameter("productId"), "productId", ignore);
        Integer quantity = parseInteger(req.getParameter("quantity"), "quantity", ignore);
        // 日付のパース（ValidationUtil を通すことで例外を防ぐ）
        sale.setSaleDate(parseDate(req.getParameter("saleDate"), "saleDate", ignore));
        sale.setProductId(productId == null ? 0 : productId);
        sale.setQuantity(quantity == null ? 0 : quantity);
        sale.setMemo(trim(req.getParameter("memo")));
        return sale;
    }

    /**
     * リクエストの各種検索パラメータを SaleSearchCondition に詰め替えます。
     */
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

    /**
     * 売上検索画面に必要な選択肢（現在有効な全スタッフ、全商品リスト）を設定し、search.jsp を表示します。
     */
    private void showSearch(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        req.setAttribute("accounts", accountDao.findAllActive());
        req.setAttribute("products", productDao.findAllActive());
        if (req.getAttribute("condition") == null) {
            req.setAttribute("condition", new SaleSearchCondition());
        }
        forward(req, res, "sale/search.jsp");
    }

    /**
     * 新規・編集フォームからの売上入力データ検証を行います。
     * 要件定義「4.6 売上」に対応します。
     */
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

    /**
     * 売上検索条件の論理整合性（From/Toの逆転）を検証します。
     * 要件定義「4.4 売上検索要件」に対応します。
     */
    private Map<String, String> validateSearch(SaleSearchCondition condition) {
        Map<String, String> errors = errors();
        // 期間の整合性チェック
        if (condition.getDateFrom() != null && condition.getDateTo() != null
                && condition.getDateFrom().isAfter(condition.getDateTo())) {
            errors.put("dateFrom", "売上日の開始日は終了日以前の日付を入力してください。");
        }
        // 金額の整合性チェック
        if (condition.getAmountFrom() != null && condition.getAmountTo() != null
                && condition.getAmountFrom() > condition.getAmountTo()) {
            errors.put("amountFrom", "合計金額の下限は上限以下で入力してください。");
        }
        return errors;
    }

    /**
     * 登録フォーム表示に必要なデータ（現在販売中である商品の選択肢リスト等）を設定し、form.jsp へフォワードします。
     */
    private void showForm(HttpServletRequest req, HttpServletResponse res, Sale sale,
            Map<String, String> errors) throws SQLException, ServletException, IOException {
        req.setAttribute("sale", sale);
        req.setAttribute("products", productDao.findSellable());
        req.setAttribute("errors", errors);
        forward(req, res, "sale/form.jsp");
    }

    /**
     * 【行レベル認可処理】
     * ログイン中のアカウントが「店長」でない場合、指定された売上データの「登録者本人」であるかを検証します。
     * 権限を持たない不正アクセスの場合は ForbiddenException をスローして処理を拒否します。
     */
    private void requireSaleOwnerOrManager(HttpServletRequest req, Sale sale) {
        Account current = AuthUtil.currentAccount(req);
        if (!current.isManager() && sale.getRegisteredAccountId() != current.getId()) {
            throw new ForbiddenException();
        }
    }

    /**
     * 検索条件に合致する売上データを、BOM付きUTF-8のCSVファイルとしてストリーム出力します（Excelでの文字化け防止）。
     */
    private void exportCsv(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, IOException {
        Map<String, String> errors = errors();
        SaleSearchCondition condition = searchConditionFromRequest(req, errors);
        // CSV出力時には、全件検索（LIMIT制限なし）の検索処理を呼び出します
        java.util.List<Sale> sales = saleDao.search(condition);

        String filename = "sales_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        res.setContentType("text/csv; charset=UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=" + filename);

        try (java.io.OutputStream os = res.getOutputStream();
             java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8);
             java.io.PrintWriter writer = new java.io.PrintWriter(osw)) {
            
            // BOM（Byte Order Mark）の書き込みにより日本語文字化けを防ぐ
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            os.flush();
            
            writer.println("売上ID,売上日,商品ID,商品名,数量,単価,合計金額,メモ,登録スタッフ名");
            
            for (Sale sale : sales) {
                String prodName = sale.getProductName();
                String staffName = sale.getRegisteredStaffName();
                // 3桁カンマ区切りのないプレーンな数値、およびダブルクォーテーションのエスケープを処理
                writer.printf("%d,%s,%d,\"%s\",%d,%d,%d,\"%s\",\"%s\"\n",
                    sale.getId(),
                    sale.getSaleDate(),
                    sale.getProductId(),
                    (prodName != null ? prodName.replace("\"", "\"\"") : ""),
                    sale.getQuantity(),
                    sale.getUnitPrice(),
                    sale.getTotalAmount(),
                    (sale.getMemo() != null ? sale.getMemo().replace("\"", "\"\"") : ""),
                    (staffName != null ? staffName.replace("\"", "\"\"") : "")
                );
            }
            writer.flush();
        }
    }
}
