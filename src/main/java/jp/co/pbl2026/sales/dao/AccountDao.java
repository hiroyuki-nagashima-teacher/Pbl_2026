package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.pbl2026.sales.model.Account;

public class AccountDao {
    public Optional<Account> findActiveByLoginId(String loginId) throws SQLException {
        String sql = "SELECT * FROM account WHERE login_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Account> findActiveById(int id) throws SQLException {
        String sql = "SELECT * FROM account WHERE account_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Account> findAllActive() throws SQLException {
        String sql = "SELECT * FROM account WHERE deleted = false ORDER BY account_id";
        List<Account> accounts = new ArrayList<>();
        try (Connection con = Db.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(map(rs));
            }
        }
        return accounts;
    }

    public boolean existsLoginId(String loginId, Integer exceptId) throws SQLException {
        return exists("login_id", loginId, exceptId);
    }

    public boolean existsStaffName(String staffName, Integer exceptId) throws SQLException {
        return exists("staff_name", staffName, exceptId);
    }

    public void insert(Account account) throws SQLException {
        String sql = "INSERT INTO account (login_id, staff_name, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, account.getLoginId());
            ps.setString(2, account.getStaffName());
            ps.setString(3, account.getPasswordHash());
            ps.setString(4, account.getRole());
            ps.executeUpdate();
        }
    }

    public void update(Account account, boolean updatePassword) throws SQLException {
        String sql = updatePassword
                ? "UPDATE account SET login_id = ?, staff_name = ?, password_hash = ?, role = ? WHERE account_id = ? AND deleted = false"
                : "UPDATE account SET login_id = ?, staff_name = ?, role = ? WHERE account_id = ? AND deleted = false";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, account.getLoginId());
            ps.setString(2, account.getStaffName());
            if (updatePassword) {
                ps.setString(3, account.getPasswordHash());
                ps.setString(4, account.getRole());
                ps.setInt(5, account.getId());
            } else {
                ps.setString(3, account.getRole());
                ps.setInt(4, account.getId());
            }
            ps.executeUpdate();
        }
    }

    public void softDelete(int id) throws SQLException {
        String sql = "UPDATE account SET deleted = true WHERE account_id = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int activeManagerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM account WHERE deleted = false AND role = ?";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Account.ROLE_MANAGER);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private boolean exists(String column, String value, Integer exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM account WHERE " + column + " = ? AND deleted = false"
                + (exceptId == null ? "" : " AND account_id <> ?");
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            if (exceptId != null) {
                ps.setInt(2, exceptId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private Account map(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("account_id"));
        a.setLoginId(rs.getString("login_id"));
        a.setStaffName(rs.getString("staff_name"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setRole(rs.getString("role"));
        a.setDeleted(rs.getBoolean("deleted"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return a;
    }
}
