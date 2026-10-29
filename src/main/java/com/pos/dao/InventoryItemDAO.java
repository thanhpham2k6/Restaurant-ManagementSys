package com.pos.dao;

import com.pos.model.InventoryItem;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryItemDAO {
    private static final Logger log = LogManager.getLogger(InventoryItemDAO.class);

    private InventoryItem mapRow(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setId(rs.getInt("id"));
        int supId = rs.getInt("supplier_id");
        if (!rs.wasNull()) item.setSupplierId(supId);
        try {
            item.setSupplierName(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("supplier_name")));
        } catch (SQLException ignored) {}
        item.setName(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("name")));
        item.setCategory(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("category")));
        item.setUnit(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("unit")));
        item.setCostPrice(rs.getBigDecimal("cost_price"));
        item.setStockQty(rs.getBigDecimal("stock_qty"));
        item.setMinQty(rs.getBigDecimal("min_qty"));
        item.setActive(rs.getBoolean("is_active"));
        return item;
    }

    private static final String BASE_SELECT =
        "SELECT i.*, s.name as supplier_name " +
        "FROM inventory_items i " +
        "LEFT JOIN suppliers s ON i.supplier_id = s.id ";

    public List<InventoryItem> findAll() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY i.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll inventory items error", e); }
        return list;
    }

    public List<InventoryItem> findActive() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE i.is_active = 1 ORDER BY i.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findActive inventory items error", e); }
        return list;
    }

    public List<InventoryItem> findLowStock() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE i.is_active = 1 AND i.stock_qty <= i.min_qty ORDER BY i.stock_qty ASC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findLowStock error", e); }
        return list;
    }

    public InventoryItem findById(int id) {
        String sql = BASE_SELECT + "WHERE i.id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById inventory item error", e); }
        return null;
    }

    public int insert(InventoryItem item) {
        String sql = "INSERT INTO inventory_items (supplier_id, name, category, unit, cost_price, stock_qty, min_qty, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (item.getSupplierId() != null) {
                ps.setInt(1, item.getSupplierId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getUnit());
            ps.setBigDecimal(5, item.getCostPrice());
            ps.setBigDecimal(6, item.getStockQty());
            ps.setBigDecimal(7, item.getMinQty());
            ps.setBoolean(8, item.isActive());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    item.setId(gk.getInt(1));
                    return item.getId();
                }
            }
        } catch (SQLException e) { log.error("insert inventory item error", e); }
        return -1;
    }

    public void update(InventoryItem item) {
        String sql = "UPDATE inventory_items SET supplier_id = ?, name = ?, category = ?, unit = ?, " +
                     "cost_price = ?, min_qty = ?, is_active = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (item.getSupplierId() != null) {
                ps.setInt(1, item.getSupplierId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getUnit());
            ps.setBigDecimal(5, item.getCostPrice());
            ps.setBigDecimal(6, item.getMinQty());
            ps.setBoolean(7, item.isActive());
            ps.setInt(8, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update inventory item error", e); }
    }

    public void updateStock(Connection conn, int itemId, BigDecimal deltaQty) throws SQLException {
        String sql = "UPDATE inventory_items SET stock_qty = stock_qty + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, deltaQty);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        }
    }

    public void setStock(Connection conn, int itemId, BigDecimal newQty) throws SQLException {
        String sql = "UPDATE inventory_items SET stock_qty = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newQty);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) {
        String sql = "UPDATE inventory_items SET is_active = 0 WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete (deactivate) inventory item error", e); }
    }
}
