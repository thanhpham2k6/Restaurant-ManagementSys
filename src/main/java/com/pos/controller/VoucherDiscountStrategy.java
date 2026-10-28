package com.pos.controller;

import com.pos.model.Voucher;
import java.math.BigDecimal;

/**
 * Chiến lược giảm giá dựa trên mã Voucher động.
 * Áp dụng Strategy Pattern.
 */
public class VoucherDiscountStrategy implements DiscountStrategy {
    private final Voucher voucher;

    public VoucherDiscountStrategy(Voucher voucher) {
        this.voucher = voucher;
    }

    @Override
    public BigDecimal apply(BigDecimal subtotal) {
        if (voucher == null || subtotal == null) return BigDecimal.ZERO;
        return voucher.computeDiscount(subtotal);
    }

    @Override
    public String getDisplayName() {
        if (voucher == null) return "Voucher";
        return "Voucher [" + voucher.getCode() + "]";
    }

    public Voucher getVoucher() {
        return voucher;
    }
}
