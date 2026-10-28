package com.pos.dao;

import com.pos.model.DiningTable;
import com.pos.model.TableStatus;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {
    private static final Logger log = LogManager.getLogger(TableDAO.class);

    private DiningTable mapRow(ResultSet rs) throws SQLException {
        int ver = 0;
        try { ver = rs.getInt("version"); } catch (SQLException ignored) {}
        DiningTable dt = new DiningTable(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("capacity"),
            TableStatus.valueOf(rs.getString("status")),
            ver
        );
        try {
            dt.setQrCodeToken(rs.getString("qr_code_token"));
        } catch (SQLException ignored) {}
        return dt;
    }

    public List<DiningTable> findAll() {
        List<DiningTable> list = new ArrayList<>();
        String sql = "SELECT * FROM dining_tables ORDER BY id";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll tables error", e); }
        return list;
    }

    public DiningTable findById(int id) {
        String sql = "SELECT * FROM dining_tables WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById table error", e); }
        return null;
    }

    /**
     * Pessimistic lock: SELECT ... WITH (UPDLOCK) trong một transaction.
     */
    public DiningTable lockForUpdate(Connection conn, int tableId) throws SQLException {
        String sql = "SELECT * FROM dining_tables WITH (UPDLOCK) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void updateStatus(Connection conn, int tableId, TableStatus status) throws SQLException {
        String sql = "UPDATE dining_tables SET status = ?, version = version + 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, tableId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(int tableId, TableStatus status) {
        String sql = "UPDATE dining_tables SET status = ?, version = version + 1 WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, tableId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateStatus error", e); }
    }

    public void updateQrCodeToken(int tableId, String token) {
        String sql = "UPDATE dining_tables SET qr_code_token = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, tableId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateQrCodeToken error", e); }
    }

    public void insert(DiningTable table) {
        String sql = "INSERT INTO dining_tables (name, capacity, status, qr_code_token) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, table.getName());
            ps.setInt(2, table.getCapacity());
            ps.setString(3, table.getStatus() != null ? table.getStatus().name() : TableStatus.AVAILABLE.name());
            ps.setString(4, table.getQrCodeToken() != null ? table.getQrCodeToken() : "QR-TABLE-" + System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) table.setId(gk.getInt(1));
            }
        } catch (SQLException e) {
            log.error("insert table error", e);
            throw new RuntimeException("Lỗi lưu bàn: " + (e.getMessage() != null && e.getMessage().contains("UNIQUE") ? "Tên bàn đã tồn tại!" : e.getMessage()), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM dining_tables WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete table error", e); }
    }
}
