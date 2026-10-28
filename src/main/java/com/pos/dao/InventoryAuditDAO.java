package com.pos.dao;

import com.pos.model.AuditStatus;
import com.pos.model.InventoryAudit;
import com.pos.model.InventoryAuditDetail;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryAuditDAO {
    private static final Logger log = LogManager.getLogger(InventoryAuditDAO.class);

    public int createAudit(InventoryAudit audit) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            return createAudit(c, audit);
        } catch (SQLException e) {
            log.error("createAudit error", e);
            return -1;
        }
    }

    public int createAudit(Connection c, InventoryAudit audit) throws SQLException {
        String sql = "INSERT INTO inventory_audits (audit_code, user_id, audit_date, status, note) " +
                     "VALUES (?, ?, GETDATE(), ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, audit.getAuditCode());
            ps.setInt(2, audit.getUserId());
            ps.setString(3, audit.getStatus().name());
            ps.setString(4, audit.getNote());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    audit.setId(gk.getInt(1));
                    return audit.getId();
                }
            }
        }
        return -1;
    }

    public void insertAuditDetail(int auditId, InventoryAuditDetail d) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            insertAuditDetail(c, auditId, d);
        } catch (SQLException e) {
            log.error("insertAuditDetail error", e);
        }
    }

    public void insertAuditDetail(Connection c, int auditId, InventoryAuditDetail d) throws SQLException {
        String sql = "INSERT INTO inventory_audit_details (audit_id, item_id, system_qty, actual_qty, difference_qty, reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, auditId);
            ps.setInt(2, d.getItemId());
            ps.setBigDecimal(3, d.getSystemQty());
            ps.setBigDecimal(4, d.getActualQty());
            BigDecimal diff = d.getDifferenceQty();
            if (diff == null) diff = d.getActualQty().subtract(d.getSystemQty());
            ps.setBigDecimal(5, diff);
            ps.setString(6, d.getReason());
            ps.executeUpdate();
        }
    }

    public List<InventoryAudit> findAll() {
        List<InventoryAudit> list = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as user_full_name " +
                     "FROM inventory_audits a " +
                     "JOIN users u ON a.user_id = u.id " +
                     "ORDER BY a.audit_date DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                InventoryAudit a = new InventoryAudit();
                a.setId(rs.getInt("id"));
                a.setAuditCode(rs.getString("audit_code"));
                a.setUserId(rs.getInt("user_id"));
                a.setUserFullName(rs.getString("user_full_name"));
                Timestamp ts = rs.getTimestamp("audit_date");
                if (ts != null) a.setAuditDate(ts.toLocalDateTime());
                a.setStatus(AuditStatus.valueOf(rs.getString("status")));
                a.setNote(rs.getString("note"));
                list.add(a);
            }
        } catch (SQLException e) { log.error("findAll audits error", e); }
        return list;
    }

    public List<InventoryAuditDetail> findDetailsByAuditId(int auditId) {
        List<InventoryAuditDetail> list = new ArrayList<>();
        String sql = "SELECT d.*, i.name as item_name, i.unit as item_unit " +
                     "FROM inventory_audit_details d " +
                     "JOIN inventory_items i ON d.item_id = i.id " +
                     "WHERE d.audit_id = ? ORDER BY i.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, auditId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryAuditDetail d = new InventoryAuditDetail();
                    d.setId(rs.getInt("id"));
                    d.setAuditId(rs.getInt("audit_id"));
                    d.setItemId(rs.getInt("item_id"));
                    d.setItemName(rs.getString("item_name"));
                    d.setItemUnit(rs.getString("item_unit"));
                    d.setSystemQty(rs.getBigDecimal("system_qty"));
                    d.setActualQty(rs.getBigDecimal("actual_qty"));
                    d.setDifferenceQty(rs.getBigDecimal("difference_qty"));
                    d.setReason(rs.getString("reason"));
                    list.add(d);
                }
            }
        } catch (SQLException e) { log.error("findDetailsByAuditId error", e); }
        return list;
    }

    public void completeAudit(int auditId) {
        String sql = "UPDATE inventory_audits SET status = 'COMPLETED' WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, auditId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("completeAudit error", e); }
    }
}
