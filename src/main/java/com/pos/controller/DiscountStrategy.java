package com.pos.controller;

import java.math.BigDecimal;

/**
 * Strategy interface cho các chiến lược giảm giá.
 * Áp dụng Strategy Pattern.
 */
public interface DiscountStrategy {
    /** Tính số tiền được giảm từ tổng tiền hàng (chưa VAT). */
    BigDecimal apply(BigDecimal subtotal);
    String getDisplayName();
}
