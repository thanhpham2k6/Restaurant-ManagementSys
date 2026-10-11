package com.nhom13.restaurant.managementsys.model;

import java.sql.Timestamp;

public class Order {
    private int orderId;
    private int tableId;
    private int userId;
    private double totalAmount;
    private String status;
    private Timestamp orderTime;

    public Order() {}

    public Order(int orderId, int tableId, int userId, double totalAmount, String status, Timestamp orderTime) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderTime = orderTime;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getOrderTime() { return orderTime; }
    public void setOrderTime(Timestamp orderTime) { this.orderTime = orderTime; }
}
