package com.pos.model;
import java.math.BigDecimal;

public class Ingredient {
    private int id;
    private String name;
    private String unit;
    private BigDecimal stockQty;
    private BigDecimal minQty;
    private boolean active;

    public Ingredient() {}
    public Ingredient(int id, String name, String unit, BigDecimal stockQty, BigDecimal minQty, boolean active) {
        this.id = id; this.name = name; this.unit = unit;
        this.stockQty = stockQty; this.minQty = minQty; this.active = active;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getStockQty() { return stockQty; }
    public void setStockQty(BigDecimal stockQty) { this.stockQty = stockQty; }
    public BigDecimal getMinQty() { return minQty; }
    public void setMinQty(BigDecimal minQty) { this.minQty = minQty; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isLowStock() { return stockQty.compareTo(minQty) <= 0; }
    @Override public String toString() { return name + " (" + unit + ")"; }
}
