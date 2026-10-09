package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.MenuItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDao {

    private MenuItem mapRow(ResultSet rs) throws SQLException {
        return new MenuItem(
            rs.getInt("item_id"),
            rs.getInt("category_id"),
            rs.getString("item_name"),
            rs.getDouble("price"),
            rs.getBoolean("is_available"),
            rs.getInt("available_quantity")
        );
    }

    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM MenuItems ORDER BY item_name";
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) items.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    public List<MenuItem> getMenuItemsByCategory(int categoryId) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM MenuItems WHERE category_id=? ORDER BY item_name";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, categoryId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    public void addMenuItem(MenuItem item) {
        String sql = "INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (?,?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, item.getCategoryId());
            p.setString(2, item.getItemName());
            p.setDouble(3, item.getPrice());
            p.setBoolean(4, item.isAvailable());
            p.setInt(5, item.getAvailableQuantity());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateMenuItem(MenuItem item) {
        String sql = "UPDATE MenuItems SET category_id=?, item_name=?, price=?, is_available=?, available_quantity=? WHERE item_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, item.getCategoryId());
            p.setString(2, item.getItemName());
            p.setDouble(3, item.getPrice());
            p.setBoolean(4, item.isAvailable());
            p.setInt(5, item.getAvailableQuantity());
            p.setInt(6, item.getItemId());
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteMenuItem(int itemId) {
        String sql = "DELETE FROM MenuItems WHERE item_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, itemId);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * Nhà bếp đặt số suất có sẵn hôm nay (ghi đè).
     * Phục vụ gọi món sẽ trừ dần số này.
     */
    public void setAvailableQuantity(int itemId, int quantity) {
        String sql = "UPDATE MenuItems SET available_quantity=? WHERE item_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, quantity);
            p.setInt(2, itemId);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * Phục vụ order → trừ số lượng khỏi available_quantity.
     * Không để âm, trả về true nếu trừ thành công.
     */
    public boolean deductAvailableQuantity(int itemId, int qty) {
        String checkSql = "SELECT available_quantity FROM MenuItems WHERE item_id=?";
        String updateSql = "UPDATE MenuItems SET available_quantity = available_quantity - ? WHERE item_id=? AND available_quantity >= ?";
        try (Connection c = Database.getConnection()) {
            // Kiểm tra còn đủ không
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) < qty) return false;
                }
            }
            // Trừ số lượng
            try (PreparedStatement pu = c.prepareStatement(updateSql)) {
                pu.setInt(1, qty);
                pu.setInt(2, itemId);
                pu.setInt(3, qty);
                return pu.executeUpdate() > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
