package com.pos.controller;

import com.pos.dao.OrderDAO;
import com.pos.dao.TableDAO;
import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class TableController {
    private static final Logger log = LogManager.getLogger(TableController.class);
    private final TableDAO tableDAO = new TableDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public List<DiningTable> getAllTables() {
        return tableDAO.findAll();
    }

    /**
     * Mở bàn với pessimistic locking (SELECT ... WITH (UPDLOCK)).
     * Cho phép mở bàn trống, bàn đặt trước (khách đến nhận bàn) hoặc bàn đã có khách.
     */
    public Order openTable(int tableId, User cashier) throws TableBusyException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                DiningTable table = tableDAO.lockForUpdate(conn, tableId);
                if (table == null) {
                    conn.rollback();
                    throw new RuntimeException("Không tìm thấy bàn ID=" + tableId);
                }

                // 1. Nếu bàn đã có đơn hàng mở sẵn, trả về đơn hàng hiện tại
                Order existing = orderDAO.findOpenByTable(tableId);
                if (existing != null) {
                    conn.commit();
                    return existing;
                }

                // 2. Chuyển trạng thái bàn sang OCCUPIED (kể cả trước đó là AVAILABLE hay RESERVED)
                tableDAO.updateStatus(conn, tableId, TableStatus.OCCUPIED);

                // 3. Nếu bàn đang có lịch đặt trước (RESERVED / CONFIRMED / PENDING), cập nhật sang SEATED
                try (PreparedStatement psRes = conn.prepareStatement(
                        "UPDATE reservations SET status = 'SEATED' WHERE table_id = ? AND status IN ('PENDING', 'CONFIRMED')")) {
                    psRes.setInt(1, tableId);
                    psRes.executeUpdate();
                }

                // 4. Khởi tạo đơn hàng mới cho bàn
                Order order = new Order();
                order.setTable(table);
                order.setCashier(cashier);
                order.setOrderType(OrderType.DINE_IN);
                order.setStatus(OrderStatus.OPEN);

                // 5. Tìm thông tin khách đặt bàn gần nhất (nếu có) để gắn ghi chú và liên kết
                try (PreparedStatement psInfo = conn.prepareStatement(
                        "SELECT TOP 1 customer_id, customer_name, customer_phone, deposit_amount FROM reservations WHERE table_id = ? AND status = 'SEATED' ORDER BY id DESC")) {
                    psInfo.setInt(1, tableId);
                    try (java.sql.ResultSet rsInfo = psInfo.executeQuery()) {
                        if (rsInfo.next()) {
                            int cId = rsInfo.getInt("customer_id");
                            if (!rsInfo.wasNull()) order.setCustomerId(cId);
                            String cName = rsInfo.getString("customer_name");
                            String cPhone = rsInfo.getString("customer_phone");
                            java.math.BigDecimal dep = rsInfo.getBigDecimal("deposit_amount");
                            String note = "Khách đặt trước: " + cName + " (" + cPhone + ")";
                            if (dep != null && dep.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                note += " | Tiền cọc: " + com.pos.utils.CurrencyFormat.format(dep);
                            }
                            order.setNote(note);
                        }
                    }
                } catch (Exception ignored) {}

                int orderId = orderDAO.insert(conn, order);
                order.setId(orderId);

                conn.commit();
                log.info("Bàn {} đã mở, mã đơn #{} bởi nhân viên {}", table.getName(), orderId, cashier != null ? cashier.getUsername() : "system");
                return order;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi khi mở bàn: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("openTable error", e);
            throw new RuntimeException("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }

    /** Chuyển bàn */
    public void moveTable(int fromOrderId, int toTableId, User cashier) throws TableBusyException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                DiningTable toTable = tableDAO.lockForUpdate(conn, toTableId);
                if (toTable == null) {
                    conn.rollback();
                    throw new RuntimeException("Không tìm thấy bàn đích!");
                }
                if (!toTable.getStatus().isAvailable()) {
                    conn.rollback();
                    throw new TableBusyException("Bàn đích '" + toTable.getName() + "' không còn trống!");
                }

                Order order = orderDAO.findById(fromOrderId);
                if (order == null) {
                    conn.rollback();
                    throw new RuntimeException("Không tìm thấy đơn hàng #" + fromOrderId);
                }
                int fromTableId = order.getTable().getId();

                tableDAO.updateStatus(conn, toTableId, TableStatus.OCCUPIED);
                tableDAO.updateStatus(conn, fromTableId, TableStatus.AVAILABLE);

                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET table_id=? WHERE id=?")) {
                    ps.setInt(1, toTableId);
                    ps.setInt(2, fromOrderId);
                    ps.executeUpdate();
                }

                conn.commit();
                log.info("Chuyển đơn #{} từ bàn {} sang bàn {}", fromOrderId, fromTableId, toTableId);
            } catch (TableBusyException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi chuyển bàn: " + e.getMessage(), e);
            }
        } catch (TableBusyException e) {
            throw e;
        } catch (SQLException e) {
            log.error("moveTable error", e);
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    /** Gộp bàn: Chuyển toàn bộ món từ fromOrderId sang toOrderId, hủy/đóng fromOrderId và trả bàn fromTable về EMPTY */
    public void mergeTables(int fromOrderId, int toOrderId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order fromOrder = orderDAO.findById(fromOrderId);
                Order toOrder = orderDAO.findById(toOrderId);
                if (fromOrder == null || toOrder == null) {
                    conn.rollback();
                    throw new RuntimeException("Đơn hàng không tồn tại để gộp bàn!");
                }

                int fromTableId = fromOrder.getTable().getId();

                // Cập nhật toàn bộ order_details của fromOrder sang toOrder
                try (PreparedStatement ps = conn.prepareStatement("UPDATE order_details SET order_id=? WHERE order_id=?")) {
                    ps.setInt(1, toOrderId);
                    ps.setInt(2, fromOrderId);
                    ps.executeUpdate();
                }

                // Cập nhật trạng thái fromOrder sang CANCELLED
                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status='CANCELLED' WHERE id=?")) {
                    ps.setInt(1, fromOrderId);
                    ps.executeUpdate();
                }

                // Trả bàn từ về AVAILABLE
                tableDAO.updateStatus(conn, fromTableId, TableStatus.AVAILABLE);

                conn.commit();
                log.info("Gộp đơn #{} vào đơn #{}, giải phóng bàn ID {}", fromOrderId, toOrderId, fromTableId);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi khi gộp bàn: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("mergeTables error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    public void clearTable(int tableId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            tableDAO.updateStatus(conn, tableId, TableStatus.AVAILABLE);
            conn.commit();
        } catch (SQLException e) {
            log.error("clearTable error", e);
        }
    }

    public void updateTableStatus(int tableId, TableStatus status) {
        tableDAO.updateStatus(tableId, status);
    }

    public void addTable(String name, int capacity) {
        DiningTable t = new DiningTable();
        t.setName(name);
        t.setCapacity(capacity);
        t.setStatus(TableStatus.AVAILABLE);
        t.setQrCodeToken("QR-TABLE-" + System.currentTimeMillis() % 10000);
        tableDAO.insert(t);
    }

    public void updateTable(int id, String name, int capacity) {
        String sql = "UPDATE dining_tables SET name=?, capacity=? WHERE id=?";
        try (java.sql.Connection c = com.pos.utils.DatabaseConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, capacity);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (java.sql.SQLException e) { log.error("updateTable error", e); }
    }

    public void deleteTable(int id) {
        tableDAO.delete(id);
    }

    public static class TableBusyException extends Exception {
        public TableBusyException(String msg) {
            super(msg);
        }
    }
}
