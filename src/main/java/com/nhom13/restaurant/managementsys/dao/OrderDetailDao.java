package com.nhom13.restaurant.managementsys.dao;

import com.nhom13.restaurant.managementsys.database.Database;
import com.nhom13.restaurant.managementsys.model.OrderDetail;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailDao {
    public void addOrderDetail(OrderDetail detail) {
        String sql = "INSERT INTO OrderDetails (order_id, item_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, detail.getOrderId());
            pstmt.setInt(2, detail.getItemId());
            pstmt.setInt(3, detail.getQuantity());
            pstmt.setDouble(4, detail.getSubtotal());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public List<OrderDetail> getOrderDetailsByOrderId(int orderId) {
        List<OrderDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM OrderDetails WHERE order_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(new OrderDetail(rs.getInt("order_detail_id"), rs.getInt("order_id"), rs.getInt("item_id"),
                            rs.getInt("quantity"), rs.getDouble("subtotal")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return details;
    }

    /** Xóa 1 dòng chi tiết đơn hàng theo order_detail_id */
    public void deleteOrderDetail(int detailId) {
        String sql = "DELETE FROM OrderDetails WHERE order_detail_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, detailId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}

