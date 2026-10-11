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
import java.awt.GridLayout;
import java.util.List;

/**
 * Màn hình Phục Vụ (Staff):
 *  Trái    : Danh sách bàn ăn + tìm kiếm / lọc trạng thái
 *  Phải-trên: Thực đơn lọc theo Danh mục + tìm kiếm (có số suất còn lại)
 *  Phải-dưới: Chi tiết hóa đơn đang mở
 *
 *  Quy trình làm việc (Workflow):
 *  1. Phục vụ mở bàn -> Đơn ở trạng thái 'Ordering' (Đang gọi món).
 *  2. Thêm món vào hóa đơn.
 *  3. Bấm "👨‍🍳 Bắt đầu làm" -> Gửi đơn sang Nhà bếp (chuyển sang 'Pending').
 *  4. Nhà bếp nhận đơn -> Bắt đầu nấu ('Cooking') -> Nấu xong ('Ready').
 *  5. Khi Nhà bếp xong ('Ready') -> Hệ thống cập nhật -> Nút "Thanh toán" mới được bật!
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
    private JTable invoiceTable;
    private JLabel lblInvoiceInfo;
    private JLabel lblOrderStatus;
    private JLabel lblTotal;

    // Các nút thao tác hóa đơn
    private JButton btnAdd;
    private JButton btnRemove;
    private JButton btnSendToKitchen;
    private JButton btnCheckout;

    private int currentOrderId = -1;
    private int selectedTableId = -1;
    private javax.swing.Timer syncTimer;

    public StaffMainFrame() {
        setTitle("Restaurant Management – Phục Vụ / POS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 700);
        setLocationRelativeTo(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTablePanel(), buildRightPanel());
        mainSplit.setDividerLocation(370);
        getContentPane().add(mainSplit);

        startSync();
    }

    private void startSync() {
        // Đồng bộ mỗi 3 giây: cập nhật bàn, món ăn và trạng thái đơn hàng từ nhà bếp
        syncTimer = new javax.swing.Timer(3000, e -> {
            loadTables();
            loadMenuItems();
            if (currentOrderId != -1) {
                updateOrderStatusUI();
            }
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
        if (rf1 == null && rf2 == null) tableSorter.setRowFilter(null);
        else if (rf1 == null) tableSorter.setRowFilter(rf2);
        else if (rf2 == null) tableSorter.setRowFilter(rf1);
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
            txtSearch.setText("");
            menuSorter.setRowFilter(null);
            reloadCategoryCombo();
            loadMenuItems();
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

        lblOrderStatus = new JLabel("  Trạng thái: Chưa có bàn");
        lblOrderStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOrderStatus.setForeground(Color.GRAY);

        lblTotal = new JLabel("  💵 Tổng tiền: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotal.setForeground(new Color(180, 50, 50));

        JButton btnSyncInvoice = new JButton("🔄");
        btnSyncInvoice.setToolTipText("Làm mới trạng thái từ nhà bếp");
        btnSyncInvoice.addActionListener(e -> {
            if (currentOrderId != -1) {
                loadInvoice();
                updateOrderStatusUI();
            }
        });

        JPanel headerLeft = new JPanel(new GridLayout(2, 1, 2, 2));
        headerLeft.add(lblInvoiceInfo);
        headerLeft.add(lblOrderStatus);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(headerLeft, BorderLayout.WEST);
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        headerRight.add(lblTotal);
        headerRight.add(btnSyncInvoice);
        headerPanel.add(headerRight, BorderLayout.EAST);

        invoiceModel = new DefaultTableModel(new String[]{"#", "Tên món", "SL", "Thành tiền (VNĐ)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceTable = new JTable(invoiceModel);

        // Điều khiển bên dưới
        JTextField txtQty = new JTextField("1", 4);
        btnAdd = new JButton("➕ Thêm món");
        btnRemove = new JButton("🗑️ Xóa món đã chọn");
        btnRemove.setForeground(new Color(180, 50, 50));

        // Nút chuyển cho nhà bếp bắt đầu làm
        btnSendToKitchen = new JButton("👨‍🍳 Bắt đầu làm (Gửi bếp)");
        btnSendToKitchen.setBackground(new Color(230, 126, 34));
        btnSendToKitchen.setForeground(Color.WHITE);
        btnSendToKitchen.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Nút thanh toán (chỉ bật khi nhà bếp đã làm xong)
        btnCheckout = new JButton("💰 Thanh toán & Đóng bàn");
        btnCheckout.setBackground(new Color(46, 125, 50));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Mặc định vô hiệu hóa khi chưa mở bàn
        btnAdd.setEnabled(false);
        btnRemove.setEnabled(false);
        btnSendToKitchen.setEnabled(false);
        btnCheckout.setEnabled(false);

        btnAdd.addActionListener(e -> addItemToOrder(txtQty));
        btnRemove.addActionListener(e -> removeItemFromOrder());
        btnSendToKitchen.addActionListener(e -> sendOrderToKitchen());
        btnCheckout.addActionListener(e -> checkout());

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrlPanel.add(new JLabel("Số lượng:"));
        ctrlPanel.add(txtQty);
        ctrlPanel.add(btnAdd);
        ctrlPanel.add(btnRemove);
        ctrlPanel.add(Box.createHorizontalStrut(15));
        ctrlPanel.add(btnSendToKitchen);
        ctrlPanel.add(Box.createHorizontalStrut(15));
        ctrlPanel.add(btnCheckout);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);
        panel.add(ctrlPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOGIC XỬ LÝ ĐƠN HÀNG & NHÀ BẾP
    // ═══════════════════════════════════════════════════════════════

    private void loadTables() {
        int selId = -1;
        if (tableGrid != null && tableGrid.getSelectedRow() >= 0)
            selId = (int) tableModel.getValueAt(tableGrid.convertRowIndexToModel(tableGrid.getSelectedRow()), 0);
        tableModel.setRowCount(0);
        for (DiningTable t : tableDao.getAllTables())
            tableModel.addRow(new Object[]{t.getTableId(), t.getTableName(), t.getStatus()});
        if (selId > 0) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if ((int)tableModel.getValueAt(i, 0) == selId) {
                    tableGrid.setRowSelectionInterval(i, i);
                    break;
                }
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

    /** Mở bàn hoặc nạp đơn hàng đang mở của bàn */
    private void openTable() {
        int row = tableGrid.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 bàn từ danh sách bên trái!");
            return;
        }
        int mr = tableGrid.convertRowIndexToModel(row);
        selectedTableId = (int) tableModel.getValueAt(mr, 0);
        String tableName = tableModel.getValueAt(mr, 1).toString();

        // Tìm đơn hàng chưa thanh toán của bàn
        Order activeOrder = orderDao.getActiveOrderByTableId(selectedTableId);

        if (activeOrder == null) {
            // Tạo đơn mới với trạng thái ban đầu là 'Ordering' (Đang chọn món)
            Order newOrder = new Order();
            newOrder.setTableId(selectedTableId);
            newOrder.setUserId(2);
            newOrder.setTotalAmount(0.0);
            newOrder.setStatus("Ordering");
            currentOrderId = orderDao.createOrder(newOrder);
            tableDao.updateTableStatus(selectedTableId, "Occupied");
            loadTables();
        } else {
            currentOrderId = activeOrder.getOrderId();
        }

        loadInvoice();
        updateOrderStatusUI();
        lblInvoiceInfo.setText("  📋 Hóa đơn #" + currentOrderId + " | Bàn: " + tableName);
    }

    /** Cập nhật giao diện trạng thái và trạng thái các nút bấm */
    private void updateOrderStatusUI() {
        if (currentOrderId == -1) {
            lblOrderStatus.setText("  Trạng thái: Chưa có bàn nào được mở");
            lblOrderStatus.setForeground(Color.GRAY);
            btnAdd.setEnabled(false);
            btnRemove.setEnabled(false);
            btnSendToKitchen.setEnabled(false);
            btnCheckout.setEnabled(false);
            return;
        }

        String status = orderDao.getOrderStatus(currentOrderId);
        int itemCount = invoiceModel.getRowCount();

        if ("Ordering".equalsIgnoreCase(status)) {
            lblOrderStatus.setText("  📝 Trạng thái: Đang gọi món (Chưa gửi nhà bếp)");
            lblOrderStatus.setForeground(new Color(211, 84, 0));
            btnAdd.setEnabled(true);
            btnRemove.setEnabled(itemCount > 0);
            btnSendToKitchen.setEnabled(itemCount > 0);
            btnCheckout.setEnabled(false); // Chưa làm xong, không thể thanh toán!

        } else if ("Pending".equalsIgnoreCase(status)) {
            lblOrderStatus.setText("  ⏳ Trạng thái: Đã gửi nhà bếp (Chờ đầu bếp nhận nấu...)");
            lblOrderStatus.setForeground(new Color(180, 100, 0));
            btnAdd.setEnabled(true);
            btnRemove.setEnabled(true);
            btnSendToKitchen.setEnabled(false); // Đã gửi rồi
            btnCheckout.setEnabled(false); // Đang chờ nấu, chưa thể thanh toán!

        } else if ("Cooking".equalsIgnoreCase(status)) {
            lblOrderStatus.setText("  🔥 Trạng thái: Nhà bếp ĐANG NẤU món...");
            lblOrderStatus.setForeground(new Color(192, 57, 43));
            btnAdd.setEnabled(true);
            btnRemove.setEnabled(false); // Đang nấu trên bếp không thể tự tiện xóa món
            btnSendToKitchen.setEnabled(false);
            btnCheckout.setEnabled(false); // Đang nấu dở, chưa thể thanh toán!

        } else if ("Ready".equalsIgnoreCase(status)) {
            lblOrderStatus.setText("  ✅ Trạng thái: Bếp ĐÃ NẤU XONG! Sẵn sàng thanh toán.");
            lblOrderStatus.setForeground(new Color(39, 174, 96));
            btnAdd.setEnabled(true);
            btnRemove.setEnabled(false);
            btnSendToKitchen.setEnabled(false);
            btnCheckout.setEnabled(true); // ĐÃ NẤU XONG -> ĐƯỢC PHÉP THANH TOÁN!

        } else {
            lblOrderStatus.setText("  Trạng thái: " + status);
            lblOrderStatus.setForeground(Color.DARK_GRAY);
            btnCheckout.setEnabled(false);
        }
    }

    /** Thêm món vào hóa đơn */
    private void addItemToOrder(JTextField txtQty) {
        if (currentOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Mở bàn trước khi gọi món!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int menuRow = menuTable.getSelectedRow();
        if (menuRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món từ bảng thực đơn bên trên!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Món '" + itemName + "' đã hết suất hôm nay!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean ok = menuItemDao.deductAvailableQuantity(itemId, qty);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Không đủ suất! Số suất còn lại: " + qtyLbl, "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double price = menuItemDao.getAllMenuItems().stream()
                .filter(m -> m.getItemId() == itemId)
                .mapToDouble(MenuItem::getPrice).findFirst().orElse(0);

            OrderDetail detail = new OrderDetail();
            detail.setOrderId(currentOrderId);
            detail.setItemId(itemId);
            detail.setQuantity(qty);
            detail.setSubtotal(price * qty);
            detailDao.addOrderDetail(detail);

            // Cập nhật tổng tiền đơn hàng
            Order order = orderDao.getOrderById(currentOrderId);
            double newTotal = (order != null ? order.getTotalAmount() : 0.0) + detail.getSubtotal();
            orderDao.updateOrderTotal(currentOrderId, newTotal);

            // Nếu đơn đã Ready mà khách gọi thêm món, chuyển về 'Ordering' để gửi bếp làm thêm món mới
            String curSt = orderDao.getOrderStatus(currentOrderId);
            if ("Ready".equalsIgnoreCase(curSt)) {
                orderDao.updateOrderStatus(currentOrderId, "Ordering");
            }

            loadInvoice();
            loadMenuItems();
            updateOrderStatusUI();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Xóa món đang được chọn trong bảng hóa đơn */
    private void removeItemFromOrder() {
        if (currentOrderId == -1) return;
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn món cần xóa trong bảng hóa đơn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int detailId = (int) invoiceModel.getValueAt(row, 0);
        String itemName = invoiceModel.getValueAt(row, 1).toString();

        if (JOptionPane.showConfirmDialog(this, "Xác nhận xóa món '" + itemName + "' khỏi hóa đơn?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        List<OrderDetail> details = detailDao.getOrderDetailsByOrderId(currentOrderId);
        OrderDetail toRemove = details.stream().filter(d -> d.getOrderDetailId() == detailId).findFirst().orElse(null);
        if (toRemove != null) {
            // Hoàn trả số suất vào thực đơn
            menuItemDao.setAvailableQuantity(toRemove.getItemId(),
                menuItemDao.getAllMenuItems().stream().filter(m -> m.getItemId() == toRemove.getItemId())
                    .mapToInt(MenuItem::getAvailableQuantity).findFirst().orElse(0) + toRemove.getQuantity());

            // Trừ tổng tiền
            Order order = orderDao.getOrderById(currentOrderId);
            if (order != null) {
                orderDao.updateOrderTotal(currentOrderId, Math.max(0, order.getTotalAmount() - toRemove.getSubtotal()));
            }
        }

        detailDao.deleteOrderDetail(detailId);
        loadInvoice();
        loadMenuItems();
        updateOrderStatusUI();
    }

    /** Gửi đơn hàng sang Nhà Bếp để bắt đầu nấu */
    private void sendOrderToKitchen() {
        if (currentOrderId == -1) return;
        if (invoiceModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Hóa đơn chưa có món ăn nào! Vui lòng thêm món trước khi gửi bếp.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Chuyển trạng thái đơn sang 'Pending' để nhà bếp nhận được
        orderDao.updateOrderStatus(currentOrderId, "Pending");
        updateOrderStatusUI();

        JOptionPane.showMessageDialog(this,
            "👨‍🍳 Đã gửi đơn hàng số #" + currentOrderId + " sang Nhà Bếp!\n" +
            "Đầu bếp sẽ thấy đơn để bắt đầu nấu. Khi bếp nấu xong, bạn mới có thể bấm Thanh toán.",
            "Gửi bếp thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Thanh toán hóa đơn (Chỉ được phép khi nhà bếp đã hoàn thành 'Ready') */
    private void checkout() {
        if (currentOrderId == -1) {
            JOptionPane.showMessageDialog(this, "Không có hóa đơn nào đang mở!");
            return;
        }

        // Kiểm tra chặt chẽ trạng thái từ Database
        String status = orderDao.getOrderStatus(currentOrderId);
        if (!"Ready".equalsIgnoreCase(status)) {
            String desc = "Ordering".equalsIgnoreCase(status) ? "Đang gọi món (Chưa gửi bếp)" :
                          "Pending".equalsIgnoreCase(status) ? "Chờ nhà bếp nhận nấu" :
                          "Cooking".equalsIgnoreCase(status) ? "Nhà bếp đang nấu dở" : status;
            JOptionPane.showMessageDialog(this,
                "⚠️ Nhà bếp chưa nấu xong món ăn!\n" +
                "Trạng thái hiện tại: " + desc + "\n\n" +
                "👉 Bạn chỉ có thể thanh toán sau khi Nhà Bếp hoàn thành món ăn (Trạng thái: Bếp đã nấu xong).",
                "Chưa thể thanh toán", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String totalText = lblTotal.getText();
        if (JOptionPane.showConfirmDialog(this,
                "Xác nhận thanh toán hóa đơn #" + currentOrderId + "?\n" + totalText,
                "Xác nhận thanh toán", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

            orderDao.updateOrderStatus(currentOrderId, "Paid");
            tableDao.updateTableStatus(selectedTableId, "Empty");
            currentOrderId = -1;
            selectedTableId = -1;
            invoiceModel.setRowCount(0);
            lblInvoiceInfo.setText("  📋 Đã thanh toán! Chọn bàn mới để tiếp tục.");
            lblTotal.setText("  💵 Tổng tiền: 0 đ");
            updateOrderStatusUI();
            loadTables();
            JOptionPane.showMessageDialog(this, "✅ Thanh toán thành công và đã đóng bàn!");
        }
    }

    private void loadInvoice() {
        invoiceModel.setRowCount(0);
        if (currentOrderId == -1) {
            lblTotal.setText("  💵 Tổng tiền: 0 đ");
            return;
        }
        List<MenuItem> all = menuItemDao.getAllMenuItems();
        double total = 0;
        for (OrderDetail d : detailDao.getOrderDetailsByOrderId(currentOrderId)) {
            String name = all.stream().filter(m -> m.getItemId() == d.getItemId())
                .map(MenuItem::getItemName).findFirst().orElse("?");
            invoiceModel.addRow(new Object[]{
                d.getOrderDetailId(),
                name,
                d.getQuantity(),
                String.format("%,.0f", d.getSubtotal())
            });
            total += d.getSubtotal();
        }
        lblTotal.setText(String.format("  💵 Tổng tiền: %,.0f đ", total));
    }
}
