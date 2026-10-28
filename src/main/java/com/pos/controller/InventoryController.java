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
                        sm.setType(MovementType.ADJUST_AUDIT);
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

    public List<InventoryAudit> getAllAudits() {
        return auditDAO.findAll();
    }

    public List<InventoryAuditDetail> getAuditDetails(int auditId) {
        return auditDAO.findDetailsByAuditId(auditId);
    }
}
