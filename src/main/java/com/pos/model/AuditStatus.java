package com.pos.model;

public enum AuditStatus {
    IN_PROGRESS("Đang kiểm kho"),
    COMPLETED("Đã hoàn thành");

    private final String displayName;
    AuditStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
