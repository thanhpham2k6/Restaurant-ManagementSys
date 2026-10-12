package com.pos.dao;

import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDAO {
    private static final Logger log = LogManager.getLogger(StockMovementDAO.class);

    public void insert(Connection conn, StockMovement sm) throws SQLException {
        String sql = "INSERT INTO stock_movements (ingredient_id, user_id, order_id, type, quantity, note) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sm.getIngredient().getId());
            ps.setInt(2, sm.getUser().getId());
            if (sm.getOrderId() != null) ps.setInt(3, sm.getOrderId()); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, sm.getType().name());
            ps.setBigDecimal(5, sm.getQuantity());
            ps.setString(6, sm.getNote());
            ps.executeUpdate();
        }
    }

    public List<StockMovement> findByIngredient(int ingredientId) {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT sm.*, i.name as ing_name, i.unit, i.stock_qty, i.min_qty, i.is_active, " +
                     "u.username, u.full_name, u.role, u.password_hash, u.is_active as u_active " +
                     "FROM stock_movements sm " +
                     "JOIN ingredients i ON sm.ingredient_id = i.id " +
                     "JOIN users u ON sm.user_id = u.id " +
                     "WHERE sm.ingredient_id = ? ORDER BY sm.created_at DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ingredient ing = new Ingredient(rs.getInt("ingredient_id"), rs.getString("ing_name"),
                        rs.getString("unit"), rs.getBigDecimal("stock_qty"), rs.getBigDecimal("min_qty"),
                        rs.getBoolean("is_active"));
                    User u = new User(rs.getInt("user_id"), rs.getString("username"),
                        rs.getString("password_hash"), rs.getString("full_name"),
                        Role.valueOf(rs.getString("role")), rs.getBoolean("u_active"));
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setIngredient(ing);
                    sm.setUser(u);
                    int oid = rs.getInt("order_id");
                    sm.setOrderId(rs.wasNull() ? null : oid);
                    sm.setType(MovementType.valueOf(rs.getString("type")));
                    sm.setQuantity(rs.getBigDecimal("quantity"));
                    sm.setNote(rs.getString("note"));
                    Timestamp ca = rs.getTimestamp("created_at");
                    if (ca != null) sm.setCreatedAt(ca.toLocalDateTime());
                    list.add(sm);
                }
            }
        } catch (SQLException e) { log.error("findByIngredient movement error", e); }
        return list;
    }
}
