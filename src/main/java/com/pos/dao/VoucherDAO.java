package com.pos.dao;

import com.pos.model.DiscountType;
import com.pos.model.Voucher;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {
    private static final Logger log = LogManager.getLogger(VoucherDAO.class);

    private Voucher mapRow(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setId(rs.getInt("id"));
        v.setCode(rs.getString("code"));
        v.setDiscountType(DiscountType.valueOf(rs.getString("discount_type")));
        v.setDiscountVal(rs.getBigDecimal("discount_val"));
        v.setMinOrderAmount(rs.getBigDecimal("min_order_amount"));
        v.setMaxDiscountAmount(rs.getBigDecimal("max_discount_amount"));
        Date sd = rs.getDate("start_date");
        if (sd != null) v.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        if (ed != null) v.setEndDate(ed.toLocalDate());
        v.setUsageLimit(rs.getInt("usage_limit"));
        v.setUsedCount(rs.getInt("used_count"));
        v.setActive(rs.getBoolean("is_active"));
        return v;
    }

    public List<Voucher> findAll() {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT * FROM vouchers ORDER BY id DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll vouchers error", e); }
        return list;
    }

    public Voucher findByCode(String code) {
        String sql = "SELECT * FROM vouchers WHERE code = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code != null ? code.trim().toUpperCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findByCode voucher error", e); }
        return null;
    }

    public Voucher findById(int id) {
        String sql = "SELECT * FROM vouchers WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById voucher error", e); }
        return null;
    }

    public void incrementUsedCount(int voucherId) {
        String sql = "UPDATE vouchers SET used_count = used_count + 1 WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("incrementUsedCount voucher error", e); }
    }

    public int insert(Voucher voucher) {
        String sql = "INSERT INTO vouchers (code, discount_type, discount_val, min_order_amount, " +
                     "max_discount_amount, start_date, end_date, usage_limit, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, voucher.getCode().toUpperCase().trim());
            ps.setString(2, voucher.getDiscountType().name());
            ps.setBigDecimal(3, voucher.getDiscountVal());
            ps.setBigDecimal(4, voucher.getMinOrderAmount());
            if (voucher.getMaxDiscountAmount() != null) {
                ps.setBigDecimal(5, voucher.getMaxDiscountAmount());
            } else {
                ps.setNull(5, Types.DECIMAL);
            }
            ps.setDate(6, Date.valueOf(voucher.getStartDate()));
            ps.setDate(7, Date.valueOf(voucher.getEndDate()));
            ps.setInt(8, voucher.getUsageLimit());
            ps.setBoolean(9, voucher.isActive());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    voucher.setId(gk.getInt(1));
                    return voucher.getId();
                }
            }
        } catch (SQLException e) { log.error("insert voucher error", e); }
        return -1;
    }

    public void update(Voucher voucher) {
        String sql = "UPDATE vouchers SET discount_type = ?, discount_val = ?, min_order_amount = ?, " +
                     "max_discount_amount = ?, start_date = ?, end_date = ?, usage_limit = ?, is_active = ? " +
                     "WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, voucher.getDiscountType().name());
            ps.setBigDecimal(2, voucher.getDiscountVal());
            ps.setBigDecimal(3, voucher.getMinOrderAmount());
            if (voucher.getMaxDiscountAmount() != null) {
                ps.setBigDecimal(4, voucher.getMaxDiscountAmount());
            } else {
                ps.setNull(4, Types.DECIMAL);
            }
            ps.setDate(5, Date.valueOf(voucher.getStartDate()));
            ps.setDate(6, Date.valueOf(voucher.getEndDate()));
            ps.setInt(7, voucher.getUsageLimit());
            ps.setBoolean(8, voucher.isActive());
            ps.setInt(9, voucher.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update voucher error", e); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM vouchers WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete voucher error", e); }
    }
}
