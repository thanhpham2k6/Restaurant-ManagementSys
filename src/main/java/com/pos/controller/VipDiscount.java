package com.pos.controller;

import java.math.BigDecimal;

/** Giảm giá VIP: 10% trên tổng tiền hàng. */
public class VipDiscount implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.10");
    @Override public BigDecimal apply(BigDecimal subtotal) { return subtotal.multiply(RATE); }
    @Override public String getDisplayName() { return "VIP (giảm 10%)"; }
    @Override public String toString() { return getDisplayName(); }
}
