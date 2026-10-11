package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.Order;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {
    public int createOrder(Order order) {
        String sql = "INSERT INTO Orders (table_id, user_id, total_amount, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getTableId());
            pstmt.setInt(2, order.getUserId());
            pstmt.setDouble(3, order.getTotalAmount());
            pstmt.setString(4, order.getStatus());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }
    
    public void updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE Orders SET status = ? WHERE order_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public void updateOrderTotal(int orderId, double totalAmount) {
        String sql = "UPDATE Orders SET total_amount = ? WHERE order_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, totalAmount);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public Order getPendingOrderByTableId(int tableId) {
        return getOrderByTableAndStatus(tableId, "Pending");
    }

    /** Lấy đơn hàng đang hoạt động của bàn (chưa thanh toán: Ordering, Pending, Cooking, Ready) */
    public Order getActiveOrderByTableId(int tableId) {
        String sql = "SELECT * FROM Orders WHERE table_id = ? AND status IN ('Ordering', 'Pending', 'Cooking', 'Ready') ORDER BY order_id DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tableId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Order(rs.getInt("order_id"), rs.getInt("table_id"), rs.getInt("user_id"),
                            rs.getDouble("total_amount"), rs.getString("status"), rs.getTimestamp("order_time"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Lấy trạng thái hiện tại của đơn hàng */
    public String getOrderStatus(int orderId) {
        String sql = "SELECT status FROM Orders WHERE order_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    /** Lấy đơn hàng theo order_id */
    public Order getOrderById(int orderId) {
        String sql = "SELECT * FROM Orders WHERE order_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Order(rs.getInt("order_id"), rs.getInt("table_id"), rs.getInt("user_id"),
                            rs.getDouble("total_amount"), rs.getString("status"), rs.getTimestamp("order_time"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Lấy đơn hàng theo bàn và trạng thái (Pending, Cooking, Ready...) */
    public Order getOrderByTableAndStatus(int tableId, String status) {
        String sql = "SELECT * FROM Orders WHERE table_id = ? AND status = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tableId);
            pstmt.setString(2, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Order(rs.getInt("order_id"), rs.getInt("table_id"), rs.getInt("user_id"),
                            rs.getDouble("total_amount"), rs.getString("status"), rs.getTimestamp("order_time"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}
