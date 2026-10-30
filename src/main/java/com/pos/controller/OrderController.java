package com.pos.controller;

import com.pos.dao.OrderDAO;
import com.pos.dao.TableDAO;
import com.pos.dao.VoucherDAO;
import com.pos.model.*;
import com.pos.utils.DatabaseConnection;
import com.pos.utils.PDFExporter;
import com.pos.utils.PrinterService;
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
    private final TableDAO tableDAO = new TableDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
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
        this.discountStrategy = strategy != null ? strategy : new NoDiscount();
    }

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
    }

    /** Tạo đơn hàng mới (Hỗ trợ Dine-in, Takeaway, Delivery) */
    public Order createOrder(Integer tableId, User cashier, Shift currentShift, OrderType orderType,
                             Customer customer, String address, String phone, String shipper, String note) {
        Order o = new Order();
        o.setTableId(tableId);
        if (tableId != null) {
            DiningTable t = tableDAO.findById(tableId);
            o.setTable(t);
        }
        o.setCashier(cashier);
        if (currentShift != null) o.setShiftId(currentShift.getId());
        o.setOrderType(orderType != null ? orderType : OrderType.DINE_IN);
        if (customer != null) {
            o.setCustomerId(customer.getId());
            o.setCustomerName(customer.getName());
        }
        o.setDeliveryAddress(address);
        o.setDeliveryPhone(phone);
        o.setShipperName(shipper);
        o.setNote(note);
        o.setStatus(OrderStatus.OPEN);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = orderDAO.insert(conn, o);
                o.setId(id);
                if (tableId != null && o.getOrderType() == OrderType.DINE_IN) {
                    tableDAO.updateStatus(conn, tableId, TableStatus.OCCUPIED);
                }
                conn.commit();
                notifyObservers(new OrderEvent(OrderEvent.Type.ORDER_CREATED, id, -1, "Đơn hàng mới #" + id));
                return o;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi tạo đơn hàng: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("createOrder error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }
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

    public void updateItemQuantity(int detailId, int quantity) {
        orderDAO.updateDetailQuantity(detailId, quantity);
    }

    public void updateDetailNote(int detailId, String note) {
        orderDAO.updateDetailNote(detailId, note);
    }

    public void removeItemFromOrder(int detailId) {
        orderDAO.deleteDetail(detailId);
    }

    public void cancelOrder(int orderId) {
        cancelOrder(orderId, null);
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

    /** Hủy toàn bộ đơn hàng */
    public void cancelOrder(int orderId, Integer tableId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDAO.cancelOrder(conn, orderId);
                if (tableId != null && tableId > 0) {
                    tableDAO.updateStatus(conn, tableId, TableStatus.AVAILABLE);
                }
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

    /** Legacy helper cancelOrder */
    public void cancelOrder(int orderId, int tableId, List<OrderDetail> acceptedDetails, StockController stockController) {
        cancelOrder(orderId, tableId);
    }

    /** Tính tổng tiền sau chiết khấu + phí ship */
    public BigDecimal calculateTotal(Order order) {
        BigDecimal subtotal = order.getDetails().stream()
            .filter(d -> d.getStatus() != ItemStatus.REJECTED)
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = discountStrategy != null ? discountStrategy.apply(subtotal) : BigDecimal.ZERO;
        BigDecimal fee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        return subtotal.subtract(discount).add(fee).max(BigDecimal.ZERO);
    }

    /**
     * Thanh toán đơn hàng đầy đủ:
     * - Áp dụng voucher / điểm tích lũy / giảm giá
     * - Tích điểm khách hàng
     * - Cập nhật ca làm việc (Shift)
     * - In hóa đơn vật lý ESC/POS & xuất PDF
     */
    public String checkout(Order order, String paymentType, DiscountStrategy strategy,
                           CustomerController customerController, ShiftController shiftController) {
        List<OrderDetail> details = orderDAO.findDetailsByOrder(order.getId());
        long validCount = details.stream().filter(d -> d.getStatus() != ItemStatus.REJECTED).count();
        if (validCount == 0) {
            throw new RuntimeException("Đơn hàng chưa có món nào để thanh toán!");
        }

        long unsentCount = details.stream().filter(d -> !d.isSentToKitchen() && d.getStatus() != ItemStatus.REJECTED).count();
        if (unsentCount > 0) {
            throw new RuntimeException("Không thể thanh toán: Đơn hàng đang có " + unsentCount + " món chưa gửi sang Bếp!");
        }

        long notDoneCount = details.stream().filter(d -> d.getStatus() != ItemStatus.DONE && d.getStatus() != ItemStatus.REJECTED).count();
        if (notDoneCount > 0) {
            throw new RuntimeException("Không thể thanh toán: Đơn hàng đang có " + notDoneCount + " món chưa được Bếp báo nấu xong!");
        }

        order.setDetails(details);

        BigDecimal subtotal = details.stream()
            .filter(d -> d.getStatus() != ItemStatus.REJECTED)
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = strategy != null ? strategy.apply(subtotal) : BigDecimal.ZERO;
        BigDecimal fee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(fee).max(BigDecimal.ZERO);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);

        try {
            order.setPaymentMethod(PaymentMethodType.valueOf(paymentType));
        } catch (Exception e) {
            order.setPaymentMethod(PaymentMethodType.CASH);
        }

        // Voucher & Points
        if (strategy instanceof VoucherDiscountStrategy vds && vds.getVoucher() != null) {
            order.setVoucherId(vds.getVoucher().getId());
            order.setVoucherCode(vds.getVoucher().getCode());
            voucherDAO.incrementUsedCount(vds.getVoucher().getId());
        }

        if (strategy instanceof LoyaltyPointsDiscount lpd) {
            order.setPointsUsed(lpd.getPointsToUse());
        }

        // Tích điểm cho khách hàng (nếu có khách)
        if (order.getCustomerId() != null && order.getCustomerId() > 0 && customerController != null) {
            int pointsEarned = total.divide(new BigDecimal("1000"), 0, RoundingMode.DOWN).intValue();
            order.setPointsEarned(pointsEarned);
            customerController.processOrderLoyalty(order.getCustomerId(), total, order.getPointsUsed());
        }

        // Ghi nhận doanh thu vào Ca làm việc hiện tại
        if (shiftController != null) {
            BigDecimal cashPart = "CASH".equalsIgnoreCase(paymentType) ? total : BigDecimal.ZERO;
            BigDecimal transferPart = "TRANSFER".equalsIgnoreCase(paymentType) ? total : BigDecimal.ZERO;
            shiftController.recordSale(cashPart, transferPart);
        }

        PaymentMethod pm = PaymentFactory.create(paymentType);
        pm.pay(total);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                orderDAO.pay(conn, order);
                if (order.getTableId() != null && order.getOrderType() == OrderType.DINE_IN) {
                    tableDAO.updateStatus(conn, order.getTableId(), TableStatus.AVAILABLE);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Lỗi lưu thanh toán: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            log.error("checkout error", e);
            throw new RuntimeException("Lỗi kết nối CSDL: " + e.getMessage(), e);
        }

        // IN HÓA ĐƠN VẬT LÝ ESC/POS (Hardware)
        PrinterService.printReceipt(order);

        // XUẤT FILE PDF
        String pdfPath = PDFExporter.export(order);

        notifyObservers(new OrderEvent(OrderEvent.Type.ORDER_PAID, order.getId(), -1, "Đơn hàng #" + order.getId() + " đã thanh toán"));
        log.info("Thanh toán thành công đơn #{}, tổng: {}, PDF: {}", order.getId(), total, pdfPath);
        return pdfPath;
    }

    /** Checkout compat method */
    public String checkout(Order order, String paymentType, DiscountStrategy strategy, StockController stockController) {
        return checkout(order, paymentType, strategy, null, null);
    }

    public List<OrderDetail> getOrderDetails(int orderId) {
        return orderDAO.findDetailsByOrder(orderId);
    }

    public Order getOpenOrderByTable(int tableId) {
        return orderDAO.findOpenByTable(tableId);
    }

    public List<Order> getActiveTakeawayOrders() {
        return orderDAO.findActiveTakeawayOrders();
    }
}
