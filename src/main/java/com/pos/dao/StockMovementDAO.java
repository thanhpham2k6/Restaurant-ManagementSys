package com.pos.dao;

import com.pos.model.MovementType;
import com.pos.model.StockMovement;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDAO {
    private static final Logger log = LogManager.getLogger(StockMovementDAO.class);

    private StockMovement mapRow(ResultSet rs) throws SQLException {
        StockMovement sm = new StockMovement();
        sm.setId(rs.getInt("id"));
        sm.setItemId(rs.getInt("item_id"));
        try { sm.setItemName(rs.getString("item_name")); } catch (SQLException ignored) {}
        int supId = rs.getInt("supplier_id");
        if (!rs.wasNull()) sm.setSupplierId(supId);
        try { sm.setSupplierName(rs.getString("supplier_name")); } catch (SQLException ignored) {}
        sm.setUserId(rs.getInt("user_id"));
        try { sm.setUserFullName(rs.getString("user_full_name")); } catch (SQLException ignored) {}
        sm.setType(MovementType.valueOf(rs.getString("type")));
        sm.setQuantity(rs.getBigDecimal("quantity"));
        sm.setUnitPrice(rs.getBigDecimal("unit_price"));
        sm.setNote(rs.getString("note"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) sm.setCreatedAt(ca.toLocalDateTime());
        return sm;
    }

    private static final String BASE_SELECT =
        "SELECT sm.*, i.name as item_name, s.name as supplier_name, u.full_name as user_full_name " +
        "FROM stock_movements sm " +
        "JOIN inventory_items i ON sm.item_id = i.id " +
        "JOIN users u ON sm.user_id = u.id " +
        "LEFT JOIN suppliers s ON sm.supplier_id = s.id ";

    public void insert(Connection conn, StockMovement sm) throws SQLException {
        String sql = "INSERT INTO stock_movements (item_id, supplier_id, user_id, type, quantity, unit_price, note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sm.getItemId());
            if (sm.getSupplierId() != null) {
                ps.setInt(2, sm.getSupplierId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, sm.getUserId());
            ps.setString(4, sm.getType().name());
            ps.setBigDecimal(5, sm.getQuantity());
            ps.setBigDecimal(6, sm.getUnitPrice());
            ps.setString(7, sm.getNote());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) sm.setId(gk.getInt(1));
            }
        }
    }

    public void insert(StockMovement sm) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            insert(conn, sm);
        } catch (SQLException e) {
            log.error("insert stock movement error", e);
        }
    }

    public List<StockMovement> findAll() {
        List<StockMovement> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY sm.created_at DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll movements error", e); }
        return list;
    }

    public List<StockMovement> findByItem(int itemId) {
        List<StockMovement> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE sm.item_id = ? ORDER BY sm.created_at DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { log.error("findByItem movements error", e); }
        return list;
    }
}
