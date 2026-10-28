package com.pos.model;

import java.math.BigDecimal;

public class InventoryAuditDetail {
    private int id;
    private int auditId;
    private int itemId;
    private String itemName; // joined
    private String itemUnit;  // joined
    private BigDecimal systemQty;
    private BigDecimal actualQty;
    private BigDecimal differenceQty;
    private String reason;

    public InventoryAuditDetail() {
        this.systemQty = BigDecimal.ZERO;
        this.actualQty = BigDecimal.ZERO;
        this.differenceQty = BigDecimal.ZERO;
    }

    public BigDecimal computeDifference() {
        return actualQty.subtract(systemQty);
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuditId() { return auditId; }
    public void setAuditId(int auditId) { this.auditId = auditId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemUnit() { return itemUnit; }
    public void setItemUnit(String itemUnit) { this.itemUnit = itemUnit; }

    public BigDecimal getSystemQty() { return systemQty; }
    public void setSystemQty(BigDecimal systemQty) { this.systemQty = systemQty; }

    public BigDecimal getActualQty() { return actualQty; }
    public void setActualQty(BigDecimal actualQty) {
        this.actualQty = actualQty;
        this.differenceQty = computeDifference();
    }

    public BigDecimal getDifferenceQty() { return differenceQty; }
    public void setDifferenceQty(BigDecimal differenceQty) { this.differenceQty = differenceQty; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
