package com.pos.model;

public enum OrderType {
    DINE_IN("Tại bàn"),
    TAKEAWAY("Mang về"),
    DELIVERY("Giao hàng");

    private final String displayName;
    OrderType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
