package com.pos.view;

import com.pos.controller.OrderObserver;
import com.pos.dao.InventoryItemDAO;
import com.pos.dao.OrderDAO;
import com.pos.dao.TableDAO;
import com.pos.model.*;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DatabaseConnection;
import com.pos.utils.DateUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardPanel extends JPanel implements OrderObserver {
    private final User currentUser;
    private final OrderDAO orderDAO = new OrderDAO();
    private final TableDAO tableDAO = new TableDAO();
    private final InventoryItemDAO itemDAO = new InventoryItemDAO();

    private JLabel lblRevenueVal;
    private JLabel lblRevenueSub;
    private JLabel lblTableVal;
    private JLabel lblTableSub;
    private JLabel lblKitchenVal;
    private JLabel lblKitchenSub;
    private JLabel lblAlertVal;
    private JLabel lblAlertSub;

    private DefaultTableModel recentOrdersModel;
    private DefaultTableModel lowStockModel;

    public DashboardPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(16, 20, 20, 20));
        initUI();
        refreshData();
    }

    private void initUI() {
        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("TỔNG QUAN KINH DOANH");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSub = new JLabel("Chào mừng trở lại, " + currentUser.getFullName() + "! Dưới đây là hoạt động hôm nay.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        headerPanel.add(titleBox, BorderLayout.WEST);

        JButton btnRefresh = new JButton("Làm mới dữ liệu");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(238, 242, 246));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> refreshData());
        headerPanel.add(btnRefresh, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center: Cards + Tables
        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);

        // Metric Cards Grid (1 row, 4 columns)
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.setPreferredSize(new Dimension(0, 115));

        // Card 1: Revenue Today
        lblRevenueVal = new JLabel("0 đ");
        lblRevenueSub = new JLabel("0 đơn đã thanh toán");
        cardsGrid.add(createMetricCard("Doanh thu hôm nay", lblRevenueVal, lblRevenueSub, new Color(16, 185, 129)));

        // Card 2: Tables Active
        lblTableVal = new JLabel("0 / 0");
        lblTableSub = new JLabel("0 bàn đang có khách");
        cardsGrid.add(createMetricCard("Bàn đang phục vụ", lblTableVal, lblTableSub, new Color(59, 130, 246)));

        // Card 3: Kitchen Pending
        lblKitchenVal = new JLabel("0");
        lblKitchenSub = new JLabel("Món đang chờ chế biến");
        cardsGrid.add(createMetricCard("Bếp đang thực hiện", lblKitchenVal, lblKitchenSub, new Color(245, 158, 11)));

        // Card 4: Low Stock Alert
        lblAlertVal = new JLabel("0");
        lblAlertSub = new JLabel("Mặt hàng cần nhập thêm");
        cardsGrid.add(createMetricCard("Cảnh báo tồn kho", lblAlertVal, lblAlertSub, new Color(239, 68, 68)));

        centerPanel.add(cardsGrid, BorderLayout.NORTH);

        // Tables Section (2 columns: Recent Orders & Low Stock)
        JPanel tablesGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesGrid.setOpaque(false);

        // Left: Recent Orders
        JPanel recentOrdersCard = createCardPanel("Đơn hàng gần đây (15 đơn mới nhất)");
        recentOrdersModel = new DefaultTableModel(
            new Object[]{"Mã đơn", "Bàn / Loại", "Thu ngân", "Tổng tiền", "Trạng thái", "Thời gian"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblOrders = new JTable(recentOrdersModel);
        styleTable(tblOrders);
        recentOrdersCard.add(new JScrollPane(tblOrders), BorderLayout.CENTER);
        tablesGrid.add(recentOrdersCard);

        // Right: Low Stock Table
        JPanel lowStockCard = createCardPanel("Hàng hóa tồn kho cần lưu ý");
        lowStockModel = new DefaultTableModel(
            new Object[]{"Tên mặt hàng", "Tồn hiện tại", "Định mức tối thiểu", "Đơn vị tính", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblStock = new JTable(lowStockModel);
        styleTable(tblStock);
        lowStockCard.add(new JScrollPane(tblStock), BorderLayout.CENTER);
        tablesGrid.add(lowStockCard);

        centerPanel.add(tablesGrid, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valLbl, JLabel subLbl, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 116, 139));
        top.add(lblTitle, BorderLayout.WEST);

        JPanel pIndicator = new JPanel();
        pIndicator.setBackground(accent);
        pIndicator.setPreferredSize(new Dimension(8, 8));
        top.add(pIndicator, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLbl.setForeground(new Color(15, 23, 42));
        card.add(valLbl, BorderLayout.CENTER);

        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(100, 116, 139));
        card.add(subLbl, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(14, 16, 16, 16)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(30, 41, 59));
        panel.add(lbl, BorderLayout.NORTH);
        return panel;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(71, 85, 105));
        table.setSelectionBackground(new Color(237, 242, 247));
        table.setSelectionForeground(new Color(15, 23, 42));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);
    }

    public void refreshData() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                loadMetrics();
                loadRecentOrders();
                loadLowStock();
                return null;
            }
        }.execute();
    }

    private void loadMetrics() {
        BigDecimal todayTotal = BigDecimal.ZERO;
        int paidCount = 0;
        String sqlRev = "SELECT COUNT(*) as cnt, COALESCE(SUM(total_amount), 0) as total " +
                        "FROM orders WHERE status='PAID' AND CAST(COALESCE(completed_at, paid_at) AS DATE) = CAST(GETDATE() AS DATE)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sqlRev)) {
            if (rs.next()) {
                paidCount = rs.getInt("cnt");
                todayTotal = rs.getBigDecimal("total");
            }
        } catch (Exception ignored) {}

        final BigDecimal finalTodayTotal = todayTotal;
        final int finalPaidCount = paidCount;

        List<DiningTable> tables = tableDAO.findAll();
        long occupied = tables.stream().filter(t -> t.getStatus() == TableStatus.OCCUPIED).count();

        int kitchenCount = 0;
        String sqlKitchen = "SELECT COUNT(*) as cnt FROM order_details od JOIN orders o ON od.order_id = o.id " +
                            "WHERE od.sent_to_kitchen = 1 AND od.status IN ('PENDING','ACCEPTED','COOKING') AND o.status IN ('OPEN','PENDING','IN_PROGRESS')";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sqlKitchen)) {
            if (rs.next()) kitchenCount = rs.getInt("cnt");
        } catch (Exception ignored) {}
        final int finalKitchenCount = kitchenCount;

        List<InventoryItem> lowStocks = itemDAO.findLowStock();

        SwingUtilities.invokeLater(() -> {
            lblRevenueVal.setText(CurrencyFormat.format(finalTodayTotal));
            lblRevenueSub.setText(finalPaidCount + " đơn đã thanh toán hôm nay");
            lblTableVal.setText(occupied + " / " + tables.size());
            lblTableSub.setText(occupied + " bàn đang có khách ngồi");
            lblKitchenVal.setText(String.valueOf(finalKitchenCount));
            lblKitchenSub.setText(finalKitchenCount > 0 ? "Món đang chờ chế biến" : "Bếp đã hoàn tất các món");
            lblAlertVal.setText(String.valueOf(lowStocks.size()));
            lblAlertSub.setText(lowStocks.isEmpty() ? "Kho ổn định, đủ định mức" : lowStocks.size() + " mặt hàng cần nhập");
        });
    }

    private void loadRecentOrders() {
        String sql = "SELECT TOP 15 o.id, COALESCE(t.name, o.order_type) as table_name, u.full_name as cashier_name, " +
                     "o.total_amount, o.status, COALESCE(o.completed_at, o.paid_at, o.created_at) as order_time " +
                     "FROM orders o " +
                     "LEFT JOIN dining_tables t ON o.table_id = t.id " +
                     "LEFT JOIN users u ON o.user_id = u.id " +
                     "ORDER BY o.id DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            SwingUtilities.invokeLater(() -> recentOrdersModel.setRowCount(0));
            while (rs.next()) {
                int id = rs.getInt("id");
                String table = rs.getString("table_name");
                String cashier = rs.getString("cashier_name");
                BigDecimal total = rs.getBigDecimal("total_amount");
                String status = rs.getString("status");
                String statusDisplay = switch (status) {
                    case "PAID", "COMPLETED" -> "Đã thanh toán";
                    case "OPEN", "PENDING", "IN_PROGRESS" -> "Đang phục vụ";
                    case "CANCELLED" -> "Đã hủy";
                    default -> status;
                };
                java.sql.Timestamp ts = rs.getTimestamp("order_time");
                String timeStr = ts != null ? DateUtils.format(ts.toLocalDateTime()) : "";

                SwingUtilities.invokeLater(() -> {
                    recentOrdersModel.addRow(new Object[]{
                        "#" + id, table, cashier != null ? cashier : "Hệ thống", CurrencyFormat.format(total), statusDisplay, timeStr
                    });
                });
            }
        } catch (Exception ignored) {}
    }

    private void loadLowStock() {
        List<InventoryItem> list = itemDAO.findAll();
        SwingUtilities.invokeLater(() -> {
            lowStockModel.setRowCount(0);
            for (InventoryItem item : list) {
                boolean isLow = item.isLowStock();
                String status = isLow ? "SẮP HẾT HÀNG" : "Đủ dùng";
                lowStockModel.addRow(new Object[]{
                    item.getName(),
                    item.getStockQty() + " " + item.getUnit(),
                    item.getMinQty() + " " + item.getUnit(),
                    item.getUnit(),
                    status
                });
            }
        });
    }

    @Override
    public void onOrderChanged(OrderEvent event) {
        refreshData();
    }
}
