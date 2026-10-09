package com.nhom13.restaurant.managementsys.view;

import com.nhom13.restaurant.managementsys.dao.*;
import com.nhom13.restaurant.managementsys.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Màn hình Phục Vụ (Staff):
 *  Trái    : Danh sách bàn ăn + tìm kiếm / lọc trạng thái
 *  Phải-trên: Thực đơn lọc theo Danh mục + tìm kiếm (có số suất còn lại)
 *  Phải-dưới: Chi tiết hóa đơn đang mở + thêm món + thanh toán
 *
 *  Khi thêm món → deductAvailableQuantity() trừ tồn kho nhà bếp.
 *  Auto-sync 5 giây: cập nhật bàn + thực đơn từ DB.
 */
public class StaffMainFrame extends JFrame {

    private final DiningTableDao tableDao      = new DiningTableDao();
    private final OrderDao       orderDao      = new OrderDao();
    private final MenuItemDao    menuItemDao   = new MenuItemDao();
    private final CategoryDao    categoryDao   = new CategoryDao();
    private final OrderDetailDao detailDao     = new OrderDetailDao();

    // Bàn ăn
    private DefaultTableModel tableModel;
    private JTable tableGrid;
    private TableRowSorter<DefaultTableModel> tableSorter;

    // Thực đơn
    private DefaultTableModel menuModel;
    private JTable menuTable;
    private TableRowSorter<DefaultTableModel> menuSorter;
    private JComboBox<String> cmbCategory;

    // Hóa đơn
    private DefaultTableModel invoiceModel;
    private JLabel lblInvoiceInfo;

    private int currentOrderId = -1;
    private int selectedTableId = -1;
    private javax.swing.Timer syncTimer;

    public StaffMainFrame() {
        setTitle("Restaurant Management – Phục Vụ / POS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 680);
        setLocationRelativeTo(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTablePanel(), buildRightPanel());
        mainSplit.setDividerLocation(370);
        getContentPane().add(mainSplit);

        startSync();
    }

    private void startSync() {
        syncTimer = new javax.swing.Timer(5000, e -> {
            loadTables();
            loadMenuItems();
        });
        syncTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  PANEL TRÁI: BÀN ĂN
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        JLabel title = new JLabel("  🪑 Danh sách bàn ăn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Tên bàn", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableGrid = new JTable(tableModel);
        tableGrid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableSorter = new TableRowSorter<>(tableModel);
        tableGrid.setRowSorter(tableSorter);
        loadTables();

        // Tìm kiếm bàn
        JTextField txtSearch   = new JTextField(10);
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Tất cả", "Empty", "Occupied"});
        JButton btnSearch = new JButton("🔍");
        btnSearch.addActionListener(e -> filterTable(txtSearch.getText().trim(), (String) cmbStatus.getSelectedItem()));
        txtSearch.addActionListener(e -> btnSearch.doClick());
        cmbStatus.addActionListener(e -> btnSearch.doClick());

        JButton btnOpen = new JButton("✅ Mở bàn / Gọi món");
        btnOpen.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOpen.addActionListener(e -> openTable());

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        searchRow.add(new JLabel("Tìm:")); searchRow.add(txtSearch);
        searchRow.add(new JLabel("Lọc:")); searchRow.add(cmbStatus);
        searchRow.add(btnSearch);

        panel.add(new JScrollPane(tableGrid), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(searchRow, BorderLayout.NORTH);
        bottom.add(btnOpen, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void filterTable(String kw, String status) {
        RowFilter<DefaultTableModel, Object> rf1 = kw.isEmpty() ? null : RowFilter.regexFilter("(?i)" + kw, 1);
        RowFilter<DefaultTableModel, Object> rf2 = "Tất cả".equals(status) ? null : RowFilter.regexFilter("(?i)" + status, 2);
        if (rf1==null && rf2==null) tableSorter.setRowFilter(null);
        else if (rf1==null) tableSorter.setRowFilter(rf2);
        else if (rf2==null) tableSorter.setRowFilter(rf1);
        else tableSorter.setRowFilter(RowFilter.andFilter(java.util.Arrays.asList(rf1, rf2)));
    }

    // ═══════════════════════════════════════════════════════════════
    //  PANEL PHẢI: THỰC ĐƠN + HÓA ĐƠN
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JSplitPane vertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildMenuPanel(), buildInvoicePanel());
        vertSplit.setDividerLocation(300);
        panel.add(vertSplit);
        return panel;
    }

    // ── Thực đơn ──
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        JLabel title = new JLabel("  🍜 Thực đơn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        menuModel = new DefaultTableModel(new String[]{"ID", "Danh mục", "Tên món", "Giá (VNĐ)", "Còn lại"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        menuTable = new JTable(menuModel);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuSorter = new TableRowSorter<>(menuModel);
        menuTable.setRowSorter(menuSorter);
        // Lọc danh mục + tìm kiếm
        cmbCategory = new JComboBox<>();
        reloadCategoryCombo();
        cmbCategory.addActionListener(e -> loadMenuItems());

        loadMenuItems();

        JTextField txtSearch = new JTextField(14);
        JButton btnSearch  = new JButton("🔍");
        JButton btnRefresh = new JButton("🔄");

        btnSearch.addActionListener(e -> menuSorter.setRowFilter(
            txtSearch.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtSearch.getText().trim(), 2)));
        txtSearch.addActionListener(e -> btnSearch.doClick());
        btnRefresh.addActionListener(e -> {
            cmbCategory.setSelectedIndex(0);
            txtSearch.setText(""); menuSorter.setRowFilter(null);
            reloadCategoryCombo(); loadMenuItems();
        });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        topBar.add(title);
        topBar.add(new JLabel("Danh mục:")); topBar.add(cmbCategory);
        topBar.add(new JLabel("Tìm:")); topBar.add(txtSearch);
        topBar.add(btnSearch); topBar.add(btnRefresh);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        return panel;
    }

    // ── Hóa đơn ──
    private JPanel buildInvoicePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        lblInvoiceInfo = new JLabel("  📋 Chưa có hóa đơn nào được mở. Chọn bàn và nhấn 'Mở bàn'.");
        lblInvoiceInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));

