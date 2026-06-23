package jp.co.pbl2026.sales.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.dao.SaleDao;

/**
 * 【模範解答解説: ダッシュボード表示コントローラー (DashboardServlet)】
 * 本サーブレットは、要件定義書「4.3 ダッシュボード要件」をベースとして、
 * 店長やスタッフが当日の販売動向を瞬時に把握できるよう、
 * 各種売上データの集計処理を行いダッシュボード画面へと連携させます。
 * 
 * ■ 設計・実装のポイント:
 * 1. ビジネス集計ロジックのDAO委譲:
 *    SQLでのデータ加工（SUMやGROUP BY、LIMIT処理）をDAO側に寄せることで、
 *    コントローラー内ではデータの詰め替えと画面遷移のみを行うように役割を整理しています。
 */
@WebServlet("/dashboard")
public class DashboardServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    /** 売上データアクセス用DAO */
    private final SaleDao saleDao = new SaleDao();

    /**
     * HTTP GETリクエストを処理します。
     * 当日/当月売上合計、直近7日間の売上推移、カテゴリ別売上、売れ筋ランキング（上位5件）
     * などの集計データをDAOから取得し、ダッシュボード画面（dashboard.jsp）にバインドします。
     */
    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        // DAOを経由した統計情報の集計実行
        int todaySalesAmount = saleDao.getTodaySalesAmount();
        int monthSalesAmount = saleDao.getMonthSalesAmount();
        java.util.List<jp.co.pbl2026.sales.model.Sale> recentSales = saleDao.getRecent7DaysSales();
        java.util.List<jp.co.pbl2026.sales.model.CategorySales> categorySales = saleDao.getCategorySales();
        java.util.List<jp.co.pbl2026.sales.model.ProductSales> topProducts = saleDao.getTopProductSales(5);

        // 日付範囲パラメータ初期値の算出
        java.time.LocalDate today = java.time.LocalDate.now();
        String todayDate = today.toString();
        String monthStartDate = today.withDayOfMonth(1).toString();

        // 画面（JSP）へ引き渡す属性値の設定
        req.setAttribute("todaySalesAmount", todaySalesAmount);
        req.setAttribute("monthSalesAmount", monthSalesAmount);
        req.setAttribute("recentSales", recentSales);
        req.setAttribute("categorySales", categorySales);
        req.setAttribute("topProducts", topProducts);
        req.setAttribute("todayDate", todayDate);
        req.setAttribute("monthStartDate", monthStartDate);

        forward(req, res, "dashboard.jsp");
    }
}
