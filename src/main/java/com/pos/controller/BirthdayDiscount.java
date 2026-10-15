package com.pos.controller;
import java.math.BigDecimal;

public class BirthdayDiscount implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.20"); // Giảm 20%
    @Override public BigDecimal apply(BigDecimal subtotal) { return subtotal.multiply(RATE); }
    @Override public String getDisplayName() { return "Sinh nhật (giảm 20%)"; }
    @Override public String toString() { return getDisplayName(); }
}