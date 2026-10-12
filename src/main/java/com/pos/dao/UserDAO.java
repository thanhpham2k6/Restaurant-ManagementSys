package com.pos.dao;

import com.pos.model.Role;
import com.pos.model.User;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Logger log = LogManager.getLogger(UserDAO.class);

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            Role.valueOf(rs.getString("role")),
            rs.getBoolean("is_active")
        );
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findByUsername error", e); }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY full_name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll users error", e); }
        return list;
    }

    public int insert(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, is_active) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { log.error("insert user error", e); }
        return -1;
    }

    public void update(User user) {
        String sql = "UPDATE users SET username=?, password_hash=?, full_name=?, role=?, is_active=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.setInt(6, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update user error", e); }
    }

    public void deactivate(int id) {
        String sql = "UPDATE users SET is_active=0 WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("deactivate user error", e); }
    }

    public void activate(int id) {
        String sql = "UPDATE users SET is_active=1 WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("activate user error", e); }
    }

    public boolean existsUsername(String username) {
        if (username == null || username.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { log.error("existsUsername error", e); }
        return false;
    }

    public void updatePassword(int id, String passwordHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updatePassword error", e); }
    }

    /**
     * Xóa tài khoản. Nếu tài khoản đã có dữ liệu liên kết trong đơn hàng hoặc kho,
     * tự động chuyển sang vô hiệu hóa (is_active = 0) để bảo toàn lịch sử sổ sách.
     * @return 1 nếu đã xóa vĩnh viễn, 2 nếu đã vô hiệu hóa do có dữ liệu liên kết, 0 nếu không tìm thấy
     */
    public int delete(int id) {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            return rows > 0 ? 1 : 0;
        } catch (SQLException e) {
            log.info("User #{} có dữ liệu liên kết, chuyển sang vô hiệu hóa: {}", id, e.getMessage());
            deactivate(id);
            return 2;
        }
    }
}
