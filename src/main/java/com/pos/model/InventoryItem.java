package com.pos.model;

import java.math.BigDecimal;

public class InventoryItem {
    private int id;
    private Integer supplierId;
    private String supplierName; // joined
    private String name;
    private String category;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal stockQty;
    private BigDecimal minQty;
    private boolean active;

    public InventoryItem() {
        this.costPrice = BigDecimal.ZERO;
        this.stockQty = BigDecimal.ZERO;
        this.minQty = BigDecimal.ZERO;
        this.active = true;
    }

    public boolean isLowStock() {
        return stockQty.compareTo(minQty) <= 0;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getStockQty() { return stockQty; }
    public void setStockQty(BigDecimal stockQty) { this.stockQty = stockQty; }

    public BigDecimal getMinQty() { return minQty; }
    public void setMinQty(BigDecimal minQty) { this.minQty = minQty; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name + " [" + stockQty + " " + unit + "]";
    }
}
