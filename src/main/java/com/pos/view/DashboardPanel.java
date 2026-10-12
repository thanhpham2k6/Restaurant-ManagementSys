package com.pos.view;

import com.pos.controller.OrderObserver;
import com.pos.dao.IngredientDAO;
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
    private final IngredientDAO ingredientDAO = new IngredientDAO();

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
        lblKitchenSub = new JLabel("Món đang đợi hoặc nấu");
        cardsGrid.add(createMetricCard("Món chờ chế biến", lblKitchenVal, lblKitchenSub, new Color(245, 158, 11)));

        // Card 4: Low Stock Alert
        lblAlertVal = new JLabel("0");
        lblAlertSub = new JLabel("Cần nhập thêm kho");
        cardsGrid.add(createMetricCard("Cảnh báo nguyên liệu", lblAlertVal, lblAlertSub, new Color(239, 68, 68)));

        centerPanel.add(cardsGrid, BorderLayout.NORTH);

        // Content Area: 2 tables (Recent Orders & Low Stock)
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesPanel.setOpaque(false);

        // Left: Recent Orders
        JPanel pnlOrders = new JPanel(new BorderLayout(0, 8));
        pnlOrders.setBackground(Color.WHITE);
        pnlOrders.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblOrderHeader = new JLabel("Đơn hàng hôm nay");
        lblOrderHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblOrderHeader.setForeground(new Color(30, 41, 59));
        pnlOrders.add(lblOrderHeader, BorderLayout.NORTH);

        String[] orderCols = {"Mã đơn", "Bàn", "Thu ngân", "Tổng tiền", "Trạng thái", "Giờ"};
        recentOrdersModel = new DefaultTableModel(orderCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblOrders = new JTable(recentOrdersModel);
        styleTable(tblOrders);
        pnlOrders.add(new JScrollPane(tblOrders), BorderLayout.CENTER);
        tablesPanel.add(pnlOrders);

        // Right: Low Stock Ingredients
        JPanel pnlStock = new JPanel(new BorderLayout(0, 8));
        pnlStock.setBackground(Color.WHITE);
        pnlStock.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblStockHeader = new JLabel("Nguyên liệu sắp hết / Cần nhập");
        lblStockHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblStockHeader.setForeground(new Color(30, 41, 59));
        pnlStock.add(lblStockHeader, BorderLayout.NORTH);

        String[] stockCols = {"Tên nguyên liệu", "Tồn kho", "Mức tối thiểu", "Đơn vị", "Tình trạng"};
        lowStockModel = new DefaultTableModel(stockCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblStock = new JTable(lowStockModel);
        styleTable(tblStock);
        pnlStock.add(new JScrollPane(tblStock), BorderLayout.CENTER);
        tablesPanel.add(pnlStock);

        centerPanel.add(tablesPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valLabel, JLabel subLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        // Top accent line & title
        JPanel topBox = new JPanel(new BorderLayout());
        topBox.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 116, 139));
        topBox.add(lblTitle, BorderLayout.WEST);

        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(10, 10));
        dot.setBackground(accentColor);
        dot.setBorder(new LineBorder(accentColor, 1, true));
        topBox.add(dot, BorderLayout.EAST);
        card.add(topBox, BorderLayout.NORTH);

        // Value
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLabel.setForeground(new Color(15, 23, 42));
        card.add(valLabel, BorderLayout.CENTER);

        // Subtitle
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));
        card.add(subLabel, BorderLayout.SOUTH);

        return card;
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
        SwingUtilities.invokeLater(() -> {
            try {
                loadMetrics();
                loadRecentOrders();
                loadLowStock();
            } catch (Exception e) {
                // Ignore connection blip
            }
        });
    }

    private void loadMetrics() {
        // Today's revenue
        BigDecimal todayTotal = BigDecimal.ZERO;
        int paidCount = 0;
        String sqlRev = "SELECT COUNT(*) as cnt, COALESCE(SUM(total_amount), 0) as total " +
                        "FROM orders WHERE status='PAID' AND CAST(paid_at AS DATE) = CAST(GETDATE() AS DATE)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sqlRev)) {
            if (rs.next()) {
                paidCount = rs.getInt("cnt");
                todayTotal = rs.getBigDecimal("total");
            }
        } catch (Exception ignored) {}

        lblRevenueVal.setText(CurrencyFormat.format(todayTotal));
        lblRevenueSub.setText(paidCount + " đơn đã thanh toán hôm nay");

        // Dining tables
        List<DiningTable> tables = tableDAO.findAll();
        long occupied = tables.stream().filter(t -> t.getStatus() == TableStatus.OCCUPIED).count();
        lblTableVal.setText(occupied + " / " + tables.size());
        lblTableSub.setText(occupied + " bàn đang có khách ngồi");

        // Kitchen active items
        int kitchenCount = 0;
        String sqlKitchen = "SELECT COUNT(*) as cnt FROM order_details od JOIN orders o ON od.order_id = o.id " +
                            "WHERE od.sent_to_kitchen = 1 AND od.status IN ('PENDING','ACCEPTED','COOKING') AND o.status='OPEN'";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sqlKitchen)) {
            if (rs.next()) kitchenCount = rs.getInt("cnt");
        } catch (Exception ignored) {}
        lblKitchenVal.setText(String.valueOf(kitchenCount));
        lblKitchenSub.setText(kitchenCount > 0 ? "Món đang chờ chế biến" : "Bếp đã hoàn tất các món");

        // Low stock count
        List<Ingredient> lowStocks = ingredientDAO.findLowStock();
        lblAlertVal.setText(String.valueOf(lowStocks.size()));
        lblAlertSub.setText(lowStocks.isEmpty() ? "Kho ổn định, đủ định mức" : lowStocks.size() + " nguyên liệu cần nhập");
    }

    private void loadRecentOrders() {
        recentOrdersModel.setRowCount(0);
        String sql = "SELECT TOP 15 o.id, t.name as table_name, u.full_name as cashier_name, " +
                     "o.total_amount, o.status, COALESCE(o.paid_at, o.created_at) as order_time " +
                     "FROM orders o " +
                     "JOIN dining_tables t ON o.table_id = t.id " +
                     "JOIN users u ON o.user_id = u.id " +
                     "ORDER BY o.id DESC";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String table = rs.getString("table_name");
                String cashier = rs.getString("cashier_name");
                BigDecimal total = rs.getBigDecimal("total_amount");
                String status = rs.getString("status");
                String statusDisplay = switch (status) {
                    case "PAID" -> "Đã thanh toán";
                    case "OPEN" -> "Đang phục vụ";
                    case "CANCELLED" -> "Đã hủy";
                    default -> status;
                };
                java.sql.Timestamp ts = rs.getTimestamp("order_time");
                String timeStr = ts != null ? DateUtils.format(ts.toLocalDateTime()) : "";

                recentOrdersModel.addRow(new Object[]{
                    "#" + id, table, cashier, CurrencyFormat.format(total), statusDisplay, timeStr
                });
            }
        } catch (Exception ignored) {}
    }

    private void loadLowStock() {
        lowStockModel.setRowCount(0);
        List<Ingredient> list = ingredientDAO.findAll();
        for (Ingredient ing : list) {
            boolean isLow = ing.isLowStock();
            String status = isLow ? "Sắp hết hàng" : "Đủ dùng";
            lowStockModel.addRow(new Object[]{
                ing.getName(),
                ing.getStockQty() + " " + ing.getUnit(),
                ing.getMinQty() + " " + ing.getUnit(),
                ing.getUnit(),
                status
            });
        }
    }

    @Override
    public void onOrderChanged(OrderEvent event) {
        refreshData();
    }
}
