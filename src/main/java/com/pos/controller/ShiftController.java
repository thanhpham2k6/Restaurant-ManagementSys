package com.pos.controller;

import com.pos.dao.ShiftDAO;
import com.pos.model.Shift;
import com.pos.model.User;
import com.pos.utils.PrinterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

public class ShiftController {
    private static final Logger log = LogManager.getLogger(ShiftController.class);
    private final ShiftDAO shiftDAO;
    private Shift currentShift;

    public ShiftController() {
        this.shiftDAO = new ShiftDAO();
        refreshCurrentShift();
    }

    public Shift refreshCurrentShift() {
        this.currentShift = shiftDAO.findCurrentOpenShift();
        return this.currentShift;
    }

    public Shift getCurrentShift() {
        if (this.currentShift == null) {
            refreshCurrentShift();
        }
        return this.currentShift;
    }

    public boolean hasOpenShift() {
        return getCurrentShift() != null && getCurrentShift().isOpen();
    }

    public Shift openShift(User user, BigDecimal initialCash, String note) throws Exception {
        if (hasOpenShift()) {
            throw new IllegalStateException("Đang có một ca làm việc mở (Ca #" + currentShift.getId() + "). Cần đóng ca trước!");
        }
        Shift s = new Shift();
        s.setUserId(user.getId());
        s.setUserFullName(user.getFullName());
        s.setInitialCash(initialCash != null ? initialCash : BigDecimal.ZERO);
        s.setNote(note);

        int id = shiftDAO.openShift(s);
        if (id <= 0) {
            throw new RuntimeException("Không thể mở ca làm việc vào database.");
        }
        s.setId(id);
        this.currentShift = s;
        log.info("Đã mở ca làm việc mới #{} bởi nhân viên {}", id, user.getFullName());
        return s;
    }

    public void closeShift(BigDecimal actualCash, String note) throws Exception {
        if (!hasOpenShift()) {
            throw new IllegalStateException("Không có ca làm việc nào đang mở để đóng.");
        }
        Shift s = getCurrentShift();
        s.setActualCash(actualCash != null ? actualCash : BigDecimal.ZERO);

        BigDecimal expectedCash = s.getInitialCash().add(s.getCashSales());
        BigDecimal diff = s.getActualCash().subtract(expectedCash);
        s.setDifferenceCash(diff);
        if (note != null && !note.isBlank()) {
            String oldNote = s.getNote() != null ? s.getNote() + " | " : "";
            s.setNote(oldNote + note);
        }

        shiftDAO.closeShift(s);
        log.info("Đã đóng ca làm việc #{}. Chênh lệch két tiền: {}", s.getId(), diff);

        // Tự động in Z-Report qua máy in nhiệt
        PrinterService.printZReport(s);

        this.currentShift = null;
    }

    public void recordSale(BigDecimal cashPart, BigDecimal transferPart) {
        if (hasOpenShift()) {
            shiftDAO.updateSales(currentShift.getId(), cashPart, transferPart);
            currentShift.setCashSales(currentShift.getCashSales().add(cashPart));
            currentShift.setTransferSales(currentShift.getTransferSales().add(transferPart));
        }
    }

    public List<Shift> getAllShifts() {
        return shiftDAO.findAll();
    }

    public int scheduleShift(User user, java.time.LocalDateTime startTime, BigDecimal initialCash, String note) {
        Shift s = new Shift();
        s.setUserId(user.getId());
        s.setUserFullName(user.getFullName());
        s.setStartTime(startTime != null ? startTime : java.time.LocalDateTime.now());
        s.setInitialCash(initialCash != null ? initialCash : BigDecimal.ZERO);
        s.setNote(note);
        int id = shiftDAO.createScheduledShift(s);
        log.info("Đã lên lịch xếp ca #{} cho nhân viên {} vào {}", id, user.getFullName(), s.getStartTime());
        return id;
    }
}
