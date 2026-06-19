package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.pbl2026.sales.model.Sale;
import jp.co.pbl2026.sales.model.SaleSearchCondition;

public class SaleDao {
    public List<Sale> search(SaleSearchCondition condition) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, p.product_name, a.staff_name FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN account a ON s.registered_account_id = a.account_id "
                + "WHERE s.deleted = false");
        List<Object> params = new ArrayList<>();

        if (condition.getDateFrom() != null) {
            sql.append(" AND s.sale_date >= ?");
            params.add(Date.valueOf(condition.getDateFrom()));
        }
        if (condition.getDateTo() != null) {
            sql.append(" AND s.sale_date <= ?");
            params.add(Date.valueOf(condition.getDateTo()));
        }
        if (condition.getStaffName() != null && !condition.getStaffName().isBlank()) {
            sql.append(" AND a.staff_name LIKE ?");
            params.add("%" + condition.getStaffName() + "%");
        }
        if (condition.getAmountFrom() != null) {
            sql.append(" AND (s.unit_price * s.quantity) >= ?");
            params.add(condition.getAmountFrom());
        }
        if (condition.getAmountTo() != null) {
            sql.append(" AND (s.unit_price * s.quantity) <= ?");
            params.add(condition.getAmountTo());
        }
        if (condition.getProductId() != null && condition.getProductId() > 0) {
            sql.append(" AND s.product_id = ?");
            params.add(condition.getProductId());
        }

        String sortColumn = "s.sale_date";
        if ("product_name".equals(condition.getSortBy())) {
            sortColumn = "p.product_name";
        } else if ("quantity".equals(condition.getSortBy())) {
            sortColumn = "s.quantity";
        } else if ("amount".equals(condition.getSortBy())) {
            sortColumn = "(s.unit_price * s.quantity)";
        } else if ("staff_name".equals(condition.getSortBy())) {
            sortColumn = "a.staff_name";
        }

        String sortOrder = "DESC";
        if ("asc".equalsIgnoreCase(condition.getOrder())) {
            sortOrder = "ASC";
        }
        sql.append(" ORDER BY ").append(sortColumn).append(" ").append(sortOrder).append(", s.sales_id DESC");

        List<Sale> sales = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sales.add(map(rs));
                }
            }
        }
        return sales;
    }

    public Optional<Sale> findActiveById(int id) throws SQLException {
        String sql = "SELECT s.*, p.product_name, a.staff_name FROM sales_transaction s "
                + "JOIN product_master p ON s.product_id = p.product_id "
                + "JOIN account a ON s.registered_account_id = a.account_id "
                + "WHERE s.sales_id = ? AND s.deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void insert(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales_transaction "
                + "(sale_date, product_id, quantity, unit_price, memo, registered_account_id, last_updated_account_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(sale.getSaleDate()));
            ps.setInt(2, sale.getProductId());
            ps.setInt(3, sale.getQuantity());
            ps.setInt(4, sale.getUnitPrice());
            ps.setString(5, sale.getMemo());
            ps.setInt(6, sale.getRegisteredAccountId());
            ps.setInt(7, sale.getLastUpdatedAccountId());
            ps.executeUpdate();
        }
    }

    public void updateMemo(int id, String memo, int accountId) throws SQLException {
        String sql = "UPDATE sales_transaction SET memo = ?, last_updated_account_id = ? "
                + "WHERE sales_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, memo);
            ps.setInt(2, accountId);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void softDelete(int id, int accountId) throws SQLException {
        String sql = "UPDATE sales_transaction SET deleted = true, last_updated_account_id = ? WHERE sales_id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public int getTodaySalesAmount() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM sales_transaction "
                + "WHERE sale_date = CURDATE() AND deleted = false";
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int getMonthSalesAmount() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM sales_transaction "
                + "WHERE sale_date >= DATE_FORMAT(CURDATE(), '%Y-%m-01') AND deleted = false";
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<Sale> getRecent7DaysSales() throws SQLException {
        java.time.LocalDate today = java.time.LocalDate.now();
        List<Sale> recentSales = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            Sale s = new Sale();
            s.setSaleDate(d);
            s.setUnitPrice(0);
            s.setQuantity(1);
            recentSales.add(s);
        }

        String sql = "SELECT sale_date, SUM(quantity * unit_price) AS daily_amount "
                + "FROM sales_transaction "
                + "WHERE sale_date >= ? AND sale_date <= ? AND deleted = false "
                + "GROUP BY sale_date";

        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today.minusDays(6)));
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate sd = rs.getDate("sale_date").toLocalDate();
                    int amount = rs.getInt("daily_amount");
                    for (Sale s : recentSales) {
                        if (s.getSaleDate().equals(sd)) {
                            s.setUnitPrice(amount);
                            break;
                        }
                    }
                }
            }
        }
        return recentSales;
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private Sale map(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("sales_id"));
        s.setSaleDate(rs.getDate("sale_date").toLocalDate());
        s.setProductId(rs.getInt("product_id"));
        s.setProductName(rs.getString("product_name"));
        s.setQuantity(rs.getInt("quantity"));
        s.setUnitPrice(rs.getInt("unit_price"));
        s.setMemo(rs.getString("memo"));
        s.setRegisteredAccountId(rs.getInt("registered_account_id"));
        s.setRegisteredStaffName(rs.getString("staff_name"));
        s.setLastUpdatedAccountId(rs.getInt("last_updated_account_id"));
        s.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return s;
    }
}
