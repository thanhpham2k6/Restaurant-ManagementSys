package com.pos.model;

public enum OrderStatus {
    OPEN("Đang phục vụ"),
    PENDING("Chờ xử lý"),
    IN_PROGRESS("Đang chế biến"),
    READY("Sẵn sàng"),
    COMPLETED("Hoàn thành"),
    PAID("Đã thanh toán"),
    CANCELLED("Đã hủy");

    private final String displayName;
    OrderStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
