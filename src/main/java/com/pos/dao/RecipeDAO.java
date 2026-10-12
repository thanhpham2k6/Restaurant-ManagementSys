package com.pos.dao;

import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeDAO {
    private static final Logger log = LogManager.getLogger(RecipeDAO.class);

    public List<RecipeLine> findByMenuItem(int menuItemId) {
        List<RecipeLine> list = new ArrayList<>();
        String sql = "SELECT r.id, r.qty_per_serving, i.id as ing_id, i.name as ing_name, i.unit, " +
                     "i.stock_qty, i.min_qty, i.is_active " +
                     "FROM recipes r JOIN ingredients i ON r.ingredient_id = i.id WHERE r.menu_item_id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ingredient ing = new Ingredient(rs.getInt("ing_id"), rs.getString("ing_name"),
                        rs.getString("unit"), rs.getBigDecimal("stock_qty"),
                        rs.getBigDecimal("min_qty"), rs.getBoolean("is_active"));
                    MenuItem mi = new MenuItem();
                    mi.setId(menuItemId);
                    list.add(new RecipeLine(rs.getInt("id"), mi, ing, rs.getBigDecimal("qty_per_serving")));
                }
            }
        } catch (SQLException e) { log.error("findByMenuItem recipe error", e); }
        return list;
    }

    /**
     * Tính số suất tối đa có thể phục vụ dựa trên tồn kho hiện tại.
     * Trả về Integer.MAX_VALUE nếu món không có công thức (không giới hạn).
     */
    public int remainingServings(int menuItemId) {
        String sql = "SELECT MIN(FLOOR(i.stock_qty / r.qty_per_serving)) as servings " +
                     "FROM recipes r JOIN ingredients i ON r.ingredient_id = i.id " +
                     "WHERE r.menu_item_id = ? AND r.qty_per_serving > 0";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal val = rs.getBigDecimal("servings");
                    return val != null ? val.intValue() : Integer.MAX_VALUE;
                }
            }
        } catch (SQLException e) { log.error("remainingServings error", e); }
        return Integer.MAX_VALUE;
    }

    public void upsert(int menuItemId, int ingredientId, BigDecimal qty) {
        String sql = "MERGE recipes AS target " +
                     "USING (SELECT ? as menu_item_id, ? as ingredient_id) AS source " +
                     "ON target.menu_item_id = source.menu_item_id AND target.ingredient_id = source.ingredient_id " +
                     "WHEN MATCHED THEN UPDATE SET qty_per_serving = ? " +
                     "WHEN NOT MATCHED THEN INSERT (menu_item_id, ingredient_id, qty_per_serving) VALUES (?,?,?);";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setInt(2, ingredientId);
            ps.setBigDecimal(3, qty);
            ps.setInt(4, menuItemId);
            ps.setInt(5, ingredientId);
            ps.setBigDecimal(6, qty);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("upsert recipe error", e); }
    }

    public void deleteByMenuItem(int menuItemId) {
        String sql = "DELETE FROM recipes WHERE menu_item_id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("deleteByMenuItem recipe error", e); }
    }
}
