package com.pos.dao;

import com.pos.model.Supplier;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {
    private static final Logger log = LogManager.getLogger(SupplierDAO.class);

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setName(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("name")));
        s.setContactPerson(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("contact_person")));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(com.pos.utils.DatabaseEncodingFixer.fixMojibake(rs.getString("address")));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll suppliers error", e); }
        return list;
    }

    public List<Supplier> findActive() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers WHERE is_active = 1 ORDER BY name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findActive suppliers error", e); }
        return list;
    }

    public Supplier findById(int id) {
        String sql = "SELECT * FROM suppliers WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById supplier error", e); }
        return null;
    }

    public int insert(Supplier s) {
        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setBoolean(6, s.isActive());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    s.setId(gk.getInt(1));
                    return s.getId();
                }
            }
        } catch (SQLException e) { log.error("insert supplier error", e); }
        return -1;
    }

    public void update(Supplier s) {
        String sql = "UPDATE suppliers SET name = ?, contact_person = ?, phone = ?, email = ?, " +
                     "address = ?, is_active = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setBoolean(6, s.isActive());
            ps.setInt(7, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update supplier error", e); }
    }

    public void delete(int id) {
        String sql = "UPDATE suppliers SET is_active = 0 WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete (deactivate) supplier error", e); }
    }
}
