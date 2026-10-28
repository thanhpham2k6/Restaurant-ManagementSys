package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Customer {
    private int id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private int loyaltyPoints;
    private BigDecimal totalSpent;
    private MembershipTier membershipTier;
    private LocalDateTime createdAt;

    public Customer() {
        this.totalSpent = BigDecimal.ZERO;
        this.membershipTier = MembershipTier.STANDARD;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public MembershipTier getMembershipTier() { return membershipTier; }
    public void setMembershipTier(MembershipTier membershipTier) { this.membershipTier = membershipTier; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Mỗi 1000đ chi tiêu = 1 điểm, mỗi 100 điểm = 10.000đ giảm giá */
    public BigDecimal getPointsValue() {
        return new BigDecimal(loyaltyPoints).multiply(new BigDecimal("100"));
    }

    @Override
    public String toString() {
        return name + " (" + phone + ") - " + membershipTier.getDisplayName();
    }
}
