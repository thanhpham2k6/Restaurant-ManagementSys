package com.pos.model;
import java.math.BigDecimal;

public class RecipeLine {
    private int id;
    private MenuItem menuItem;
    private Ingredient ingredient;
    private BigDecimal qtyPerServing;

    public RecipeLine() {}
    public RecipeLine(int id, MenuItem menuItem, Ingredient ingredient, BigDecimal qtyPerServing) {
        this.id = id; this.menuItem = menuItem; this.ingredient = ingredient; this.qtyPerServing = qtyPerServing;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public BigDecimal getQtyPerServing() { return qtyPerServing; }
    public void setQtyPerServing(BigDecimal qtyPerServing) { this.qtyPerServing = qtyPerServing; }
}
