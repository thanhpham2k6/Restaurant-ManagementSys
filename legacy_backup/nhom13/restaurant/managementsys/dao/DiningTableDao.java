package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.DiningTable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiningTableDao {
    public List<DiningTable> getAllTables() {
        List<DiningTable> tables = new ArrayList<>();
        String sql = "SELECT * FROM DiningTables";
        try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(new DiningTable(rs.getInt("table_id"), rs.getString("table_name"), rs.getString("status")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return tables;
    }
    
    public void updateTableStatus(int tableId, String status) {
        String sql = "UPDATE DiningTables SET status = ? WHERE table_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, tableId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public void addTable(DiningTable table) {
        String sql = "INSERT INTO DiningTables (table_name, status) VALUES (?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, table.getTableName());
            pstmt.setString(2, table.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
