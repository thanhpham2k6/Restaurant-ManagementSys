package com.pos.model;

public enum MovementType {
    IMPORT("Nhập kho"),
    EXPORT("Xuất kho"),
    ADJUST("Điều chỉnh"),
    RETURN("Trả hàng"),
    ADJUST_AUDIT("Điều chỉnh kiểm kho");

    private final String displayName;
    MovementType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
