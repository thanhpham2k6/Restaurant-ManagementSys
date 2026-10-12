package com.nhom13.restaurant.managementsys.model;

public class DiningTable {
    private int tableId;
    private String tableName;
    private String status;

    public DiningTable() {}

    public DiningTable(int tableId, String tableName, String status) {
        this.tableId = tableId;
        this.tableName = tableName;
        this.status = status;
    }

    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() { return tableName + " - " + status; }
}
