package com.pos.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockMovement {
    private int id;
    private Ingredient ingredient;
    private User user;
    private Integer orderId;
    private MovementType type;
    private BigDecimal quantity;
    private String note;
    private LocalDateTime createdAt;

    public StockMovement() {}
    public StockMovement(int id, Ingredient ingredient, User user, Integer orderId,
                         MovementType type, BigDecimal quantity, String note, LocalDateTime createdAt) {
        this.id = id; this.ingredient = ingredient; this.user = user; this.orderId = orderId;
        this.type = type; this.quantity = quantity; this.note = note; this.createdAt = createdAt;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
