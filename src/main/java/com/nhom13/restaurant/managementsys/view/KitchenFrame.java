package com.nhom13.restaurant.managementsys.view;

import com.nhom13.restaurant.managementsys.dao.*;
import com.nhom13.restaurant.managementsys.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Màn hình Nhà Bếp (Kitchen):
 *  Tab 1 – 🔥 Đơn cần nấu   : xem đơn Pending → Bắt đầu nấu → Hoàn thành
 *  Tab 2 – 🍽️ Số suất hôm nay: đặt available_quantity cho từng món
 *  Tab 3 – 📦 Kho nguyên liệu: xem tồn kho, nhập/xuất hàng
 */
public class KitchenFrame extends JFrame {

    private final OrderDao       orderDao       = new OrderDao();
    private final OrderDetailDao detailDao      = new OrderDetailDao();
    private final MenuItemDao    menuItemDao    = new MenuItemDao();
    private final CategoryDao    categoryDao    = new CategoryDao();
    private final IngredientDao  ingredientDao  = new IngredientDao();
    private final DiningTableDao tableDao       = new DiningTableDao();

    // Tab 1 – orders
    private DefaultTableModel orderModel;
    private DefaultTableModel detailModel;
    private int selectedOrderId = -1;

    // Auto-sync timer (5 giây làm mới đơn hàng)
    private javax.swing.Timer syncTimer;

    public KitchenFrame() {
        setTitle("Restaurant Management – Nhà Bếp");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("🔥 Đơn cần nấu",    buildOrderTab());
        tabs.addTab("🍽️ Số suất hôm nay", buildAvailabilityTab());
        tabs.addTab("📦 Kho nguyên liệu", buildIngredientTab());
        getContentPane().add(tabs);

        startSyncTimer();
    }

