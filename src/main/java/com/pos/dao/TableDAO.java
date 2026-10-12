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
        return new DiningTable(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("capacity"),
            TableStatus.valueOf(rs.getString("status")),
            rs.getInt("version")
        );
    }

    public List<DiningTable> findAll() {
        List<DiningTable> list = new ArrayList<>();
        String sql = "SELECT * FROM dining_tables ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll tables error", e); }
        return list;
    }

    /**
     * Pessimistic lock: SELECT ... WITH (UPDLOCK) trong một transaction.
     * Phải được gọi trong transaction thủ công (autoCommit=false).
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

    public void insert(DiningTable table) {
        String sql = "INSERT INTO dining_tables (name, capacity, status) VALUES (?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table.getName());
            ps.setInt(2, table.getCapacity());
            ps.setString(3, table.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("insert table error", e); }
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
