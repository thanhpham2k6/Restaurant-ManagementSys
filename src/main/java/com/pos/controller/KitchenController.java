package com.pos.controller;

import com.pos.dao.OrderDAO;
import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class KitchenController {
    private static final Logger log = LogManager.getLogger(KitchenController.class);
    private final OrderDAO orderDAO = new OrderDAO();
    private final StockController stockController;
    private final List<OrderObserver> observers = new ArrayList<>();

    public KitchenController(StockController stockController) {
        this.stockController = stockController;
    }

    public void attach(OrderObserver o) {
        if (!observers.contains(o)) observers.add(o);
    }

    public void detach(OrderObserver o) {
        observers.remove(o);
    }

    private void notifyObservers(OrderEvent event) {
        for (OrderObserver o : observers) {
            try {
                o.onOrderChanged(event);
            } catch (Exception e) {
                log.error("KitchenController notify error", e);
            }
        }
    }

    public List<OrderDetail> getPendingItems() {
        String sql = "SELECT od.id FROM order_details od JOIN orders o ON od.order_id = o.id " +
                     "WHERE od.status='PENDING' AND o.status IN ('OPEN','PENDING','IN_PROGRESS') AND od.sent_to_kitchen = 1";
        List<OrderDetail> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                OrderDetail d = orderDAO.findDetailById(rs.getInt("id"));
                if (d != null) list.add(d);
            }
        } catch (Exception e) {
            log.error("getPendingItems error", e);
        }
        return list;
    }

    public List<OrderDetail> getActiveItems() {
        List<OrderDetail> list = new ArrayList<>();
        String sql = "SELECT od.id FROM order_details od JOIN orders o ON od.order_id = o.id " +
                     "WHERE od.status IN ('PENDING','ACCEPTED','COOKING') AND o.status IN ('OPEN','PENDING','IN_PROGRESS') AND od.sent_to_kitchen = 1 " +
                     "ORDER BY od.id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                OrderDetail d = orderDAO.findDetailById(rs.getInt("id"));
                if (d != null) list.add(d);
            }
        } catch (Exception e) {
            log.error("getActiveItems error", e);
        }
        return list;
    }

    public void acceptItem(int detailId, User kitchen) {
        OrderDetail detail = orderDAO.findDetailById(detailId);
        if (detail == null) return;

        // Bếp nhận chế biến món. Theo quy trình nhà hàng thực tế, hệ thống không tự động trừ kho
        // nguyên liệu tức thời (vì mỗi món ăn có độ tiêu hao linh hoạt, gia vị nêm nếm khó trừ từng gram).
        // Việc trừ tồn kho thực tế sẽ do Người kiểm kê hàng hóa (Stocktake) hoặc Phiếu xuất kho Bếp thực hiện.
        orderDAO.updateDetailStatus(detailId, ItemStatus.ACCEPTED);
        notifyObservers(new OrderEvent(OrderEvent.Type.ITEM_ACCEPTED, detail.getOrderId(), detailId,
            "Bếp đã nhận chế biến: " + (detail.getItem() != null ? detail.getItem().getName() : "")));
    }

    public void rejectItem(int detailId, String reason) {
        OrderDetail detail = orderDAO.findDetailById(detailId);
        if (detail == null) return;
        orderDAO.updateDetailStatus(detailId, ItemStatus.REJECTED);
        notifyObservers(new OrderEvent(OrderEvent.Type.ITEM_REJECTED, detail.getOrderId(), detailId,
            "Bếp từ chối món: " + (detail.getItem() != null ? detail.getItem().getName() : "") + " (" + reason + ")"));
    }

    public void startCooking(int detailId) {
        OrderDetail detail = orderDAO.findDetailById(detailId);
        if (detail == null) return;
        orderDAO.updateDetailStatus(detailId, ItemStatus.COOKING);
        notifyObservers(new OrderEvent(OrderEvent.Type.ITEM_COOKING, detail.getOrderId(), detailId,
            "Bếp đang nấu: " + (detail.getItem() != null ? detail.getItem().getName() : "")));
    }

    public void markDone(int detailId) {
        OrderDetail detail = orderDAO.findDetailById(detailId);
        if (detail == null) return;
        orderDAO.updateDetailStatus(detailId, ItemStatus.DONE);
        notifyObservers(new OrderEvent(OrderEvent.Type.ITEM_DONE, detail.getOrderId(), detailId,
            "Món đã nấu xong: " + (detail.getItem() != null ? detail.getItem().getName() : "")));
    }
}
