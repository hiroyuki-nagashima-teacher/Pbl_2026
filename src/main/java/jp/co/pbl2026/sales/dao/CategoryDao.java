package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jp.co.pbl2026.sales.model.Category;

public class CategoryDao {
    public List<Category> findAllActive() throws SQLException {
        String sql = "SELECT * FROM category_master WHERE deleted = false ORDER BY display_order, category_id";
        List<Category> categories = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("category_id"));
                c.setName(rs.getString("category_name"));
                c.setDisplayOrder(rs.getInt("display_order"));
                categories.add(c);
            }
        }
        return categories;
    }

    public boolean existsActive(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM category_master WHERE category_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
