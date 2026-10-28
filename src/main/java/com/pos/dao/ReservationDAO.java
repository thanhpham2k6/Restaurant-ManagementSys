package com.pos.dao;

import com.pos.model.Reservation;
import com.pos.model.ReservationStatus;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    private static final Logger log = LogManager.getLogger(ReservationDAO.class);

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setTableId(rs.getInt("table_id"));
        try {
            r.setTableName(rs.getString("table_name"));
        } catch (SQLException ignored) {}
        int cid = rs.getInt("customer_id");
        if (!rs.wasNull()) r.setCustomerId(cid);
        r.setCustomerName(rs.getString("customer_name"));
        r.setCustomerPhone(rs.getString("customer_phone"));
        Timestamp rt = rs.getTimestamp("reservation_time");
        if (rt != null) r.setReservationTime(rt.toLocalDateTime());
        r.setGuestCount(rs.getInt("guest_count"));
        r.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        try {
            String ch = rs.getString("channel");
            if (ch != null) r.setChannel(ch);
        } catch (SQLException ignored) {}
        r.setNote(rs.getString("note"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) r.setCreatedAt(ct.toLocalDateTime());
        return r;
    }

    private static final String BASE_SELECT =
        "SELECT r.*, t.name as table_name " +
        "FROM reservations r " +
        "JOIN dining_tables t ON r.table_id = t.id ";

    public List<Reservation> findAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY r.reservation_time DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findAll reservations error", e); }
        return list;
    }

    public List<Reservation> findByDate(LocalDate date) {
        List<Reservation> list = new ArrayList<>();
        String sql = BASE_SELECT +
                     "WHERE CAST(r.reservation_time AS DATE) = ? ORDER BY r.reservation_time ASC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { log.error("findByDate reservations error", e); }
        return list;
    }

    public List<Reservation> findUpcomingToday() {
        return findByDate(LocalDate.now());
    }

    public Reservation findById(int id) {
        String sql = BASE_SELECT + "WHERE r.id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById reservation error", e); }
        return null;
    }

    public int insert(Reservation res) {
        String sql = "INSERT INTO reservations (table_id, customer_id, customer_name, customer_phone, " +
                     "reservation_time, guest_count, deposit_amount, status, channel, note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, res.getTableId());
            if (res.getCustomerId() != null) {
                ps.setInt(2, res.getCustomerId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, res.getCustomerName());
            ps.setString(4, res.getCustomerPhone());
            ps.setTimestamp(5, Timestamp.valueOf(res.getReservationTime()));
            ps.setInt(6, res.getGuestCount());
            ps.setBigDecimal(7, res.getDepositAmount());
            ps.setString(8, res.getStatus().name());
            ps.setString(9, res.getChannel() != null ? res.getChannel() : "DIRECT");
            ps.setString(10, res.getNote());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    res.setId(gk.getInt(1));
                    return res.getId();
                }
            }
        } catch (SQLException e) { log.error("insert reservation error", e); }
        return -1;
    }

    public void updateStatus(int id, ReservationStatus status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateStatus reservation error", e); }
    }

    public void update(Reservation res) {
        String sql = "UPDATE reservations SET table_id = ?, customer_name = ?, customer_phone = ?, " +
                     "reservation_time = ?, guest_count = ?, deposit_amount = ?, status = ?, channel = ?, note = ? " +
                     "WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, res.getTableId());
            ps.setString(2, res.getCustomerName());
            ps.setString(3, res.getCustomerPhone());
            ps.setTimestamp(4, Timestamp.valueOf(res.getReservationTime()));
            ps.setInt(5, res.getGuestCount());
            ps.setBigDecimal(6, res.getDepositAmount());
            ps.setString(7, res.getStatus().name());
            ps.setString(8, res.getChannel() != null ? res.getChannel() : "DIRECT");
            ps.setString(9, res.getNote());
            ps.setInt(10, res.getId());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("update reservation error", e); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("delete reservation error", e); }
    }
}
