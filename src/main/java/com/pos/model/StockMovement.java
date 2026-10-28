package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockMovement {
    private int id;
    private int itemId;
    private String itemName; // joined
    private Integer supplierId;
    private String supplierName; // joined
    private int userId;
    private String userFullName; // joined
    private MovementType type;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String note;
    private LocalDateTime createdAt;
    private Integer orderId; // legacy compat

    public StockMovement() {
        this.quantity = BigDecimal.ZERO;
        this.unitPrice = BigDecimal.ZERO;
    }

    // Legacy compat methods
    public void setIngredient(Ingredient ing) {
        if (ing != null) {
            this.itemId = ing.getId();
            this.itemName = ing.getName();
        }
    }

    public Ingredient getIngredient() {
        Ingredient ing = new Ingredient();
        ing.setId(this.itemId);
        ing.setName(this.itemName);
        return ing;
    }

    public void setUser(User user) {
        if (user != null) {
            this.userId = user.getId();
            this.userFullName = user.getFullName();
        }
    }

    public User getUser() {
        User u = new User();
        u.setId(this.userId);
        u.setFullName(this.userFullName);
        return u;
    }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
