package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Shift {
    private int id;
    private int userId;
    private String userFullName; // joined field
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal initialCash;
    private BigDecimal actualCash;
    private BigDecimal cashSales;
    private BigDecimal transferSales;
    private BigDecimal differenceCash;
    private ShiftStatus status;
    private String note;

    public Shift() {
        this.initialCash = BigDecimal.ZERO;
        this.cashSales = BigDecimal.ZERO;
        this.transferSales = BigDecimal.ZERO;
        this.status = ShiftStatus.OPEN;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public BigDecimal getInitialCash() { return initialCash; }
    public void setInitialCash(BigDecimal initialCash) { this.initialCash = initialCash; }

    public BigDecimal getActualCash() { return actualCash; }
    public void setActualCash(BigDecimal actualCash) { this.actualCash = actualCash; }

    public BigDecimal getCashSales() { return cashSales; }
    public void setCashSales(BigDecimal cashSales) { this.cashSales = cashSales; }

    public BigDecimal getTransferSales() { return transferSales; }
    public void setTransferSales(BigDecimal transferSales) { this.transferSales = transferSales; }

    public BigDecimal getDifferenceCash() { return differenceCash; }
    public void setDifferenceCash(BigDecimal differenceCash) { this.differenceCash = differenceCash; }

    public ShiftStatus getStatus() { return status; }
    public void setStatus(ShiftStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isOpen() { return status == ShiftStatus.OPEN; }

    @Override
    public String toString() {
        return "Ca #" + id + " - " + (userFullName != null ? userFullName : "N/A");
    }
}
