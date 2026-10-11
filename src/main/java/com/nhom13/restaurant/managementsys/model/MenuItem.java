package com.nhom13.restaurant.managementsys.model;

public class MenuItem {
    private int itemId;
    private int categoryId;
    private String itemName;
    private double price;
    private boolean isAvailable;
    private int availableQuantity; // Số suất có thể phục vụ hôm nay (nhà bếp cập nhật)

    public MenuItem() {}

    public MenuItem(int itemId, int categoryId, String itemName, double price, boolean isAvailable, int availableQuantity) {
        this.itemId = itemId;
        this.categoryId = categoryId;
        this.itemName = itemName;
        this.price = price;
        this.isAvailable = isAvailable;
        this.availableQuantity = availableQuantity;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    @Override
    public String toString() { return itemName; }
}
