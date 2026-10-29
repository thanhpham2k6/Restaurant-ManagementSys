package com.pos.dao;

import com.pos.model.EmployeeAttendance;
import com.pos.utils.DatabaseConnection;
import com.pos.utils.DatabaseEncodingFixer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAttendanceDAO {
    private static final Logger log = LogManager.getLogger(EmployeeAttendanceDAO.class);

    private EmployeeAttendance mapRow(ResultSet rs) throws SQLException {
        EmployeeAttendance a = new EmployeeAttendance();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setEmployeeName(DatabaseEncodingFixer.fixMojibake(rs.getString("full_name")));
        a.setRoleName(rs.getString("role"));
        java.sql.Date d = rs.getDate("work_date");
        if (d != null) a.setWorkDate(d.toLocalDate());
        a.setShiftName(DatabaseEncodingFixer.fixMojibake(rs.getString("shift_name")));

        Timestamp inTs = rs.getTimestamp("check_in_time");
        if (inTs != null) a.setCheckInTime(inTs.toLocalDateTime());

        Timestamp outTs = rs.getTimestamp("check_out_time");
        if (outTs != null) a.setCheckOutTime(outTs.toLocalDateTime());

        a.setHoursWorked(rs.getBigDecimal("hours_worked"));
        a.setStatus(DatabaseEncodingFixer.fixMojibake(rs.getString("status")));
        a.setNote(DatabaseEncodingFixer.fixMojibake(rs.getString("note")));

        Timestamp crTs = rs.getTimestamp("created_at");
        if (crTs != null) a.setCreatedAt(crTs.toLocalDateTime());

        return a;
    }

    private static final String BASE_SELECT =
        "SELECT ea.id, ea.user_id, ea.work_date, ea.shift_name, ea.check_in_time, " +
        "ea.check_out_time, ea.hours_worked, ea.status, ea.note, ea.created_at, " +
        "u.full_name, u.role " +
        "FROM employee_attendances ea " +
        "JOIN users u ON ea.user_id = u.id ";

    public List<EmployeeAttendance> findAll() {
        List<EmployeeAttendance> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY ea.work_date DESC, ea.check_in_time DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAll employee_attendances error", e);
        }
        return list;
    }

    public List<EmployeeAttendance> findByDate(LocalDate date) {
        List<EmployeeAttendance> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE ea.work_date = ? ORDER BY ea.check_in_time DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByDate employee_attendances error", e);
        }
        return list;
    }

    public EmployeeAttendance findActiveCheckIn(int userId, LocalDate date) {
        String sql = BASE_SELECT + "WHERE ea.user_id = ? AND ea.work_date = ? AND ea.check_out_time IS NULL";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            log.error("findActiveCheckIn error", e);
        }
        return null;
    }

    public int checkIn(int userId, String shiftName, String note) {
        String sql = "INSERT INTO employee_attendances (user_id, work_date, shift_name, check_in_time, status, note, hours_worked) " +
                     "VALUES (?, CAST(GETDATE() AS DATE), ?, GETDATE(), N'Đúng giờ', ?, 0)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, shiftName);
            ps.setString(3, note != null ? note : "");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            log.error("checkIn error", e);
        }
        return -1;
    }

    public boolean checkOut(int id, String note) {
        String selectSql = "SELECT check_in_time FROM employee_attendances WHERE id = ?";
        LocalDateTime inTime = null;
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    if (ts != null) inTime = ts.toLocalDateTime();
                }
            }
        } catch (SQLException e) {
            log.error("checkOut read error", e);
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal hours = BigDecimal.ZERO;
        if (inTime != null) {
            long minutes = Duration.between(inTime, now).toMinutes();
            hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        String updateSql = "UPDATE employee_attendances SET check_out_time = ?, hours_worked = ?, note = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.setBigDecimal(2, hours);
            ps.setString(3, note != null ? note : "");
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("checkOut update error", e);
        }
        return false;
    }

    public void delete(int id) {
        String sql = "DELETE FROM employee_attendances WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("delete attendance error", e);
        }
    }
}
