package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    // Legacy fields (kept for backward compatibility)
    private DiningTable table;
    private User cashier;
    // New fields v3
    private Integer shiftId;
    private Integer tableId;
    private Integer customerId;
    private String customerName;   // joined
    private Integer voucherId;
    private String voucherCode;    // joined
    private OrderType orderType;
    private OrderStatus status;
    private PaymentMethodType paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private int pointsEarned;
    private int pointsUsed;
    private String deliveryAddress;
    private String deliveryPhone;
    private String shipperName;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<OrderDetail> details = new ArrayList<>();

    // Legacy compat
    private DiscountType discountType;
    private BigDecimal vatRate;
    private LocalDateTime paidAt;

    public Order() {
        this.orderType = OrderType.DINE_IN;
        this.status = OrderStatus.PENDING;
        this.subtotal = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.deliveryFee = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public BigDecimal computeTotal() {
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal fee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        return sub.subtract(disc).add(fee).max(BigDecimal.ZERO);
    }

    // ========= Getters & Setters =========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public DiningTable getTable() { return table; }
    public void setTable(DiningTable table) {
        this.table = table;
        if (table != null) this.tableId = table.getId();
    }
    public String getTableName() {
        if (table != null && table.getName() != null) return table.getName();
        if (tableId != null) return "Bàn #" + tableId;
        return orderType != null ? orderType.getDisplayName() : "N/A";
    }

    public User getCashier() { return cashier; }
    public void setCashier(User cashier) { this.cashier = cashier; }

    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }

    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Integer getVoucherId() { return voucherId; }
    public void setVoucherId(Integer voucherId) { this.voucherId = voucherId; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public PaymentMethodType getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethodType paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public int getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(int pointsUsed) { this.pointsUsed = pointsUsed; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }

    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        this.paidAt = completedAt;
    }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }

    public List<OrderDetail> getDetails() { return details; }
    public void setDetails(List<OrderDetail> details) { this.details = details; }
    public void addDetail(OrderDetail d) { this.details.add(d); }
}
