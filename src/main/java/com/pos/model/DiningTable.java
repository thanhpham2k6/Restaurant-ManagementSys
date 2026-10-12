package com.pos.model;

public class DiningTable {
    private int id;
    private String name;
    private int capacity;
    private TableStatus status;
    private int version;

    public DiningTable() {}
    public DiningTable(int id, String name, int capacity, TableStatus status, int version) {
        this.id = id; this.name = name; this.capacity = capacity; this.status = status; this.version = version;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    @Override public String toString() { return name + " (" + capacity + " người) - " + status; }
}
