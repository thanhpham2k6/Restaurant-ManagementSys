package com.pos.model;

import java.math.BigDecimal;

public class TheoreticalUsage {
    private int itemId;
    private String itemName;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal theoreticalUsedQty;
    private BigDecimal estimatedRemaining;
    private int dishesSoldCount;

    public TheoreticalUsage(int itemId, String itemName, String unit, BigDecimal currentStock, BigDecimal theoreticalUsedQty, BigDecimal estimatedRemaining, int dishesSoldCount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unit = unit;
        this.currentStock = currentStock;
        this.theoreticalUsedQty = theoreticalUsedQty;
        this.estimatedRemaining = estimatedRemaining;
        this.dishesSoldCount = dishesSoldCount;
    }

    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getUnit() { return unit; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public BigDecimal getTheoreticalUsedQty() { return theoreticalUsedQty; }
    public BigDecimal getEstimatedRemaining() { return estimatedRemaining; }
    public int getDishesSoldCount() { return dishesSoldCount; }
}
