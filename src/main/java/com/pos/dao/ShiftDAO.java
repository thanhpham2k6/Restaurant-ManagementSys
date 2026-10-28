package com.pos.dao;

import com.pos.model.Shift;
import com.pos.model.ShiftStatus;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShiftDAO {
    private static final Logger log = LogManager.getLogger(ShiftDAO.class);

    private Shift mapRow(ResultSet rs) throws SQLException {
        Shift s = new Shift();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        try {
            s.setUserFullName(rs.getString("user_full_name"));
        } catch (SQLException ignored) {}
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) s.setStartTime(st.toLocalDateTime());
        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) s.setEndTime(et.toLocalDateTime());
        s.setInitialCash(rs.getBigDecimal("initial_cash"));
        s.setActualCash(rs.getBigDecimal("actual_cash"));
        s.setCashSales(rs.getBigDecimal("cash_sales"));
        s.setTransferSales(rs.getBigDecimal("transfer_sales"));
        s.setDifferenceCash(rs.getBigDecimal("difference_cash"));
        s.setStatus(ShiftStatus.valueOf(rs.getString("status")));
        s.setNote(rs.getString("note"));
        return s;
    }

    private static final String BASE_SELECT =
        "SELECT s.*, u.full_name as user_full_name " +
        "FROM shifts s JOIN users u ON s.user_id = u.id ";

    public Shift findActiveShiftByUserId(int userId) {
        String sql = BASE_SELECT + "WHERE s.user_id = ? AND s.status = 'OPEN'";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findActiveShiftByUserId error", e); }
        return null;
    }

    public Shift findCurrentOpenShift() {
        String sql = BASE_SELECT + "WHERE s.status = 'OPEN' ORDER BY s.id DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { log.error("findCurrentOpenShift error", e); }
        return null;
    }

    public Shift findById(int id) {
        String sql = BASE_SELECT + "WHERE s.id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById shift error", e); }
        return null;
    }

    public List<Shift> findAll() {
        List<Shift> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY s.id DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll shifts error", e); }
        return list;
    }

    public int openShift(Shift shift) {
        String sql = "INSERT INTO shifts (user_id, start_time, initial_cash, status, note) " +
                     "VALUES (?, GETDATE(), ?, 'OPEN', ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, shift.getUserId());
            ps.setBigDecimal(2, shift.getInitialCash());
            ps.setString(3, shift.getNote());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    int id = gk.getInt(1);
                    shift.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) { log.error("openShift error", e); }
        return -1;
    }

    public int createScheduledShift(Shift shift) {
        String sql = "INSERT INTO shifts (user_id, start_time, initial_cash, status, note) " +
                     "VALUES (?, ?, ?, 'OPEN', ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, shift.getUserId());
            ps.setTimestamp(2, shift.getStartTime() != null ? Timestamp.valueOf(shift.getStartTime()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setBigDecimal(3, shift.getInitialCash() != null ? shift.getInitialCash() : BigDecimal.ZERO);
            ps.setString(4, shift.getNote());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    int id = gk.getInt(1);
                    shift.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) { log.error("createScheduledShift error", e); }
        return -1;
    }

    public void updateSales(int shiftId, java.math.BigDecimal cashDelta, java.math.BigDecimal transferDelta) {
        String sql = "UPDATE shifts SET cash_sales = cash_sales + ?, transfer_sales = transfer_sales + ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, cashDelta);
            ps.setBigDecimal(2, transferDelta);
            ps.setInt(3, shiftId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateSales shift error", e); }
    }

    public void closeShift(Shift shift) {
        String sql = "UPDATE shifts SET end_time = GETDATE(), actual_cash = ?, difference_cash = ?, " +
                     "status = 'CLOSED', note = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, shift.getActualCash());
            ps.setBigDecimal(2, shift.getDifferenceCash());
            ps.setString(3, shift.getNote());
            ps.setInt(4, shift.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("closeShift error", e); }
    }
}
