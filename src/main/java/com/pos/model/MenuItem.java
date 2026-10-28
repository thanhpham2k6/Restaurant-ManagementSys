package com.pos.model;
import java.math.BigDecimal;

public class MenuItem {
    private int id;
    private Category category;
    private String name;
    private BigDecimal price;
    private boolean available;
    private int version;
    private String imagePath;

    public MenuItem() {}
    public MenuItem(int id, Category category, String name, BigDecimal price, boolean available, int version) {
        this.id = id; this.category = category; this.name = name;
        this.price = price; this.available = available; this.version = version;
    }
    public MenuItem(int id, Category category, String name, BigDecimal price, boolean available, int version, String imagePath) {
        this.id = id; this.category = category; this.name = name;
        this.price = price; this.available = available; this.version = version;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override public String toString() { return name; }
}
