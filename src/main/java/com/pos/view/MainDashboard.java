package com.pos.view;

import com.pos.controller.*;
import com.pos.model.DiningTable;
import com.pos.model.Role;
import com.pos.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainDashboard extends JFrame {
    private final User currentUser;
    private final StockController stockController = new StockController();
    private final OrderController orderController = new OrderController();
    private final KitchenController kitchenController = new KitchenController(stockController);
    private final TableController tableController = new TableController();

    private JTabbedPane tabs;
    private POSPanel posPanel;

    public MainDashboard(User currentUser) {
        this.currentUser = currentUser;
        setTitle("Hệ Thống Quản Lý Nhà Hàng Enterprise v3.0 — " + currentUser.getFullName() + " [" + currentUser.getRole() + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 860);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());

        // Header Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(24, 38, 59));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel("RESTAURANT MANAGEMENT SYSTEM v3.0");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(new Color(255, 214, 102));
        topBar.add(lblTitle, BorderLayout.WEST);

        JLabel lblUser = new JLabel("Tài khoản: " + currentUser.getFullName() + " [" + currentUser.getRole() + "]");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setForeground(new Color(241, 245, 249));

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setBackground(new Color(225, 29, 72));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginForm().setVisible(true);
        });

        JPanel rightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightContainer.setOpaque(false);
        rightContainer.add(lblUser);
        rightContainer.add(btnLogout);
        topBar.add(rightContainer, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        // Tabbed Pane (No parentheses in tab titles)
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Panels
        DashboardPanel dashboardPanel = new DashboardPanel(currentUser);
        posPanel = new POSPanel(currentUser, orderController, kitchenController, stockController);
        KitchenPanel kitchenPanel = new KitchenPanel(kitchenController, currentUser);
        ShiftPanel shiftPanel = new ShiftPanel(currentUser);
        TableReservationPanel tableReservationPanel = new TableReservationPanel(currentUser, tableController, this::navigateToPOS);
        CustomerPanel customerPanel = new CustomerPanel(currentUser);
        VoucherPanel voucherPanel = new VoucherPanel(currentUser);
        InventoryPanel inventoryPanel = new InventoryPanel(currentUser);

        posPanel.setOnNavigateTabRequested(this::navigateToTab);

        // Observers wiring
        orderController.attach(dashboardPanel);
        orderController.attach(posPanel);
        orderController.attach(kitchenPanel);
        kitchenController.attach(dashboardPanel);
        kitchenController.attach(posPanel);
        kitchenController.attach(kitchenPanel);
        stockController.attach(posPanel);

        if (currentUser.getRole() == Role.CASHIER) {
            tabs.addTab("Thu ngân", posPanel);
            tabs.addTab("Sơ đồ & Đặt bàn", tableReservationPanel);
            tabs.addTab("Khách hàng", customerPanel);
            tabs.addTab("Ca làm việc", shiftPanel);
            tabs.addTab("Màn hình Bếp", kitchenPanel);
        } else if (currentUser.getRole() == Role.KITCHEN) {
            tabs.addTab("Màn hình Bếp", kitchenPanel);
            tabs.addTab("Quản lý kho", inventoryPanel);
        } else {
            // Role.MANAGER (Full Enterprise Modules)
            tabs.addTab("Tổng quan", dashboardPanel);
            tabs.addTab("Thu ngân", posPanel);
            tabs.addTab("Sơ đồ & Đặt bàn", tableReservationPanel);
            tabs.addTab("Khách hàng", customerPanel);
            tabs.addTab("Khuyến mãi", voucherPanel);
            tabs.addTab("Quản lý kho", inventoryPanel);
            tabs.addTab("Màn hình Bếp", kitchenPanel);
            tabs.addTab("Ca làm việc", shiftPanel);

            ReportPanel reportPanel = new ReportPanel(currentUser);
            tabs.addTab("Báo cáo doanh thu", reportPanel);
            orderController.attach(reportPanel);

            UserManagementPanel userPanel = new UserManagementPanel(currentUser);
            tabs.addTab("Quản lý tài khoản", userPanel);
        }

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    public void navigateToTab(String title) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equalsIgnoreCase(title) || tabs.getTitleAt(i).contains(title)) {
                tabs.setSelectedIndex(i);
                break;
            }
        }
    }

    public void navigateToPOS(DiningTable table) {
        navigateToTab("Thu ngân");
        if (posPanel != null && table != null) {
            posPanel.selectDiningTable(table);
        }
    }
}
