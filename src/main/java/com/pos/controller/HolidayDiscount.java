package com.pos.controller;

import java.math.BigDecimal;

/** Giảm giá ngày lễ: 5% trên tổng tiền hàng. */
public class HolidayDiscount implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.05");
    @Override public BigDecimal apply(BigDecimal subtotal) { return subtotal.multiply(RATE); }
    @Override public String getDisplayName() { return "Ngày lễ (giảm 5%)"; }
    @Override public String toString() { return getDisplayName(); }
}
