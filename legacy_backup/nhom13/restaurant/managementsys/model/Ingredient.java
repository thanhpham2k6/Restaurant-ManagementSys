package com.nhom13.restaurant.managementsys.model;

public class Ingredient {
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private double quantity;
    private double minQuantity;

    public Ingredient() {}

    public Ingredient(int ingredientId, String ingredientName, String unit, double quantity, double minQuantity) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.unit = unit;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
    }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getMinQuantity() { return minQuantity; }
    public void setMinQuantity(double minQuantity) { this.minQuantity = minQuantity; }

    /** Cảnh báo nếu sắp hết nguyên liệu */
    public boolean isLowStock() { return quantity <= minQuantity; }

    @Override
    public String toString() { return ingredientName + " (" + unit + ")"; }
}
