package com.pos.controller;

import com.pos.dao.InventoryAuditDAO;
import com.pos.dao.InventoryItemDAO;
import com.pos.dao.StockMovementDAO;
import com.pos.dao.SupplierDAO;
import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller quản lý Kho hàng hóa (Inventory Management).
 * Hỗ trợ Quản lý mặt hàng, Nhà cung cấp, Xuất nhập tồn và Kiểm kê cân bằng kho.
 */
public class InventoryController {
    private static final Logger log = LogManager.getLogger(InventoryController.class);

    private final InventoryItemDAO itemDAO = new InventoryItemDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final StockMovementDAO movementDAO = new StockMovementDAO();
    private final InventoryAuditDAO auditDAO = new InventoryAuditDAO();
    private final List<StockObserver> observers = new ArrayList<>();

    public void attach(StockObserver o) {
        if (!observers.contains(o)) observers.add(o);
    }

    public void detach(StockObserver o) {
        observers.remove(o);
    }

    private void notifyObservers() {
        for (StockObserver o : observers) {
            try {
                o.onStockChanged(-1);
            } catch (Exception e) {
                log.error("StockObserver notify error", e);
            }
        }
    }

    // ==================== INVENTORY ITEMS ====================
    public List<InventoryItem> getAllItems() {
        return itemDAO.findAll();
    }

    public List<InventoryItem> getActiveItems() {
        return itemDAO.findActive();
    }

    public List<InventoryItem> getLowStockItems() {
        return itemDAO.findLowStock();
    }

    public InventoryItem getItemById(int id) {
        return itemDAO.findById(id);
    }

    public int saveItem(InventoryItem item) {
        int id;
        if (item.getId() > 0) {
            itemDAO.update(item);
            id = item.getId();
        } else {
            id = itemDAO.insert(item);
        }
        notifyObservers();
        return id;
    }

    public void deleteItem(int id) {
        itemDAO.delete(id);
        notifyObservers();
    }

    // ==================== SUPPLIERS ====================
    public List<Supplier> getAllSuppliers() {
        return supplierDAO.findAll();
    }

    public List<Supplier> getActiveSuppliers() {
        return supplierDAO.findActive();
    }

    public int saveSupplier(Supplier s) {
        if (s.getId() > 0) {
            supplierDAO.update(s);
            return s.getId();
        } else {
            return supplierDAO.insert(s);
        }
    }

    public void deleteSupplier(int id) {
        supplierDAO.delete(id);
    }

    // ==================== STOCK MOVEMENTS (IMPORT / EXPORT / ADJUST) ====================
    public void importStock(int itemId, Integer supplierId, BigDecimal qty, BigDecimal unitPrice, User user, String note) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                itemDAO.updateStock(conn, itemId, qty);

