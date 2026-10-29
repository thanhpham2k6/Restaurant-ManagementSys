package com.pos.dao;

import com.pos.model.CashTransaction;
import com.pos.utils.DatabaseConnection;
import com.pos.utils.DatabaseEncodingFixer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CashTransactionDAO {
    private static final Logger log = LogManager.getLogger(CashTransactionDAO.class);

    private CashTransaction mapRow(ResultSet rs) throws SQLException {
        CashTransaction t = new CashTransaction();
        t.setId(rs.getInt("id"));
        t.setCode(rs.getString("code"));
        t.setType(rs.getString("type"));
        t.setCategory(DatabaseEncodingFixer.fixMojibake(rs.getString("category")));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setPaymentMethod(DatabaseEncodingFixer.fixMojibake(rs.getString("payment_method")));
        t.setPersonName(DatabaseEncodingFixer.fixMojibake(rs.getString("person_name")));
        t.setNote(DatabaseEncodingFixer.fixMojibake(rs.getString("note")));
        t.setUserId(rs.getInt("user_id"));
        try {
            t.setUserName(DatabaseEncodingFixer.fixMojibake(rs.getString("user_name")));
        } catch (SQLException ignored) {
            t.setUserName("");
        }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            t.setCreatedAt(ts.toLocalDateTime());
        }
        return t;
    }

    private static final String BASE_SELECT =
        "SELECT ct.id, ct.code, ct.type, ct.category, ct.amount, ct.payment_method, " +
        "ct.person_name, ct.note, ct.user_id, ct.created_at, u.full_name AS user_name " +
        "FROM cash_transactions ct " +
        "LEFT JOIN users u ON ct.user_id = u.id ";

    public List<CashTransaction> findAll() {
        List<CashTransaction> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY ct.created_at DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAll cash_transactions error", e);
        }
        return list;
    }

    public List<CashTransaction> findByDateRange(LocalDate fromDate, LocalDate toDate, String typeFilter) {
        List<CashTransaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE CAST(ct.created_at AS DATE) >= ? AND CAST(ct.created_at AS DATE) <= ? ");
        if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL")) {
            sql.append("AND ct.type = ? ");
        }
        sql.append("ORDER BY ct.created_at DESC");

        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setDate(1, java.sql.Date.valueOf(fromDate));
            ps.setDate(2, java.sql.Date.valueOf(toDate));
            if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL")) {
                ps.setString(3, typeFilter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByDateRange error", e);
        }
        return list;
    }

    public int insert(CashTransaction tx) {
        String sql = "INSERT INTO cash_transactions (code, type, category, amount, payment_method, person_name, note, user_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tx.getCode());
            ps.setString(2, tx.getType());
            ps.setString(3, tx.getCategory());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setString(5, tx.getPaymentMethod());
            ps.setString(6, tx.getPersonName());
            ps.setString(7, tx.getNote());
            ps.setInt(8, tx.getUserId());
            ps.setTimestamp(9, tx.getCreatedAt() != null ? Timestamp.valueOf(tx.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            log.error("insert cash_transaction error", e);
        }
        return -1;
    }

    public void delete(int id) {
        String sql = "DELETE FROM cash_transactions WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("delete cash_transaction error", e);
        }
    }

    public BigDecimal getTotalReceipt(LocalDate fromDate, LocalDate toDate) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE type = 'RECEIPT' AND CAST(created_at AS DATE) >= ? AND CAST(created_at AS DATE) <= ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fromDate));
            ps.setDate(2, java.sql.Date.valueOf(toDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            log.error("getTotalReceipt error", e);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalPayment(LocalDate fromDate, LocalDate toDate) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE type = 'PAYMENT' AND CAST(created_at AS DATE) >= ? AND CAST(created_at AS DATE) <= ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fromDate));
            ps.setDate(2, java.sql.Date.valueOf(toDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            log.error("getTotalPayment error", e);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getCurrentCashBalance() {
        String sql = "SELECT " +
                     "COALESCE(SUM(CASE WHEN type = 'RECEIPT' AND payment_method = N'Tiền mặt' THEN amount ELSE 0 END), 0) - " +
                     "COALESCE(SUM(CASE WHEN type = 'PAYMENT' AND payment_method = N'Tiền mặt' THEN amount ELSE 0 END), 0) " +
                     "FROM cash_transactions";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            log.error("getCurrentCashBalance error", e);
        }
        return BigDecimal.ZERO;
    }
}