    private void startSyncTimer() {
        syncTimer = new javax.swing.Timer(5000, e -> refreshOrders());
        syncTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 1: ĐƠN CẦN NẤU
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildOrderTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Bảng đơn hàng
        orderModel = new DefaultTableModel(new String[]{"ID Đơn", "Bàn", "Nhân viên", "Trạng thái", "Thời gian"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable orderTable = new JTable(orderModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Bảng chi tiết đơn hàng
        detailModel = new DefaultTableModel(new String[]{"Tên món", "Số lượng", "Thành tiền"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable detailTable = new JTable(detailModel);

        refreshOrders();

        // Click vào đơn → hiển thị chi tiết
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && orderTable.getSelectedRow() >= 0) {
                int mr = orderTable.convertRowIndexToModel(orderTable.getSelectedRow());
                selectedOrderId = (int) orderModel.getValueAt(mr, 0);
                loadOrderDetails(selectedOrderId);
            }
        });

        // Buttons điều khiển
        JButton btnRefresh   = new JButton("🔄 Làm mới");
        JButton btnStartCook = new JButton("🔥 Bắt đầu nấu");
        JButton btnDone      = new JButton("✅ Hoàn thành / Đã phục vụ");
        btnDone.setBackground(new Color(70, 160, 70));
        btnDone.setForeground(Color.WHITE);

        JLabel lblStatus = new JLabel("  Chọn đơn hàng để xem chi tiết và cập nhật trạng thái.");

        btnRefresh.addActionListener(e -> refreshOrders());

        btnStartCook.addActionListener(e -> {
            if (selectedOrderId < 0) { JOptionPane.showMessageDialog(this, "Chọn đơn hàng!"); return; }
            orderDao.updateOrderStatus(selectedOrderId, "Cooking");
            refreshOrders();
            lblStatus.setText("  🔥 Đơn #" + selectedOrderId + " đang được nấu...");
        });

        btnDone.addActionListener(e -> {
            if (selectedOrderId < 0) { JOptionPane.showMessageDialog(this, "Chọn đơn hàng!"); return; }
            orderDao.updateOrderStatus(selectedOrderId, "Ready");
            refreshOrders();
            detailModel.setRowCount(0);
            selectedOrderId = -1;
            lblStatus.setText("  ✅ Đơn đã hoàn thành và sẵn sàng phục vụ!");
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(btnRefresh); btnPanel.add(btnStartCook); btnPanel.add(btnDone); btnPanel.add(lblStatus);

        // Layout: trái = danh sách đơn, phải = chi tiết
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(480);

        JPanel leftPanel = new JPanel(new BorderLayout(3,3));
        leftPanel.add(new JLabel("  📋 Danh sách đơn hàng (Pending / Cooking)", SwingConstants.LEFT), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(3,3));
        rightPanel.add(new JLabel("  🧾 Chi tiết đơn hàng đã chọn", SwingConstants.LEFT), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);

        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void refreshOrders() {
        orderModel.setRowCount(0);
        // Lấy tất cả đơn Pending hoặc Cooking
        String sql = "SELECT o.order_id, t.table_name, u.full_name, o.status, o.order_time " +
                     "FROM Orders o " +
                     "JOIN DiningTables t ON o.table_id = t.table_id " +
                     "JOIN Users u ON o.user_id = u.user_id " +
                     "WHERE o.status IN ('Pending','Cooking') " +
                     "ORDER BY o.order_time ASC";
        try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection();
             java.sql.Statement s = c.createStatement();
             java.sql.ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                String status = rs.getString("status");
                String statusLabel = "Pending".equals(status) ? "⏳ Chờ nấu" : "🔥 Đang nấu";
                orderModel.addRow(new Object[]{
                    rs.getInt("order_id"), rs.getString("table_name"),
                    rs.getString("full_name"), statusLabel, rs.getTimestamp("order_time")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadOrderDetails(int orderId) {
        detailModel.setRowCount(0);
        List<MenuItem> allItems = menuItemDao.getAllMenuItems();
        for (OrderDetail d : detailDao.getOrderDetailsByOrderId(orderId)) {
            String name = allItems.stream().filter(m -> m.getItemId() == d.getItemId())
                .map(MenuItem::getItemName).findFirst().orElse("ID:" + d.getItemId());
            detailModel.addRow(new Object[]{name, d.getQuantity(), String.format("%,.0f đ", d.getSubtotal())});
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 2: SỐ SUẤT CÓ SẴN HÔM NAY
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildAvailabilityTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"ID", "Danh mục", "Tên món", "Giá", "Số suất còn lại"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtQty    = new JTextField("10", 6);
        JTextField txtSearch = new JTextField(18);

        Runnable reload = () -> {
            model.setRowCount(0);
            List<Category> cats = categoryDao.getAllCategories();
            for (MenuItem item : menuItemDao.getAllMenuItems()) {
                if (!item.isAvailable()) continue;
                String catName = cats.stream().filter(c -> c.getCategoryId() == item.getCategoryId())
                    .map(Category::getCategoryName).findFirst().orElse("-");
                model.addRow(new Object[]{item.getItemId(), catName, item.getItemName(),
                    String.format("%,.0f", item.getPrice()), item.getAvailableQuantity()});
            }
        };
        reload.run();

        JButton btnSet     = new JButton("✏️ Đặt số suất");
        JButton btnRefresh = new JButton("🔄 Làm mới");

        btnSet.addActionListener(e -> {
            int sr = table.getSelectedRow();
            if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn món cần cập nhật số suất!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int itemId = (int) model.getValueAt(mr, 0);
            try {
                int qty = Integer.parseInt(txtQty.getText().trim());
                menuItemDao.setAvailableQuantity(itemId, qty);
                reload.run();
                JOptionPane.showMessageDialog(this, "✅ Đã đặt " + qty + " suất cho món: " + model.getValueAt(mr, 2));
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Số suất phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnRefresh.addActionListener(e -> { txtSearch.setText(""); sorter.setRowFilter(null); reload.run(); });
        txtSearch.addActionListener(e -> sorter.setRowFilter(txtSearch.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtSearch.getText().trim())));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("🔍 Tìm:"));
        topBar.add(txtSearch);
        topBar.add(new JButton("Tìm") {{ addActionListener(ev -> txtSearch.getActionListeners()[0].actionPerformed(null)); }});
        topBar.add(btnRefresh);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Số suất hôm nay:"));
        form.add(txtQty);
        form.add(btnSet);
        form.add(new JLabel("   ⚠️ Phục vụ gọi món sẽ trừ dần số này."));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 3: KHO NGUYÊN LIỆU
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildIngredientTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"ID", "Tên nguyên liệu", "Đơn vị", "Tồn kho", "Tối thiểu", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtAmount = new JTextField("1", 8);

        Runnable reload = () -> {
            model.setRowCount(0);
            for (Ingredient ing : ingredientDao.getAllIngredients())
                model.addRow(new Object[]{ing.getIngredientId(), ing.getIngredientName(), ing.getUnit(),
                    ing.getQuantity(), ing.getMinQuantity(),
                    ing.isLowStock() ? "⚠️ Sắp hết" : "✅ Đủ"});
        };
        reload.run();

        JButton btnAddStock  = new JButton("📥 Nhập hàng (+)");
        JButton btnUseStock  = new JButton("📤 Xuất kho (-)");
        JButton btnRefresh   = new JButton("🔄 Làm mới");

        btnAddStock.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nguyên liệu!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            try {
                double delta = Double.parseDouble(txtAmount.getText().trim());
                ingredientDao.addStock(id, delta); reload.run();
                JOptionPane.showMessageDialog(this, "✅ Đã nhập thêm " + delta + " " + model.getValueAt(mr, 2));
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnUseStock.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nguyên liệu!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            try {
                double delta = Double.parseDouble(txtAmount.getText().trim());
                ingredientDao.useStock(id, delta); reload.run();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnRefresh.addActionListener(e -> reload.run());

        JTextField txtSearch = new JTextField(16);
        txtSearch.addActionListener(e -> sorter.setRowFilter(txtSearch.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtSearch.getText().trim())));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("🔍:")); topBar.add(txtSearch);
        topBar.add(new JButton("Tìm") {{ addActionListener(ev -> txtSearch.getActionListeners()[0].actionPerformed(null)); }});
        topBar.add(btnRefresh);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Số lượng:")); form.add(txtAmount);
        form.add(btnAddStock); form.add(btnUseStock);
        form.add(new JLabel("   ⚠️ Món ⚠️ Sắp hết cần báo Admin nhập thêm hàng!"));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }
}
