package com.pos.controller;

import com.pos.dao.OrderDAO;
import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import com.pos.utils.PDFExporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderController {
    private static final Logger log = LogManager.getLogger(OrderController.class);
    private final OrderDAO orderDAO = new OrderDAO();
    private final List<OrderObserver> observers = new ArrayList<>();
    private DiscountStrategy discountStrategy = new NoDiscount();

    public void attach(OrderObserver o) {
        if (!observers.contains(o)) observers.add(o);
    }

    public void detach(OrderObserver o) {
        observers.remove(o);
    }

    public void notifyObservers(OrderEvent event) {
        for (OrderObserver o : observers) {
            try {
                o.onOrderChanged(event);
            } catch (Exception e) {
                log.error("OrderObserver notify error", e);
            }
        }
    }

    public void setDiscountStrategy(DiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
    }

    /** Thêm món vào đơn hàng với trạng thái ban đầu PENDING */
    public void addItem(Order order, MenuItem item, int quantity, String note) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderDetail detail = new OrderDetail();
                detail.setOrderId(order.getId());
                detail.setItem(item);
                detail.setQuantity(quantity);
                detail.setUnitPrice(item.getPrice());
                detail.setNote(note);
                detail.setStatus(ItemStatus.PENDING);
                detail.setSentToKitchen(false);

                orderDAO.addDetail(conn, detail);
                order.addDetail(detail);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi thêm món vào đơn: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("addItem error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    /** Gửi toàn bộ món chưa gửi của đơn hàng sang bếp */
    public void sendToKitchen(int orderId) {
        orderDAO.sendToKitchen(orderId);
        notifyObservers(new OrderEvent(OrderEvent.Type.ITEM_SENT, orderId, -1, "Đơn hàng #" + orderId + " đã gửi bếp"));
        log.info("Đơn hàng #{} đã gửi bếp", orderId);
    }

    /** Hủy 1 món cụ thể (chỉ cho phép nếu PENDING hoặc ACCEPTED) */
    public void cancelItem(int detailId) {
        OrderDetail detail = orderDAO.findDetailById(detailId);
        if (detail == null) return;
        ItemStatus st = detail.getStatus();
        if (st == ItemStatus.COOKING || st == ItemStatus.DONE) {
            throw new RuntimeException("Không thể hủy món đang nấu hoặc đã nấu xong!");
        }
        orderDAO.updateDetailStatus(detailId, ItemStatus.REJECTED);
        notifyObservers(new OrderEvent(OrderEvent.Type.ITEM_REJECTED, detail.getOrderId(), detailId,
            "Hủy món: " + (detail.getItem() != null ? detail.getItem().getName() : "")));
    }

    /** Hủy toàn bộ đơn hàng và hoàn kho các món ACCEPTED */
    public void cancelOrder(int orderId, int tableId, List<OrderDetail> acceptedDetails, StockController stockController) {
        if (!orderDAO.canCancel(orderId)) {
            throw new RuntimeException("Đơn hàng có món đang hoặc đã nấu xong, không thể hủy toàn bộ!");
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (acceptedDetails != null) {
                    for (OrderDetail d : acceptedDetails) {
                        if (d.getStatus() == ItemStatus.ACCEPTED) {
                            stockController.returnItem(conn, d);
                        }
                    }
                }
                orderDAO.cancelOrder(conn, orderId);
                conn.commit();
                notifyObservers(new OrderEvent(OrderEvent.Type.ORDER_CANCELLED, orderId, -1, "Đơn hàng #" + orderId + " đã hủy"));
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi khi hủy đơn hàng: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("cancelOrder error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
    }

    /** Tính tổng tiền sau chiết khấu + VAT 10% */
    public BigDecimal calculateTotal(Order order) {
        BigDecimal subtotal = order.getDetails().stream()
            .filter(d -> d.getStatus() != ItemStatus.REJECTED)
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = discountStrategy.apply(subtotal);
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal vatRate = order.getVatRate() != null ? order.getVatRate() : new BigDecimal("0.10");
        BigDecimal vat = afterDiscount.multiply(vatRate).setScale(0, RoundingMode.HALF_UP);
        return afterDiscount.add(vat);
    }

    /** Thanh toán đơn hàng */
    public String checkout(Order order, String paymentType, DiscountStrategy strategy, StockController stockController) {
        List<OrderDetail> details = orderDAO.findDetailsByOrder(order.getId());
        long validCount = details.stream().filter(d -> d.getStatus() != ItemStatus.REJECTED).count();
        if (validCount == 0) {
            throw new RuntimeException("Đơn hàng chưa có món nào để thanh toán!");
        }

        int unfinished = orderDAO.countUnfinishedDetails(order.getId());
        if (unfinished > 0) {
            throw new RuntimeException("Còn " + unfinished + " món chưa hoàn thành (chưa xong bếp)! Không thể thanh toán.");
        }

        order.setDetails(details);

        BigDecimal subtotal = order.getDetails().stream()
            .filter(d -> d.getStatus() != ItemStatus.REJECTED)
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = strategy.apply(subtotal);
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal vatRate = new BigDecimal("0.10");
        BigDecimal vat = afterDiscount.multiply(vatRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = afterDiscount.add(vat);

        order.setDiscountAmount(discount);
        order.setDiscountType(DiscountType.valueOf(
            strategy instanceof VipDiscount ? "VIP" : strategy instanceof HolidayDiscount ? "HOLIDAY" : "NONE"));
        order.setVatRate(vatRate);
        order.setTotalAmount(total);
        order.setPaymentMethod(PaymentMethodType.valueOf(paymentType));

        PaymentMethod pm = PaymentFactory.create(paymentType);
        pm.pay(total);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDAO.pay(conn, order);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi lưu thanh toán: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("checkout error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }

        String pdfPath = PDFExporter.export(order);
        notifyObservers(new OrderEvent(OrderEvent.Type.ORDER_PAID, order.getId(), -1, "Đơn hàng #" + order.getId() + " đã thanh toán"));
        log.info("Thanh toán thành công đơn #{}, tổng tiền: {}, PDF: {}", order.getId(), total, pdfPath);
        return pdfPath;
    }

    public boolean canPay(int orderId) {
        return orderDAO.countUnfinishedDetails(orderId) == 0;
    }

    public List<OrderDetail> getOrderDetails(int orderId) {
        return orderDAO.findDetailsByOrder(orderId);
    }
}