        invoiceModel = new DefaultTableModel(new String[]{"Tên món", "SL", "Thành tiền (VNĐ)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable invoiceTable = new JTable(invoiceModel);

        // Điều khiển
        JTextField txtQty = new JTextField("1", 5);
        JButton btnAdd      = new JButton("➕ Thêm món đã chọn");
        JButton btnCheckout = new JButton("💰 Thanh toán & Đóng bàn");
        btnCheckout.setBackground(new Color(50, 140, 50));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnAdd.addActionListener(e -> addItemToOrder(txtQty));
        btnCheckout.addActionListener(e -> checkout());

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrlPanel.add(new JLabel("Số lượng:")); ctrlPanel.add(txtQty);
        ctrlPanel.add(btnAdd);
        ctrlPanel.add(Box.createHorizontalStrut(30));
        ctrlPanel.add(btnCheckout);

        panel.add(lblInvoiceInfo, BorderLayout.NORTH);
        panel.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);
        panel.add(ctrlPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOGIC
    // ═══════════════════════════════════════════════════════════════

    private void loadTables() {
        int selId = -1;
        if (tableGrid != null && tableGrid.getSelectedRow() >= 0)
            selId = (int) tableModel.getValueAt(tableGrid.convertRowIndexToModel(tableGrid.getSelectedRow()), 0);
        tableModel.setRowCount(0);
        for (DiningTable t : tableDao.getAllTables())
            tableModel.addRow(new Object[]{t.getTableId(), t.getTableName(), t.getStatus()});
        // Giữ lại lựa chọn
        if (selId > 0) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if ((int)tableModel.getValueAt(i,0) == selId) { tableGrid.setRowSelectionInterval(i,i); break; }
            }
        }
    }

    private void loadMenuItems() {
        menuModel.setRowCount(0);
        List<Category> cats = categoryDao.getAllCategories();
        String sel = (String) cmbCategory.getSelectedItem();
        int catId = (sel == null || sel.startsWith("--")) ? -1 :
            Integer.parseInt(sel.split(" - ")[0].trim());

        List<MenuItem> list = (catId <= 0) ? menuItemDao.getAllMenuItems()
            : menuItemDao.getMenuItemsByCategory(catId);

        for (MenuItem item : list) {
            if (!item.isAvailable()) continue;
            String catName = cats.stream().filter(c -> c.getCategoryId() == item.getCategoryId())
                .map(Category::getCategoryName).findFirst().orElse("-");
            String qtyLabel = item.getAvailableQuantity() > 0
                ? String.valueOf(item.getAvailableQuantity())
                : "⚠️ Hết";
            menuModel.addRow(new Object[]{item.getItemId(), catName, item.getItemName(),
                String.format("%,.0f", item.getPrice()), qtyLabel});
        }
    }

