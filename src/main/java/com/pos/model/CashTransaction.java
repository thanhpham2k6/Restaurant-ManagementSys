package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashTransaction {
    private int id;
    private String code;
    private String type; // "RECEIPT" (Thu) or "PAYMENT" (Chi)
    private String category;
    private BigDecimal amount;
    private String paymentMethod; // "Tiền mặt" or "Chuyển khoản"
    private String personName;
    private String note;
    private int userId;
    private String userName;
    private LocalDateTime createdAt;

    public CashTransaction() {}

    public CashTransaction(int id, String code, String type, String category, BigDecimal amount,
                           String paymentMethod, String personName, String note, int userId,
                           String userName, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.personName = personName;
        this.note = note;
        this.userId = userId;
        this.userName = userName;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isReceipt() { return "RECEIPT".equalsIgnoreCase(type) || "THU".equalsIgnoreCase(type); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
