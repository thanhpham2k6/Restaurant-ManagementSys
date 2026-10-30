package com.pos.controller;

import com.pos.dao.ReservationDAO;
import com.pos.dao.TableDAO;
import com.pos.model.Reservation;
import com.pos.model.ReservationStatus;
import com.pos.model.TableStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationController {
    private static final Logger log = LogManager.getLogger(ReservationController.class);
    private final ReservationDAO reservationDAO;
    private final TableDAO tableDAO;

    public ReservationController() {
        this.reservationDAO = new ReservationDAO();
        this.tableDAO = new TableDAO();
    }

    public List<Reservation> getAllReservations() {
        return reservationDAO.findAll();
    }

    public List<Reservation> getTodayReservations() {
        return reservationDAO.findUpcomingToday();
    }

    public List<Reservation> getByDate(LocalDate date) {
        return reservationDAO.findByDate(date);
    }

    public int createReservation(int tableId, Integer customerId, String customerName, String customerPhone,
                                 LocalDateTime time, int guestCount, BigDecimal deposit, String note) throws Exception {
        return createReservation(tableId, customerId, customerName, customerPhone, time, guestCount, deposit, "DIRECT", note);
    }

    public int createReservation(int tableId, Integer customerId, String customerName, String customerPhone,
                                 LocalDateTime time, int guestCount, BigDecimal deposit, String channel, String note) throws Exception {
        if (customerName == null || customerName.isBlank()) throw new IllegalArgumentException("Vui lòng nhập tên khách đặt bàn.");
        if (customerPhone == null || customerPhone.isBlank()) throw new IllegalArgumentException("Vui lòng nhập SĐT khách đặt bàn.");
        if (time == null || time.isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new IllegalArgumentException("Thời gian đặt bàn phải từ thời điểm hiện tại trở đi.");
        }

        Reservation res = new Reservation();
        res.setTableId(tableId);
        res.setCustomerId(customerId);
        res.setCustomerName(customerName.trim());
        res.setCustomerPhone(customerPhone.trim());
        res.setReservationTime(time);
        res.setGuestCount(guestCount);
        res.setDepositAmount(deposit != null ? deposit : BigDecimal.ZERO);
        res.setStatus(ReservationStatus.PENDING);
        res.setChannel(channel != null ? channel : "DIRECT");
        res.setNote(note);

        int id = reservationDAO.insert(res);
        if (id <= 0) {
            throw new RuntimeException("Lỗi lưu cơ sở dữ liệu: Không thể tạo lịch đặt bàn!");
        }

        // Đặt trạng thái bàn thành RESERVED nếu đặt trong ngày hôm nay
        if (time.toLocalDate().isEqual(LocalDate.now())) {
            tableDAO.updateStatus(tableId, TableStatus.RESERVED);
        }

        return id;
    }

    public void confirmReservation(int resId) {
        reservationDAO.updateStatus(resId, ReservationStatus.CONFIRMED);
    }

    public void seatGuest(int resId, int tableId) {
        reservationDAO.updateStatus(resId, ReservationStatus.SEATED);
        tableDAO.updateStatus(tableId, TableStatus.OCCUPIED);
    }

    public void cancelReservation(int resId, int tableId) {
        reservationDAO.updateStatus(resId, ReservationStatus.CANCELLED);
        tableDAO.updateStatus(tableId, TableStatus.AVAILABLE);
    }

    public void updateReservation(Reservation res) {
        reservationDAO.update(res);
    }
}
