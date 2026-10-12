package com.pos.controller;

import java.math.BigDecimal;

/** Không giảm giá. */
public class NoDiscount implements DiscountStrategy {
    @Override public BigDecimal apply(BigDecimal subtotal) { return BigDecimal.ZERO; }
    @Override public String getDisplayName() { return "Không giảm giá"; }
    @Override public String toString() { return getDisplayName(); }
}
