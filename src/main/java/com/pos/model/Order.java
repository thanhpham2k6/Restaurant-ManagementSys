package com.pos.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private DiningTable table;
    private User cashier;
    private OrderStatus status;
    private DiscountType discountType;
    private BigDecimal discountAmount;
    private BigDecimal vatRate;
    private BigDecimal totalAmount;
    private PaymentMethodType paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private List<OrderDetail> details = new ArrayList<>();

    public Order() {}
    public Order(int id, DiningTable table, User cashier, OrderStatus status,
                 DiscountType discountType, BigDecimal discountAmount, BigDecimal vatRate,
                 BigDecimal totalAmount, PaymentMethodType paymentMethod,
                 LocalDateTime createdAt, LocalDateTime paidAt) {
        this.id = id; this.table = table; this.cashier = cashier; this.status = status;
        this.discountType = discountType; this.discountAmount = discountAmount;
        this.vatRate = vatRate; this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod; this.createdAt = createdAt; this.paidAt = paidAt;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public DiningTable getTable() { return table; }
    public void setTable(DiningTable table) { this.table = table; }
    public User getCashier() { return cashier; }
    public void setCashier(User cashier) { this.cashier = cashier; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public PaymentMethodType getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethodType paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public List<OrderDetail> getDetails() { return details; }
    public void setDetails(List<OrderDetail> details) { this.details = details; }
    public void addDetail(OrderDetail d) { this.details.add(d); }
}
