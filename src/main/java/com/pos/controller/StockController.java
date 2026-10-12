package com.pos.controller;

import com.pos.dao.IngredientDAO;
import com.pos.dao.RecipeDAO;
import com.pos.dao.StockMovementDAO;
import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockController {
    private static final Logger log = LogManager.getLogger(StockController.class);
    private final IngredientDAO ingredientDAO = new IngredientDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final StockMovementDAO movementDAO = new StockMovementDAO();
    private final List<StockObserver> observers = new ArrayList<>();

    public void attach(StockObserver o) {
        if (!observers.contains(o)) observers.add(o);
    }

    public void detach(StockObserver o) {
        observers.remove(o);
    }

    private void notifyObservers(int menuItemId) {
        for (StockObserver o : observers) {
            try {
                o.onStockChanged(menuItemId);
            } catch (Exception e) {
                log.error("StockObserver notify error", e);
            }
        }
    }

    /** Nhập kho nguyên liệu */
    public void importStock(int ingredientId, BigDecimal qty, User user, String note) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                Ingredient ing = ingredientDAO.lockForUpdate(conn, ingredientId);
                if (ing == null) throw new RuntimeException("Không tìm thấy nguyên liệu ID=" + ingredientId);
                BigDecimal newQty = ing.getStockQty().add(qty);
                ingredientDAO.updateQty(conn, ingredientId, newQty);

                StockMovement sm = new StockMovement();
                sm.setIngredient(ing);
                sm.setUser(user);
                sm.setType(MovementType.IMPORT);
                sm.setQuantity(qty);
                sm.setNote(note != null ? note : "Nhập kho");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Nhập kho: {} +{} {} bởi {}", ing.getName(), qty, ing.getUnit(), user.getUsername());
                notifyObservers(-1);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi nhập kho: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("importStock error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    /** Điều chỉnh kho / kiểm kê */
    public void adjustStock(int ingredientId, BigDecimal newQty, User user, String note) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                Ingredient ing = ingredientDAO.lockForUpdate(conn, ingredientId);
                if (ing == null) return;
                BigDecimal diff = newQty.subtract(ing.getStockQty());
                ingredientDAO.updateQty(conn, ingredientId, newQty);

                StockMovement sm = new StockMovement();
                sm.setIngredient(ing);
                sm.setUser(user);
                sm.setType(MovementType.ADJUST);
                sm.setQuantity(diff);
                sm.setNote(note != null ? note : "Kiểm kê điều chỉnh");
                movementDAO.insert(conn, sm);

                conn.commit();
                log.info("Điều chỉnh kho: {} thành {} {} bởi {}", ing.getName(), newQty, ing.getUnit(), user.getUsername());
                notifyObservers(-1);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi điều chỉnh kho: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("adjustStock error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    /**
     * Trừ kho khi bếp nhận món (pessimistic lock trên nguyên liệu).
     * @return true nếu đủ nguyên liệu và đã trừ thành công, false nếu thiếu nguyên liệu
     */
    public boolean deductForItem(OrderDetail detail, User user) {
        List<RecipeLine> recipe = recipeDAO.findByMenuItem(detail.getItem().getId());
        if (recipe == null || recipe.isEmpty()) {
            return true; // Món không có định lượng công thức, không cần trừ kho
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (RecipeLine line : recipe) {
                    Ingredient locked = ingredientDAO.lockForUpdate(conn, line.getIngredient().getId());
                    if (locked == null) {
                        conn.rollback();
                        return false;
                    }
                    BigDecimal needed = line.getQtyPerServing().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    if (locked.getStockQty().compareTo(needed) < 0) {
                        conn.rollback();
                        log.warn("Hết nguyên liệu: {} cần {} {} nhưng chỉ còn {}",
                                locked.getName(), needed, locked.getUnit(), locked.getStockQty());
                        return false;
                    }
                    BigDecimal remaining = locked.getStockQty().subtract(needed);
                    ingredientDAO.updateQty(conn, locked.getId(), remaining);

                    StockMovement sm = new StockMovement();
                    sm.setIngredient(locked);
                    sm.setUser(user);
                    sm.setOrderId(detail.getOrderId());
                    sm.setType(MovementType.EXPORT);
                    sm.setQuantity(needed.negate());
                    sm.setNote("Xuất chế biến chi tiết đơn #" + detail.getId());
                    movementDAO.insert(conn, sm);
                }
                conn.commit();
                notifyObservers(detail.getItem().getId());
                return true;
            } catch (Exception e) {
                conn.rollback();
                log.error("deductForItem error", e);
                return false;
            }
        } catch (SQLException e) {
            log.error("deductForItem connection error", e);
            return false;
        }
    }

    /** Hoàn kho khi hủy 1 món ACCEPTED trong cùng transaction */
    public void returnItem(Connection conn, OrderDetail detail) throws SQLException {
        List<RecipeLine> recipe = recipeDAO.findByMenuItem(detail.getItem().getId());
        if (recipe == null || recipe.isEmpty()) return;

        User sysUser = new User();
        sysUser.setId(1); // default admin/system user

        for (RecipeLine line : recipe) {
            Ingredient locked = ingredientDAO.lockForUpdate(conn, line.getIngredient().getId());
            if (locked == null) continue;
            BigDecimal returned = line.getQtyPerServing().multiply(BigDecimal.valueOf(detail.getQuantity()));
            ingredientDAO.updateQty(conn, locked.getId(), locked.getStockQty().add(returned));

            StockMovement sm = new StockMovement();
            sm.setIngredient(locked);
            sm.setUser(sysUser);
            sm.setOrderId(detail.getOrderId());
            sm.setType(MovementType.RETURN);
            sm.setQuantity(returned);
            sm.setNote("Hoàn kho do hủy chi tiết đơn #" + detail.getId());
            movementDAO.insert(conn, sm);
        }
        notifyObservers(detail.getItem().getId());
    }

    public int getRemainingServings(int menuItemId) {
        return recipeDAO.remainingServings(menuItemId);
    }

    public List<Ingredient> getLowStock() {
        return ingredientDAO.findLowStock();
    }

    public List<Ingredient> getAllIngredients() {
        return ingredientDAO.findAll();
    }
}
