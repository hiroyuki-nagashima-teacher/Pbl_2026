package jp.co.pbl2026.sales.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.co.pbl2026.sales.dao.SaleDao;

@WebServlet("/dashboard")
public class DashboardServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final SaleDao saleDao = new SaleDao();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse res)
            throws SQLException, ServletException, IOException {
        int todaySalesAmount = saleDao.getTodaySalesAmount();
        int monthSalesAmount = saleDao.getMonthSalesAmount();
        java.util.List<jp.co.pbl2026.sales.model.Sale> recentSales = saleDao.getRecent7DaysSales();
        java.util.List<jp.co.pbl2026.sales.model.CategorySales> categorySales = saleDao.getCategorySales();
        java.util.List<jp.co.pbl2026.sales.model.ProductSales> topProducts = saleDao.getTopProductSales(5);

        java.time.LocalDate today = java.time.LocalDate.now();
        String todayDate = today.toString();
        String monthStartDate = today.withDayOfMonth(1).toString();

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
