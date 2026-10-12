package com.pos.dao;

import com.pos.model.Ingredient;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IngredientDAO {
    private static final Logger log = LogManager.getLogger(IngredientDAO.class);

    private Ingredient mapRow(ResultSet rs) throws SQLException {
        return new Ingredient(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("unit"),
            rs.getBigDecimal("stock_qty"),
            rs.getBigDecimal("min_qty"),
            rs.getBoolean("is_active")
        );
    }

    public List<Ingredient> findAll() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT * FROM ingredients WHERE is_active=1 ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll ingredients error", e); }
        return list;
    }

    public List<Ingredient> findLowStock() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT * FROM ingredients WHERE is_active=1 AND stock_qty <= min_qty ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findLowStock error", e); }
        return list;
    }

    /**
     * Pessimistic lock cho ingredient trong transaction thủ công.
     */
    public Ingredient lockForUpdate(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM ingredients WITH (UPDLOCK) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void updateQty(Connection conn, int id, BigDecimal newQty) throws SQLException {
        String sql = "UPDATE ingredients SET stock_qty = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newQty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public int insert(Ingredient ing) {
        String sql = "INSERT INTO ingredients (name, unit, stock_qty, min_qty, is_active) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ing.getName());
            ps.setString(2, ing.getUnit());
            ps.setBigDecimal(3, ing.getStockQty());
            ps.setBigDecimal(4, ing.getMinQty());
            ps.setBoolean(5, ing.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { log.error("insert ingredient error", e); }
        return -1;
    }

    public void update(Ingredient ing) {
        String sql = "UPDATE ingredients SET name=?, unit=?, stock_qty=?, min_qty=?, is_active=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ing.getName());
            ps.setString(2, ing.getUnit());
            ps.setBigDecimal(3, ing.getStockQty());
            ps.setBigDecimal(4, ing.getMinQty());
            ps.setBoolean(5, ing.isActive());
            ps.setInt(6, ing.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update ingredient error", e); }
    }
}
