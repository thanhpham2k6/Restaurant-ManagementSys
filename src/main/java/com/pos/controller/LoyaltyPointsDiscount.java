package com.pos.controller;

import java.math.BigDecimal;

/**
 * Chiến lược giảm giá bằng điểm tích lũy thành viên.
 * Mỗi 100 điểm = 10,000 VND (1 điểm = 100 VND).
 */
public class LoyaltyPointsDiscount implements DiscountStrategy {
    private final int pointsToUse;

    public LoyaltyPointsDiscount(int pointsToUse) {
        this.pointsToUse = Math.max(0, pointsToUse);
    }

    @Override
    public BigDecimal apply(BigDecimal subtotal) {
        if (subtotal == null) return BigDecimal.ZERO;
        BigDecimal pointVal = new BigDecimal(pointsToUse).multiply(new BigDecimal("100"));
        return pointVal.min(subtotal);
    }

    @Override
    public String getDisplayName() {
        return "Điểm tích lũy (" + pointsToUse + " điểm)";
    }

    public int getPointsToUse() {
        return pointsToUse;
    }
}
