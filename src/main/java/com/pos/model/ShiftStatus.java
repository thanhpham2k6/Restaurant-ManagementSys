package com.pos.model;

public enum ShiftStatus {
    OPEN("Đang mở"),
    CLOSED("Đã đóng");

    private final String displayName;

    ShiftStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
