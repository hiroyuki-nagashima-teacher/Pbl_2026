package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.pbl2026.sales.model.Product;

public class ProductDao {
    public List<Product> findAllActive() throws SQLException {
        return findAllActive(null, null);
    }

    public List<Product> findAllActive(String sortBy, String order) throws SQLException {
        String sortColumn = "p.product_id";
        if ("product_name".equals(sortBy)) {
            sortColumn = "p.product_name";
        } else if ("category_name".equals(sortBy)) {
            sortColumn = "c.category_name";
        } else if ("price".equals(sortBy)) {
            sortColumn = "p.price";
        } else if ("on_sale".equals(sortBy)) {
            sortColumn = "p.on_sale";
        } else if ("updated_at".equals(sortBy)) {
            sortColumn = "p.updated_at";
        }

        String sortOrder = "ASC";
        if ("desc".equalsIgnoreCase(order)) {
            sortOrder = "DESC";
        }

        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE p.deleted = false "
                + "ORDER BY " + sortColumn + " " + sortOrder + ", p.product_id ASC";
        
        List<Product> products = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(map(rs));
            }
        }
        return products;
    }

    public List<Product> findSellable() throws SQLException {
        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE p.deleted = false AND p.on_sale = true ORDER BY p.product_id";
        List<Product> products = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(map(rs));
            }
        }
        return products;
    }

    public Optional<Product> findActiveById(int id) throws SQLException {
        return findById(id, "p.deleted = false");
    }

    public Optional<Product> findSellableById(int id) throws SQLException {
        return findById(id, "p.deleted = false AND p.on_sale = true");
    }

    public void insert(Product product) throws SQLException {
        String sql = "INSERT INTO product_master (category_id, product_name, price, on_sale, last_updated_account_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            setWritableFields(ps, product);
            ps.executeUpdate();
        }
    }

    public void update(Product product) throws SQLException {
        String sql = "UPDATE product_master SET category_id = ?, product_name = ?, price = ?, on_sale = ?, "
                + "last_updated_account_id = ? WHERE product_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            setWritableFields(ps, product);
            ps.setInt(6, product.getId());
            ps.executeUpdate();
        }
    }

    public void softDelete(int id, int accountId) throws SQLException {
        String sql = "UPDATE product_master SET deleted = true, last_updated_account_id = ? WHERE product_id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Optional<Product> findById(int id, String condition) throws SQLException {
        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id WHERE p.product_id = ? AND " + condition;
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private void setWritableFields(PreparedStatement ps, Product product) throws SQLException {
        ps.setInt(1, product.getCategoryId());
        ps.setString(2, product.getName());
        ps.setInt(3, product.getPrice());
        ps.setBoolean(4, product.isOnSale());
        ps.setInt(5, product.getLastUpdatedAccountId());
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("product_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setName(rs.getString("product_name"));
        p.setPrice(rs.getInt("price"));
        p.setOnSale(rs.getBoolean("on_sale"));
        p.setLastUpdatedAccountId(rs.getInt("last_updated_account_id"));
        p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return p;
    }

    public int countAllActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_master WHERE deleted = false";
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<Product> findAllActive(String sortBy, String order, int page, int pageSize) throws SQLException {
        String sortColumn = "p.product_id";
        if ("product_name".equals(sortBy)) {
            sortColumn = "p.product_name";
        } else if ("category_name".equals(sortBy)) {
            sortColumn = "c.category_name";
        } else if ("price".equals(sortBy)) {
            sortColumn = "p.price";
        } else if ("on_sale".equals(sortBy)) {
            sortColumn = "p.on_sale";
        } else if ("updated_at".equals(sortBy)) {
            sortColumn = "p.updated_at";
        }

        String sortOrder = "ASC";
        if ("desc".equalsIgnoreCase(order)) {
            sortOrder = "DESC";
        }

        int offset = (page - 1) * pageSize;

        String sql = "SELECT p.*, c.category_name FROM product_master p "
                + "JOIN category_master c ON p.category_id = c.category_id "
                + "WHERE p.deleted = false "
                + "ORDER BY " + sortColumn + " " + sortOrder + ", p.product_id ASC "
                + "LIMIT ? OFFSET ?";
        
        List<Product> products = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(map(rs));
                }
            }
        }
        return products;
    }
}
