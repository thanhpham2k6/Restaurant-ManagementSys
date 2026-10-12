package com.pos.dao;

import com.pos.model.Category;
import com.pos.model.CategoryType;
import com.pos.model.MenuItem;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuDAO {
    private static final Logger log = LogManager.getLogger(MenuDAO.class);

    private MenuItem mapRow(ResultSet rs) throws SQLException {
        Category cat = new Category(
            rs.getInt("cat_id"),
            rs.getString("cat_name"),
            CategoryType.valueOf(rs.getString("cat_type"))
        );
        return new MenuItem(
            rs.getInt("id"),
            cat,
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getBoolean("is_available"),
            rs.getInt("version")
        );
    }

    private static final String BASE_SQL =
        "SELECT m.id, m.name, m.price, m.is_available, m.version, " +
        "c.id as cat_id, c.name as cat_name, c.type as cat_type " +
        "FROM menu_items m JOIN categories c ON m.category_id = c.id";

    public List<MenuItem> findAll() {
        List<MenuItem> list = new ArrayList<>();
        String sql = BASE_SQL + " ORDER BY c.name, m.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll menu error", e); }
        return list;
    }

    public List<MenuItem> search(String keyword) {
        List<MenuItem> list = new ArrayList<>();
        String sql = BASE_SQL + " WHERE m.name LIKE ? ORDER BY m.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { log.error("search menu error", e); }
        return list;
    }

    public List<MenuItem> findByCategory(int categoryId) {
        List<MenuItem> list = new ArrayList<>();
        String sql = BASE_SQL + " WHERE m.category_id = ? AND m.is_available = 1 ORDER BY m.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { log.error("findByCategory error", e); }
        return list;
    }

    public MenuItem findById(int id) {
        String sql = BASE_SQL + " WHERE m.id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById menu error", e); }
        return null;
    }

    public int insert(MenuItem item) {
        String sql = "INSERT INTO menu_items (category_id, name, price, is_available) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getCategory().getId());
            ps.setString(2, item.getName());
            ps.setBigDecimal(3, item.getPrice());
            ps.setBoolean(4, item.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { log.error("insert menu item error", e); }
        return -1;
    }

    public void update(MenuItem item) {
        String sql = "UPDATE menu_items SET category_id=?, name=?, price=?, is_available=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, item.getCategory().getId());
            ps.setString(2, item.getName());
            ps.setBigDecimal(3, item.getPrice());
            ps.setBoolean(4, item.isAvailable());
            ps.setInt(5, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update menu item error", e); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete menu item error", e); }
    }
}
