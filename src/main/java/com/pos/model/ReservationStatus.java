package com.pos.model;

public enum ReservationStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SEATED("Đã vào bàn"),
    CANCELLED("Đã hủy");

    private final String displayName;
    ReservationStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
