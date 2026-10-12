package com.pos.model;

public class OrderEvent {
    public enum Type { ITEM_SENT, ITEM_ACCEPTED, ITEM_COOKING, ITEM_DONE, ITEM_REJECTED, ORDER_CANCELLED, ORDER_PAID }

    private final Type type;
    private final int orderId;
    private final int detailId;
    private final String message;

    public OrderEvent(Type type, int orderId, int detailId, String message) {
        this.type = type; this.orderId = orderId; this.detailId = detailId; this.message = message;
    }
    public Type getType() { return type; }
    public int getOrderId() { return orderId; }
    public int getDetailId() { return detailId; }
    public String getMessage() { return message; }
}