    private void reloadCategoryCombo() {
        String sel = (String) cmbCategory.getSelectedItem();
        cmbCategory.removeAllItems();
        cmbCategory.addItem("-- Tất cả danh mục --");
        for (Category c : categoryDao.getAllCategories())
            cmbCategory.addItem(c.getCategoryId() + " - " + c.getCategoryName());
        if (sel != null) cmbCategory.setSelectedItem(sel);
    }

    private void openTable() {
        int row = tableGrid.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 bàn!"); return; }
        int mr = tableGrid.convertRowIndexToModel(row);
        selectedTableId = (int) tableModel.getValueAt(mr, 0);
        String tableName = tableModel.getValueAt(mr, 1).toString();

        Order pending = orderDao.getPendingOrderByTableId(selectedTableId);
        if (pending == null) {
            // Kiểm tra xem có đơn Cooking chưa
            pending = orderDao.getOrderByTableAndStatus(selectedTableId, "Cooking");
        }
        if (pending == null) {
            Order newOrder = new Order();
            newOrder.setTableId(selectedTableId);
            newOrder.setUserId(2); // Staff user — TODO: truyền userId thực từ login
            newOrder.setTotalAmount(0.0);
            newOrder.setStatus("Pending");
            currentOrderId = orderDao.createOrder(newOrder);
            tableDao.updateTableStatus(selectedTableId, "Occupied");
            loadTables();
        } else {
            currentOrderId = pending.getOrderId();
        }
        loadInvoice();
        lblInvoiceInfo.setText("  📋 Hóa đơn #" + currentOrderId + " | Bàn: " + tableName);
    }

    private void addItemToOrder(JTextField txtQty) {
        if (currentOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Mở bàn trước khi gọi món!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int menuRow = menuTable.getSelectedRow();
        if (menuRow < 0) {
            JOptionPane.showMessageDialog(this, "Chọn món từ bảng thực đơn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            int qty = Integer.parseInt(txtQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();

            int mr       = menuTable.convertRowIndexToModel(menuRow);
            int itemId   = (int) menuModel.getValueAt(mr, 0);
            String qtyLbl = menuModel.getValueAt(mr, 4).toString();
            String itemName = menuModel.getValueAt(mr, 2).toString();

            if ("⚠️ Hết".equals(qtyLbl)) {
                JOptionPane.showMessageDialog(this, "Món '" + itemName + "' đã hết!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Trừ số suất tại nhà bếp
            boolean ok = menuItemDao.deductAvailableQuantity(itemId, qty);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Không đủ suất! Số suất còn lại: " + qtyLbl, "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Lấy giá từ DB
            double price = menuItemDao.getAllMenuItems().stream()
                .filter(m -> m.getItemId() == itemId)
                .mapToDouble(MenuItem::getPrice).findFirst().orElse(0);

            OrderDetail detail = new OrderDetail();
            detail.setOrderId(currentOrderId);
            detail.setItemId(itemId);
            detail.setQuantity(qty);
            detail.setSubtotal(price * qty);
            detailDao.addOrderDetail(detail);

            Order order = orderDao.getPendingOrderByTableId(selectedTableId);
            if (order == null) order = orderDao.getOrderByTableAndStatus(selectedTableId, "Cooking");
            if (order != null) orderDao.updateOrderTotal(currentOrderId, order.getTotalAmount() + detail.getSubtotal());

            loadInvoice();
            loadMenuItems(); // Cập nhật lại số suất còn lại
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkout() {
        if (currentOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Không có hóa đơn nào đang mở!"); return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xác nhận thanh toán hóa đơn #" + currentOrderId + "?",
            "Thanh toán", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            orderDao.updateOrderStatus(currentOrderId, "Paid");
            tableDao.updateTableStatus(selectedTableId, "Empty");
            currentOrderId = -1; selectedTableId = -1;
            invoiceModel.setRowCount(0);
            lblInvoiceInfo.setText("  📋 Đã thanh toán! Chọn bàn mới để tiếp tục.");
            loadTables();
            JOptionPane.showMessageDialog(this, "✅ Thanh toán thành công!");
        }
    }

    private void loadInvoice() {
        invoiceModel.setRowCount(0);
        if (currentOrderId == -1) return;
        List<MenuItem> all = menuItemDao.getAllMenuItems();
        for (OrderDetail d : detailDao.getOrderDetailsByOrderId(currentOrderId)) {
            String name = all.stream().filter(m -> m.getItemId() == d.getItemId())
                .map(MenuItem::getItemName).findFirst().orElse("?");
            invoiceModel.addRow(new Object[]{name, d.getQuantity(), String.format("%,.0f", d.getSubtotal())});
        }
    }
}
