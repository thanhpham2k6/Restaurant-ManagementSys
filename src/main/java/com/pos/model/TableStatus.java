package com.pos.model;

public enum TableStatus {
    AVAILABLE("Trống"),
    EMPTY("Trống"),
    OCCUPIED("Có khách"),
    RESERVED("Đã đặt"),
    CLEANING("Dọn dẹp");

    private final String displayName;

    TableStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailable() {
        return this == AVAILABLE || this == EMPTY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