                StockMovement sm = new StockMovement();
                sm.setItemId(itemId);
                sm.setSupplierId(supplierId);
                sm.setUserId(user.getId());
                sm.setType(MovementType.IMPORT);
                sm.setQuantity(qty);
                sm.setUnitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO);
                sm.setNote(note != null ? note : "Nhập hàng từ nhà cung cấp");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Nhập kho thành công: item={}, qty={}, supplier={}", itemId, qty, supplierId);
                notifyObservers();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi nhập kho: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("importStock error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    public void exportStock(int itemId, BigDecimal qty, User user, String note) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng xuất phải lớn hơn 0");
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                InventoryItem item = itemDAO.findById(itemId);
                if (item == null) throw new IllegalArgumentException("Hàng hóa không tồn tại");
                if (item.getStockQty().compareTo(qty) < 0) {
                    throw new IllegalStateException("Số lượng tồn kho không đủ để xuất (Tồn: " + item.getStockQty() + " " + item.getUnit() + ")");
                }

                itemDAO.updateStock(conn, itemId, qty.negate());

                StockMovement sm = new StockMovement();
                sm.setItemId(itemId);
                sm.setUserId(user.getId());
                sm.setType(MovementType.EXPORT);
                sm.setQuantity(qty.negate());
                sm.setUnitPrice(item.getCostPrice());
                sm.setNote(note != null ? note : "Xuất kho");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Xuất kho thành công: item={}, qty={}", itemId, qty);
                notifyObservers();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi xuất kho: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("exportStock error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    public void adjustStock(int itemId, BigDecimal newQty, User user, String note) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                InventoryItem item = itemDAO.findById(itemId);
                if (item == null) return;
                BigDecimal diff = newQty.subtract(item.getStockQty());
                itemDAO.setStock(conn, itemId, newQty);

                StockMovement sm = new StockMovement();
                sm.setItemId(itemId);
                sm.setUserId(user.getId());
                sm.setType(MovementType.ADJUST);
                sm.setQuantity(diff);
                sm.setUnitPrice(item.getCostPrice());
                sm.setNote(note != null ? note : "Điều chỉnh kho thủ công");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Điều chỉnh kho thành công: item={}, newQty={}, diff={}", itemId, newQty, diff);
                notifyObservers();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi điều chỉnh kho: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("adjustStock error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    public List<StockMovement> getMovementsByItem(int itemId) {
        return movementDAO.findByItem(itemId);
    }

    public List<StockMovement> getAllMovements() {
        return movementDAO.findAll();
    }

    // ==================== INVENTORY AUDITS (KIỂM KHO) ====================
    public int performStocktake(User user, String note, List<InventoryAuditDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng kiểm kê không được rỗng.");
        }
        String auditCode = "KK-" + System.currentTimeMillis();
        InventoryAudit audit = new InventoryAudit();
        audit.setAuditCode(auditCode);
        audit.setUserId(user.getId());
        audit.setNote(note);
        audit.setStatus(AuditStatus.COMPLETED);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int auditId = auditDAO.createAudit(conn, audit);
                for (InventoryAuditDetail d : details) {
                    BigDecimal diff = d.getDifferenceQty() != null ? d.getDifferenceQty() : d.getActualQty().subtract(d.getSystemQty());
                    d.setDifferenceQty(diff);
                    auditDAO.insertAuditDetail(conn, auditId, d);

                    // Cân bằng số tồn kho theo số thực tế
                    itemDAO.setStock(conn, d.getItemId(), d.getActualQty());

                    // Ghi nhận biến động kiểm kho
                    if (diff.compareTo(BigDecimal.ZERO) != 0) {
                        StockMovement sm = new StockMovement();
                        sm.setItemId(d.getItemId());
                        sm.setUserId(user.getId());
                        sm.setType(MovementType.ADJUSTMENT);
                        sm.setQuantity(diff);
                        sm.setNote("Kiểm kê #" + auditCode + (d.getReason() != null ? ": " + d.getReason() : ""));
                        movementDAO.insert(conn, sm);
                    }
                }
                conn.commit();
                log.info("Kiểm kho hoàn tất thành công: code={}, items={}", auditCode, details.size());
                notifyObservers();
                return auditId;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi kiểm kho: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("performStocktake error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    public void returnStockToSupplier(int itemId, Integer supplierId, BigDecimal qty, BigDecimal unitPrice, User user, String note) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng trả phải lớn hơn 0");
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                InventoryItem item = itemDAO.findById(itemId);
                if (item == null) throw new IllegalArgumentException("Hàng hóa không tồn tại");
                if (item.getStockQty().compareTo(qty) < 0) {
                    throw new IllegalStateException("Số lượng tồn kho không đủ để trả (Tồn hiện tại: " + item.getStockQty() + " " + item.getUnit() + ")");
                }

                itemDAO.updateStock(conn, itemId, qty.negate());

                StockMovement sm = new StockMovement();
                sm.setItemId(itemId);
                sm.setSupplierId(supplierId);
                sm.setUserId(user.getId());
                sm.setType(MovementType.RETURN);
                sm.setQuantity(qty.negate());
                sm.setUnitPrice(unitPrice != null ? unitPrice : item.getCostPrice());
                sm.setNote(note != null ? note : "Trả hàng lại Nhà Cung Cấp");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Trả hàng NCC thành công: item={}, qty={}", itemId, qty);
                notifyObservers();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi trả hàng: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("returnStockToSupplier error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    public void exportWaste(int itemId, BigDecimal qty, User user, String reason) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng xuất hủy phải lớn hơn 0");
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                InventoryItem item = itemDAO.findById(itemId);
                if (item == null) throw new IllegalArgumentException("Hàng hóa không tồn tại");
                if (item.getStockQty().compareTo(qty) < 0) {
                    throw new IllegalStateException("Số lượng tồn kho không đủ để xuất hủy (Tồn: " + item.getStockQty() + " " + item.getUnit() + ")");
                }

                itemDAO.updateStock(conn, itemId, qty.negate());

                StockMovement sm = new StockMovement();
                sm.setItemId(itemId);
                sm.setUserId(user.getId());
                sm.setType(MovementType.WASTE);
                sm.setQuantity(qty.negate());
                sm.setUnitPrice(item.getCostPrice());
                sm.setNote(reason != null ? reason : "Xuất hủy do hư hỏng / hết hạn sử dụng");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Xuất hủy thành công: item={}, qty={}", itemId, qty);
                notifyObservers();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi xuất hủy: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("exportWaste error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    /**
     * Tính toán ước tính định lượng tiêu hao lý thuyết của các nguyên vật liệu dựa trên số lượng các món đã bán hôm nay.
     * Người kiểm kho dùng số liệu này để so sánh, đối chiếu với số lượng đếm thực tế khi kiểm kê.
     */
    public List<TheoreticalUsage> getTheoreticalUsagesToday() {
        List<TheoreticalUsage> result = new ArrayList<>();
        List<InventoryItem> items = itemDAO.findActive();

        // Đếm số món bán ra hôm nay
        java.util.Map<Integer, Integer> dishSales = new java.util.HashMap<>();
        String sqlSales = "SELECT od.menu_item_id, SUM(od.quantity) as total_sold " +
                          "FROM order_details od JOIN orders o ON od.order_id = o.id " +
                          "WHERE CAST(o.created_at AS DATE) = CAST(GETDATE() AS DATE) AND o.status != 'CANCELLED' " +
                          "GROUP BY od.menu_item_id";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sqlSales)) {
            while (rs.next()) {
                dishSales.put(rs.getInt("menu_item_id"), rs.getInt("total_sold"));
            }
        } catch (Exception ex) {
            log.warn("getTheoreticalUsagesToday sales query error: {}", ex.getMessage());
        }

        for (InventoryItem it : items) {
            BigDecimal used = BigDecimal.ZERO;
            int dishesCount = 0;

            // Tính toán định lượng tiêu hao lý thuyết theo công thức chuẩn:
            switch (it.getId()) {
                case 1: // Thịt bò thăn
                    // Phở bò (id 1): 0.15kg, Cơm rang dưa bò (id 15): 0.12kg, Bò lúc lắc (id 34): 0.20kg, Bún bò Huế (id 5): 0.08kg
                    int s1 = dishSales.getOrDefault(1, 0);
                    int s15 = dishSales.getOrDefault(15, 0);
                    int s34 = dishSales.getOrDefault(34, 0);
                    int s5 = dishSales.getOrDefault(5, 0);
                    dishesCount = s1 + s15 + s34 + s5;
                    used = new BigDecimal(s1 * 0.15 + s15 * 0.12 + s34 * 0.20 + s5 * 0.08);
                    break;
                case 2: // Thịt heo ba chỉ
                    // Cơm tấm (id 13): 0.15kg, Bún đậu (id 7): 0.15kg, Bánh xèo (id 21): 0.10kg
                    int s13 = dishSales.getOrDefault(13, 0);
                    int s7 = dishSales.getOrDefault(7, 0);
                    int s21 = dishSales.getOrDefault(21, 0);
                    dishesCount = s13 + s7 + s21;
                    used = new BigDecimal(s13 * 0.15 + s7 * 0.15 + s21 * 0.10);
                    break;
                case 3: // Gà ta
                    // Phở gà (id 2): 0.25 con, Cơm gà (id 14): 0.25 con, Gà rán (id 28): 0.20 con
                    int s2 = dishSales.getOrDefault(2, 0);
                    int s14 = dishSales.getOrDefault(14, 0);
                    int s28 = dishSales.getOrDefault(28, 0);
                    dishesCount = s2 + s14 + s28;
                    used = new BigDecimal(s2 * 0.25 + s14 * 0.25 + s28 * 0.20);
                    break;
                case 4: // Tôm sú
                    // Hủ tiếu (id 3): 0.08kg, Mì Quảng (id 9): 0.08kg, Tôm càng (id 38): 0.25kg
                    int s3 = dishSales.getOrDefault(3, 0);
                    int s9 = dishSales.getOrDefault(9, 0);
                    int s38 = dishSales.getOrDefault(38, 0);
                    dishesCount = s3 + s9 + s38;
                    used = new BigDecimal(s3 * 0.08 + s9 * 0.08 + s38 * 0.25);
                    break;
                case 5: // Cua biển
                    // Bánh canh cua (id 4): 0.15kg, Bún riêu (id 8): 0.15kg, Cua rang me (id 39): 0.30kg
                    int s4 = dishSales.getOrDefault(4, 0);
                    int s8 = dishSales.getOrDefault(8, 0);
                    int s39 = dishSales.getOrDefault(39, 0);
                    dishesCount = s4 + s8 + s39;
                    used = new BigDecimal(s4 * 0.15 + s8 * 0.15 + s39 * 0.30);
                    break;
                case 7: // Hành hoa & Ngò rí
                    // Món nước (1-4) & Món bún (5-8): ~0.03 kg / tô
                    int totalBowls = 0;
                    for (int mi = 1; mi <= 8; mi++) totalBowls += dishSales.getOrDefault(mi, 0);
                    dishesCount = totalBowls;
                    used = new BigDecimal(totalBowls * 0.03);
                    break;
                case 8: // Cà chua Đà Lạt
                    // Bún riêu (id 8): 0.10kg, Mì xào (id 11): 0.08kg
                    int s8_c = dishSales.getOrDefault(8, 0);
                    int s11 = dishSales.getOrDefault(11, 0);
                    dishesCount = s8_c + s11;
                    used = new BigDecimal(s8_c * 0.10 + s11 * 0.08);
                    break;
                case 9: // Bánh phở tươi
                    // Phở bò (id 1) & Phở gà (id 2): ~0.20 kg / bát
                    int phoCount = dishSales.getOrDefault(1, 0) + dishSales.getOrDefault(2, 0);
                    dishesCount = phoCount;
                    used = new BigDecimal(phoCount * 0.20);
                    break;
                case 11: // Gạo ST25
                    // Cơm (13-16): ~0.15 kg / đĩa
                    int riceCount = 0;
                    for (int mi = 13; mi <= 16; mi++) riceCount += dishSales.getOrDefault(mi, 0);
                    dishesCount = riceCount;
                    used = new BigDecimal(riceCount * 0.15);
                    break;
                default:
                    // Các mặt hàng khác: ước tính tỉ lệ tượng trưng theo doanh số chung
                    used = BigDecimal.ZERO;
                    break;
            }

            used = used.setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal rem = it.getStockQty().subtract(used);
            result.add(new TheoreticalUsage(it.getId(), it.getName(), it.getUnit(), it.getStockQty(), used, rem, dishesCount));
        }
        return result;
    }

    public List<InventoryAudit> getAllAudits() {
        return auditDAO.findAll();
    }

    public List<InventoryAuditDetail> getAuditDetails(int auditId) {
        return auditDAO.findDetailsByAuditId(auditId);
    }
}
