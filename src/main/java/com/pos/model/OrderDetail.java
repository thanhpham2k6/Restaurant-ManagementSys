package com.pos.model;
import java.math.BigDecimal;

public class OrderDetail {
    private int id;
    private int orderId;
    private MenuItem item;
    private ItemStatus status;
    private int quantity;
    private BigDecimal unitPrice;
    private String note;
    private boolean sentToKitchen;

    public OrderDetail() {}
    public OrderDetail(int id, int orderId, MenuItem item, ItemStatus status, int quantity, BigDecimal unitPrice, String note) {
        this(id, orderId, item, status, quantity, unitPrice, note, false);
    }
    public OrderDetail(int id, int orderId, MenuItem item, ItemStatus status, int quantity, BigDecimal unitPrice, String note, boolean sentToKitchen) {
        this.id = id; this.orderId = orderId; this.item = item; this.status = status;
        this.quantity = quantity; this.unitPrice = unitPrice; this.note = note;
        this.sentToKitchen = sentToKitchen;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public MenuItem getItem() { return item; }
    public void setItem(MenuItem item) { this.item = item; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isSentToKitchen() { return sentToKitchen; }
    public void setSentToKitchen(boolean sentToKitchen) { this.sentToKitchen = sentToKitchen; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
