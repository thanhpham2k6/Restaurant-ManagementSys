package com.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryAudit {
    private int id;
    private String auditCode;
    private int userId;
    private String userFullName; // joined
    private LocalDateTime auditDate;
    private AuditStatus status;
    private String note;
    private List<InventoryAuditDetail> details;

    public InventoryAudit() {
        this.status = AuditStatus.IN_PROGRESS;
        this.details = new ArrayList<>();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAuditCode() { return auditCode; }
    public void setAuditCode(String auditCode) { this.auditCode = auditCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public LocalDateTime getAuditDate() { return auditDate; }
    public void setAuditDate(LocalDateTime auditDate) { this.auditDate = auditDate; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<InventoryAuditDetail> getDetails() { return details; }
    public void setDetails(List<InventoryAuditDetail> details) { this.details = details; }
}
