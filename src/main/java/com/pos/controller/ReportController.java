package com.pos.controller;

import com.pos.dao.OrderDAO;
import com.pos.model.Order;
import com.pos.utils.PDFExporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportController {
    private static final Logger log = LogManager.getLogger(ReportController.class);
    private final OrderDAO orderDAO = new OrderDAO();

    public Map<String, BigDecimal> revenueChart() {
        return orderDAO.revenueLast7Days();
    }

    public List<Order> getOrdersByDateRange(LocalDateTime from, LocalDateTime to) {
        return orderDAO.findPaidOrders(from, to);
    }

    public String exportInvoice(Order order) {
        return PDFExporter.export(order);
    }
}
