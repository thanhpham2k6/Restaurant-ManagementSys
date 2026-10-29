package com.pos.model;

public enum MovementType {
    IMPORT("Nhập kho (Mua hàng/NCC)"),
    EXPORT("Xuất kho (Cấp cho Bếp)"),
    WASTE("Xuất hủy (Hao hụt/Hỏng)"),
    RETURN("Trả hàng Nhà cung cấp"),
    ADJUSTMENT("Cân bằng kiểm kê"),
    ADJUST("Điều chỉnh thủ công"),
    ADJUST_AUDIT("Cân bằng kiểm kê");

    private final String displayName;

    MovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MovementType fromString(String val) {
        if (val == null) return IMPORT;
        String upper = val.trim().toUpperCase();
        if (upper.equals("ADJUST_AUDIT") || upper.equals("ADJUSTMENT")) return ADJUSTMENT;
        if (upper.equals("WASTE")) return WASTE;
        if (upper.equals("RETURN")) return RETURN;
        if (upper.equals("EXPORT")) return EXPORT;
        if (upper.equals("ADJUST")) return ADJUST;
        if (upper.equals("IMPORT")) return IMPORT;
        try {
            return MovementType.valueOf(upper);
        } catch (Exception e) {
            return IMPORT;
        }
    }
}
