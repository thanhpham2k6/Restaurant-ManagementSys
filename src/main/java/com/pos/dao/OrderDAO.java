package com.pos.dao;

import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OrderDAO {
    private static final Logger log = LogManager.getLogger(OrderDAO.class);

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));

        int shiftId = rs.getInt("shift_id");
        if (!rs.wasNull()) o.setShiftId(shiftId);

        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            User cashier = new User();
            cashier.setId(userId);
            try { cashier.setFullName(rs.getString("cashier_name")); } catch (SQLException ignored) {}
            o.setCashier(cashier);
        }

        int tableId = rs.getInt("table_id");
        if (!rs.wasNull()) {
            o.setTableId(tableId);
            DiningTable table = new DiningTable();
            table.setId(tableId);
            try { table.setName(rs.getString("table_name")); } catch (SQLException ignored) {}
            o.setTable(table);
        }

        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) {
            o.setCustomerId(custId);
            try { o.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignored) {}
        }

        int voucherId = rs.getInt("voucher_id");
        if (!rs.wasNull()) {
            o.setVoucherId(voucherId);
            try { o.setVoucherCode(rs.getString("voucher_code")); } catch (SQLException ignored) {}
        }

        String ot = rs.getString("order_type");
        o.setOrderType(ot != null ? OrderType.valueOf(ot) : OrderType.DINE_IN);

        String st = rs.getString("status");
        o.setStatus(st != null ? OrderStatus.valueOf(st) : OrderStatus.OPEN);

        String pm = rs.getString("payment_method");
        if (pm != null) {
            try { o.setPaymentMethod(PaymentMethodType.valueOf(pm)); } catch (IllegalArgumentException ignored) {}
        }

        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        o.setDeliveryFee(rs.getBigDecimal("delivery_fee"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setPointsEarned(rs.getInt("points_earned"));
        o.setPointsUsed(rs.getInt("points_used"));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setDeliveryPhone(rs.getString("delivery_phone"));
        o.setShipperName(rs.getString("shipper_name"));
        o.setNote(rs.getString("note"));

        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());

        Timestamp coa = rs.getTimestamp("completed_at");
        if (coa != null) o.setCompletedAt(coa.toLocalDateTime());

        return o;
    }

    private static final String BASE_SELECT =
        "SELECT o.*, t.name as table_name, u.full_name as cashier_name, " +
        "c.name as customer_name, v.code as voucher_code " +
        "FROM orders o " +
        "LEFT JOIN dining_tables t ON o.table_id = t.id " +
        "LEFT JOIN users u ON o.user_id = u.id " +
        "LEFT JOIN customers c ON o.customer_id = c.id " +
        "LEFT JOIN vouchers v ON o.voucher_id = v.id ";

    public int insert(Connection conn, Order order) throws SQLException {
        String sql = "INSERT INTO orders (shift_id, user_id, table_id, customer_id, voucher_id, order_type, " +
                     "status, subtotal, discount_amount, delivery_fee, total_amount, points_earned, points_used, " +
                     "delivery_address, delivery_phone, shipper_name, note) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (order.getShiftId() != null) ps.setInt(1, order.getShiftId()); else ps.setNull(1, Types.INTEGER);
            if (order.getCashier() != null && order.getCashier().getId() > 0) ps.setInt(2, order.getCashier().getId()); else ps.setNull(2, Types.INTEGER);
            if (order.getTableId() != null) ps.setInt(3, order.getTableId());
            else if (order.getTable() != null) ps.setInt(3, order.getTable().getId());
            else ps.setNull(3, Types.INTEGER);

            if (order.getCustomerId() != null) ps.setInt(4, order.getCustomerId()); else ps.setNull(4, Types.INTEGER);
            if (order.getVoucherId() != null) ps.setInt(5, order.getVoucherId()); else ps.setNull(5, Types.INTEGER);

            ps.setString(6, order.getOrderType() != null ? order.getOrderType().name() : OrderType.DINE_IN.name());
            ps.setString(7, order.getStatus() != null ? order.getStatus().name() : OrderStatus.OPEN.name());
            ps.setBigDecimal(8, order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO);
            ps.setBigDecimal(9, order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
            ps.setBigDecimal(10, order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO);
            ps.setBigDecimal(11, order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            ps.setInt(12, order.getPointsEarned());
            ps.setInt(13, order.getPointsUsed());
            ps.setString(14, order.getDeliveryAddress());
            ps.setString(15, order.getDeliveryPhone());
            ps.setString(16, order.getShipperName());
            ps.setString(17, order.getNote());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    order.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public Order findById(int id) {
        String sql = BASE_SELECT + "WHERE o.id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findById order error", e); }
        return null;
    }

    public Order findOpenByTable(int tableId) {
        String sql = BASE_SELECT + "WHERE o.table_id = ? AND o.status IN ('OPEN', 'PENDING', 'IN_PROGRESS')";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findOpenByTable error", e); }
        return null;
    }

    public List<Order> findActiveTakeawayOrders() {
        List<Order> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE o.order_type IN ('TAKEAWAY', 'DELIVERY') AND o.status IN ('OPEN', 'PENDING', 'IN_PROGRESS', 'READY') ORDER BY o.id DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.error("findActiveTakeawayOrders error", e); }
        return list;
    }

    public void addDetail(Connection conn, OrderDetail detail) throws SQLException {
        String sql = "INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getOrderId());
            ps.setInt(2, detail.getItem().getId());
            ps.setString(3, detail.getStatus() != null ? detail.getStatus().name() : ItemStatus.PENDING.name());
            ps.setInt(4, detail.getQuantity());
            ps.setBigDecimal(5, detail.getUnitPrice());
            ps.setString(6, detail.getNote());
            ps.setBoolean(7, detail.isSentToKitchen());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) detail.setId(keys.getInt(1));
            }
        }
    }

    public List<OrderDetail> findDetailsByOrder(int orderId) {
        List<OrderDetail> list = new ArrayList<>();
        String sql = "SELECT od.*, m.name as item_name, m.price as item_price, m.image_path, m.is_available, m.version as item_version, " +
                     "c.id as cat_id, c.name as cat_name, c.type as cat_type " +
                     "FROM order_details od JOIN menu_items m ON od.menu_item_id = m.id " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "WHERE od.order_id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category cat = new Category(rs.getInt("cat_id"), rs.getString("cat_name"),
                        CategoryType.valueOf(rs.getString("cat_type")));
                    MenuItem item = new MenuItem(rs.getInt("menu_item_id"), cat, rs.getString("item_name"),
                        rs.getBigDecimal("item_price"), rs.getBoolean("is_available"), rs.getInt("item_version"),
                        rs.getString("image_path"));
                    OrderDetail d = new OrderDetail(
                        rs.getInt("id"), orderId, item,
                        ItemStatus.valueOf(rs.getString("status")),
                        rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                        rs.getString("note"),
                        rs.getBoolean("sent_to_kitchen")
                    );
                    list.add(d);
                }
            }
        } catch (SQLException e) { log.error("findDetailsByOrder error", e); }
        return list;
    }

    public void updateDetailStatus(Connection conn, int detailId, ItemStatus status) throws SQLException {
        String sql = "UPDATE order_details SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, detailId);
            ps.executeUpdate();
        }
    }

    public void updateDetailStatus(int detailId, ItemStatus status) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            updateDetailStatus(c, detailId, status);
        } catch (SQLException e) { log.error("updateDetailStatus error", e); }
    }

    public OrderDetail findDetailById(int detailId) {
        String sql = "SELECT od.*, m.name as item_name, m.price as item_price, m.image_path, m.is_available, m.version as item_version, " +
                     "c.id as cat_id, c.name as cat_name, c.type as cat_type " +
                     "FROM order_details od JOIN menu_items m ON od.menu_item_id = m.id " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "WHERE od.id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, detailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category cat = new Category(rs.getInt("cat_id"), rs.getString("cat_name"),
                        CategoryType.valueOf(rs.getString("cat_type")));
                    MenuItem item = new MenuItem(rs.getInt("menu_item_id"), cat, rs.getString("item_name"),
                        rs.getBigDecimal("item_price"), rs.getBoolean("is_available"), rs.getInt("item_version"),
                        rs.getString("image_path"));
                    return new OrderDetail(
                        rs.getInt("id"), rs.getInt("order_id"), item,
                        ItemStatus.valueOf(rs.getString("status")),
                        rs.getInt("quantity"), rs.getBigDecimal("unit_price"), rs.getString("note"),
                        rs.getBoolean("sent_to_kitchen")
                    );
                }
            }
        } catch (SQLException e) { log.error("findDetailById error", e); }
        return null;
    }

    public void sendToKitchen(int orderId) {
        String sql = "UPDATE order_details SET sent_to_kitchen = 1 WHERE order_id = ? AND sent_to_kitchen = 0";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("sendToKitchen error", e); }
    }

    public void updateOrderFinancials(Connection conn, Order order) throws SQLException {
        String sql = "UPDATE orders SET customer_id=?, voucher_id=?, subtotal=?, discount_amount=?, " +
                     "delivery_fee=?, total_amount=?, points_earned=?, points_used=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (order.getCustomerId() != null) ps.setInt(1, order.getCustomerId()); else ps.setNull(1, Types.INTEGER);
            if (order.getVoucherId() != null) ps.setInt(2, order.getVoucherId()); else ps.setNull(2, Types.INTEGER);
            ps.setBigDecimal(3, order.getSubtotal());
            ps.setBigDecimal(4, order.getDiscountAmount());
            ps.setBigDecimal(5, order.getDeliveryFee());
            ps.setBigDecimal(6, order.getTotalAmount());
            ps.setInt(7, order.getPointsEarned());
            ps.setInt(8, order.getPointsUsed());
            ps.setInt(9, order.getId());
            ps.executeUpdate();
        }
    }

    public void pay(Connection conn, Order order) throws SQLException {
        String sql = "UPDATE orders SET status='PAID', payment_method=?, subtotal=?, discount_amount=?, " +
                     "delivery_fee=?, total_amount=?, points_earned=?, points_used=?, customer_id=?, " +
                     "voucher_id=?, completed_at=GETDATE() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getPaymentMethod() != null ? order.getPaymentMethod().name() : PaymentMethodType.CASH.name());
            ps.setBigDecimal(2, order.getSubtotal());
            ps.setBigDecimal(3, order.getDiscountAmount());
            ps.setBigDecimal(4, order.getDeliveryFee());
            ps.setBigDecimal(5, order.getTotalAmount());
            ps.setInt(6, order.getPointsEarned());
            ps.setInt(7, order.getPointsUsed());
            if (order.getCustomerId() != null) ps.setInt(8, order.getCustomerId()); else ps.setNull(8, Types.INTEGER);
            if (order.getVoucherId() != null) ps.setInt(9, order.getVoucherId()); else ps.setNull(9, Types.INTEGER);
            ps.setInt(10, order.getId());
            ps.executeUpdate();
        }
    }

    public void cancelOrder(Connection conn, int orderId) throws SQLException {
        String sql = "UPDATE orders SET status='CANCELLED', completed_at=GETDATE() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
        String sqlD = "UPDATE order_details SET status='REJECTED' WHERE order_id=? AND status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sqlD)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    public Map<String, BigDecimal> revenueLast7Days() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        String sql = "SELECT CONVERT(date, completed_at) as day, SUM(total_amount) as total " +
                     "FROM orders WHERE status='PAID' AND completed_at >= DATEADD(day,-6,CAST(GETDATE() AS date)) " +
                     "GROUP BY CONVERT(date, completed_at) ORDER BY day";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("day"), rs.getBigDecimal("total"));
            }
        } catch (SQLException e) { log.error("revenueLast7Days error", e); }
        return map;
    }

    public List<Order> findPaidOrders(LocalDateTime from, LocalDateTime to) {
        List<Order> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE o.status='PAID' AND o.completed_at BETWEEN ? AND ? ORDER BY o.completed_at DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { log.error("findPaidOrders error", e); }
        return list;
    }

    public void updateDetailQuantity(int detailId, int quantity) {
        String sql = "UPDATE order_details SET quantity = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, detailId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateDetailQuantity error", e); }
    }

    public void deleteDetail(int detailId) {
        String sql = "DELETE FROM order_details WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, detailId);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("deleteDetail error", e); }
    }
}
