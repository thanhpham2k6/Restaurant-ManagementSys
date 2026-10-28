package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Voucher {
    private int id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountVal;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private int usageLimit;
    private int usedCount;
    private boolean active;

    public Voucher() {
        this.minOrderAmount = BigDecimal.ZERO;
        this.usageLimit = 1;
    }

    public boolean isValid(BigDecimal orderAmount) {
        if (!active) return false;
        if (usedCount >= usageLimit) return false;
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate) || today.isAfter(endDate)) return false;
        return orderAmount.compareTo(minOrderAmount) >= 0;
    }

    public BigDecimal computeDiscount(BigDecimal orderAmount) {
        if (!isValid(orderAmount)) return BigDecimal.ZERO;
        BigDecimal discount;
        if (discountType == DiscountType.PERCENT) {
            discount = orderAmount.multiply(discountVal).divide(new BigDecimal("100"));
        } else {
            discount = discountVal;
        }
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }
        return discount.min(orderAmount);
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountVal() { return discountVal; }
    public void setDiscountVal(BigDecimal discountVal) { this.discountVal = discountVal; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return code + " - " + (discountType == DiscountType.PERCENT
                ? discountVal + "%" : discountVal + "đ");
    }
}
