package com.pos.model;

public class Category {
    private int id;
    private String name;
    private CategoryType type;

    public Category() {}
    public Category(int id, String name, CategoryType type) {
        this.id = id; this.name = name; this.type = type;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }
    @Override public String toString() { return name; }
}
