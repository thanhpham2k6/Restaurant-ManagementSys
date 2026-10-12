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
        DiningTable table = new DiningTable();
        table.setId(rs.getInt("table_id"));
        User cashier = new User();
        cashier.setId(rs.getInt("user_id"));
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setTable(table);
        o.setCashier(cashier);
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setDiscountType(DiscountType.valueOf(rs.getString("discount_type")));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        o.setVatRate(rs.getBigDecimal("vat_rate"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        String pm = rs.getString("payment_method");
        if (pm != null) o.setPaymentMethod(PaymentMethodType.valueOf(pm));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());
        Timestamp pa = rs.getTimestamp("paid_at");
        if (pa != null) o.setPaidAt(pa.toLocalDateTime());
        return o;
    }

    public int insert(Connection conn, Order order) throws SQLException {
        String sql = "INSERT INTO orders (table_id, user_id, status, discount_type, discount_amount, vat_rate, total_amount) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getTable().getId());
            ps.setInt(2, order.getCashier().getId());
            ps.setString(3, OrderStatus.OPEN.name());
            ps.setString(4, DiscountType.NONE.name());
            ps.setBigDecimal(5, BigDecimal.ZERO);
            ps.setBigDecimal(6, new BigDecimal("0.10"));
            ps.setBigDecimal(7, BigDecimal.ZERO);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public Order findById(int id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
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
        String sql = "SELECT * FROM orders WHERE table_id = ? AND status = 'OPEN'";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { log.error("findOpenByTable error", e); }
        return null;
    }

    public void addDetail(Connection conn, OrderDetail detail) throws SQLException {
        String sql = "INSERT INTO order_details (order_id, menu_item_id, status, quantity, unit_price, note, sent_to_kitchen) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getOrderId());
            ps.setInt(2, detail.getItem().getId());
            ps.setString(3, ItemStatus.PENDING.name());
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
        String sql = "SELECT od.*, m.name as item_name, m.price as item_price, m.is_available, m.version as item_version, " +
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
                        rs.getBigDecimal("item_price"), rs.getBoolean("is_available"), rs.getInt("item_version"));
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
        String sql = "SELECT od.*, m.name as item_name, m.price as item_price, m.is_available, m.version as item_version, " +
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
                        rs.getBigDecimal("item_price"), rs.getBoolean("is_available"), rs.getInt("item_version"));
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

    public int countUnfinishedDetails(int orderId) {
        String sql = "SELECT COUNT(*) FROM order_details WHERE order_id = ? AND status NOT IN ('DONE','REJECTED')";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { log.error("countUnfinished error", e); }
        return Integer.MAX_VALUE;
    }

    public boolean canCancel(int orderId) {
        String sql = "SELECT COUNT(*) FROM order_details WHERE order_id = ? AND status IN ('COOKING','DONE')";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) { log.error("canCancel error", e); }
        return false;
    }

    public void pay(Connection conn, Order order) throws SQLException {
        String sql = "UPDATE orders SET status='PAID', discount_type=?, discount_amount=?, vat_rate=?, " +
                     "total_amount=?, payment_method=?, paid_at=GETDATE() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getDiscountType().name());
            ps.setBigDecimal(2, order.getDiscountAmount());
            ps.setBigDecimal(3, order.getVatRate());
            ps.setBigDecimal(4, order.getTotalAmount());
            ps.setString(5, order.getPaymentMethod().name());
            ps.setInt(6, order.getId());
            ps.executeUpdate();
        }
    }

    public void cancelOrder(Connection conn, int orderId) throws SQLException {
        String sql = "UPDATE orders SET status='CANCELLED' WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
        String sqlD = "UPDATE order_details SET status='REJECTED' WHERE order_id=? AND status IN ('PENDING')";
        try (PreparedStatement ps = conn.prepareStatement(sqlD)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    /** Doanh thu 7 ngày gần nhất: Map&lt;ngày, tổng&gt; */
    public Map<String, BigDecimal> revenueLast7Days() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        String sql = "SELECT CONVERT(date, paid_at) as day, SUM(total_amount) as total " +
                     "FROM orders WHERE status='PAID' AND paid_at >= DATEADD(day,-6,CAST(GETDATE() AS date)) " +
                     "GROUP BY CONVERT(date, paid_at) ORDER BY day";
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
        String sql = "SELECT * FROM orders WHERE status='PAID' AND paid_at BETWEEN ? AND ? ORDER BY paid_at DESC";
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
}
