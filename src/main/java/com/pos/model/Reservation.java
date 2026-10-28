package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Reservation {
    private int id;
    private int tableId;
    private String tableName; // joined
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private LocalDateTime reservationTime;
    private int guestCount;
    private BigDecimal depositAmount;
    private ReservationStatus status;
    private String channel; // HOTLINE, ZALO, FACEBOOK, DIRECT, WEBSITE
    private String note;
    private LocalDateTime createdAt;

    public Reservation() {
        this.depositAmount = BigDecimal.ZERO;
        this.guestCount = 2;
        this.status = ReservationStatus.PENDING;
        this.channel = "DIRECT";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public LocalDateTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalDateTime reservationTime) { this.reservationTime = reservationTime; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public String getChannel() { return channel != null ? channel : "DIRECT"; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getChannelDisplayName() {
        if (channel == null) return "Trực tiếp";
        return switch (channel.toUpperCase()) {
            case "HOTLINE" -> "📞 Hotline";
            case "ZALO" -> "💬 Zalo";
            case "FACEBOOK" -> "🌐 Facebook / MXH";
            case "WEBSITE" -> "💻 Website";
            default -> "🏬 Trực tiếp";
        };
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return customerName + " - " + (tableName != null ? tableName : "Bàn #" + tableId)
                + " @ " + reservationTime + " (" + getChannelDisplayName() + ")";
    }
}
