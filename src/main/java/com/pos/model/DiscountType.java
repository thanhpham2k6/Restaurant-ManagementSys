package com.pos.model;

public enum DiscountType {
    PERCENT("Phần trăm"),
    FIXED("Cố định"),
    NONE("Không"),
    VIP("VIP"),
    HOLIDAY("Ngày lễ");

    private final String displayName;
    DiscountType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
