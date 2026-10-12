package com.pos.view;

import com.pos.controller.*;
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

    public MainDashboard(User currentUser) {
        this.currentUser = currentUser;
        setTitle("Restaurant POS — " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());

        // Header Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(24, 38, 59));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel("RESTAURANT MANAGEMENT SYSTEM");
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

        // Tabbed Pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Panels
        DashboardPanel dashboardPanel = new DashboardPanel(currentUser);
        POSPanel posPanel = new POSPanel(currentUser, orderController, kitchenController, stockController);
        KitchenPanel kitchenPanel = new KitchenPanel(kitchenController, currentUser);

        // Observers wiring
        orderController.attach(dashboardPanel);
        orderController.attach(posPanel);
        orderController.attach(kitchenPanel);
        kitchenController.attach(dashboardPanel);
        kitchenController.attach(posPanel);
        kitchenController.attach(kitchenPanel);
        stockController.attach(posPanel);

        if (currentUser.getRole() == Role.CASHIER) {
            tabs.addTab("Tổng quan", dashboardPanel);
            tabs.addTab("Thu ngân (POS)", posPanel);
            tabs.addTab("Màn hình Bếp", kitchenPanel);
        } else if (currentUser.getRole() == Role.KITCHEN) {
            tabs.addTab("Màn hình Bếp", kitchenPanel);
            InventoryPanel inventoryPanel = new InventoryPanel(currentUser, stockController);
            tabs.addTab("Kho nguyên liệu", inventoryPanel);
            stockController.attach(inventoryPanel);
        } else {
            // Role.MANAGER
            tabs.addTab("Tổng quan", dashboardPanel);
            tabs.addTab("Thu ngân (POS)", posPanel);
            tabs.addTab("Màn hình Bếp", kitchenPanel);
            InventoryPanel inventoryPanel = new InventoryPanel(currentUser, stockController);
            tabs.addTab("Kho nguyên liệu", inventoryPanel);
            stockController.attach(inventoryPanel);

            ReportPanel reportPanel = new ReportPanel(currentUser);
            tabs.addTab("Báo cáo doanh thu", reportPanel);
            orderController.attach(reportPanel);

            UserManagementPanel userPanel = new UserManagementPanel(currentUser);
            tabs.addTab("Quản lý tài khoản", userPanel);
        }

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }
}
