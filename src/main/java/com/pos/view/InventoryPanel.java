package com.pos.view;

import com.pos.controller.InventoryController;
import com.pos.controller.StockObserver;
import com.pos.model.*;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DateUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Quản lý Kho hàng hóa (Inventory Management).
 * 4 Phân hệ con: Tồn kho, Xuất nhập kho, Kiểm kho cân bằng, Nhà cung cấp.
 */
public class InventoryPanel extends JPanel implements StockObserver {
    private final User currentUser;
    private final InventoryController inventoryController;

    private JTabbedPane tabbedPane;

    // Tab 1: Items
    private JTable itemTable;
    private DefaultTableModel itemModel;
    private List<InventoryItem> itemsList;
    private JLabel lblLowStockWarning;

    // Tab 2: Movements
    private JTable movementTable;
    private DefaultTableModel movementModel;
    private List<StockMovement> movementsList;

    // Tab 3: Audits
    private JTable auditTable;
    private DefaultTableModel auditModel;

    // Tab 4: Suppliers
    private JTable supplierTable;
    private DefaultTableModel supplierModel;
    private List<Supplier> suppliersList;

    public InventoryPanel(User currentUser) {
        this.currentUser = currentUser;
        this.inventoryController = new InventoryController();
        this.inventoryController.attach(this);
        setLayout(new BorderLayout());
        initComponents();
        loadAllDataAsync();
    }

    public InventoryPanel(User currentUser, com.pos.controller.StockController oldStock) {
        this(currentUser);
    }

    @Override
    public void onStockChanged(int menuItemId) {
        SwingUtilities.invokeLater(this::loadAllDataAsync);
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("Tồn kho hàng hóa", createItemsTab());
        tabbedPane.addTab("Nhập / Xuất kho", createMovementsTab());
        tabbedPane.addTab("Kiểm kho (Stocktake)", createAuditsTab());
        tabbedPane.addTab("Nhà cung cấp", createSuppliersTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== TAB 1: INVENTORY ITEMS ====================
    private JPanel createItemsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(245, 247, 250));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        lblLowStockWarning = new JLabel("Đang tải dữ liệu...");
        lblLowStockWarning.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLowStockWarning.setForeground(new Color(211, 47, 47));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnEdit = createButton("Sửa", new Color(230, 81, 0));
        JButton btnDelete = createButton("Xóa đã chọn", new Color(211, 47, 47));
        JButton btnRefresh = createButton("Làm mới", new Color(120, 144, 156));

        btnEdit.addActionListener(e -> showEditItemDialog());
        btnDelete.addActionListener(e -> deleteSelectedItem());
        btnRefresh.addActionListener(e -> loadItemsAsync());

        btnBar.add(btnEdit);
        btnBar.add(btnDelete);
        btnBar.add(btnRefresh);

        topBar.add(lblLowStockWarning, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Chọn", "ID", "Tên hàng hóa", "Danh mục", "Đơn vị tính", "Giá vốn (VND)", "Tồn kho", "Định mức tối thiểu", "Nhà cung cấp", "Cảnh báo"};
        itemModel = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int col) {
                if (col == 0) return Boolean.class;
                return super.getColumnClass(col);
            }
            @Override public boolean isCellEditable(int r, int c) {
                return c == 0;
            }
        };
        itemTable = new JTable(itemModel);
        itemTable.setRowHeight(30);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        itemTable.setAutoCreateRowSorter(true);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        itemTable.getColumnModel().getColumn(0).setMaxWidth(60);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        return panel;
    }

    // ==================== TAB 2: STOCK MOVEMENTS ====================
    private JPanel createMovementsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(245, 247, 250));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnImport = createButton("+ Nhập hàng (NCC)", new Color(46, 125, 50));
        JButton btnExport = createButton("- Xuất hao hụt/hủy", new Color(230, 81, 0));
        JButton btnAdjust = createButton("Điều chỉnh tồn kho", new Color(30, 136, 229));
        JButton btnPrintSlip = createButton("📄 In/Xem Phiếu Nhập - Xuất", new Color(139, 92, 246));
        JButton btnRefresh = createButton("Làm mới", new Color(120, 144, 156));

        btnImport.addActionListener(e -> showImportDialog());
        btnExport.addActionListener(e -> showExportDialog());
        btnAdjust.addActionListener(e -> showAdjustDialog());
        btnPrintSlip.addActionListener(e -> showMovementSlipDialog());
        btnRefresh.addActionListener(e -> loadMovementsAsync());

        topBar.add(btnImport);
        topBar.add(btnExport);
        topBar.add(btnAdjust);
        topBar.add(btnPrintSlip);
        topBar.add(btnRefresh);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Thời gian", "Hàng hóa", "Loại biến động", "Số lượng", "Đơn giá", "Nhà cung cấp", "Người thực hiện", "Ghi chú"};
        movementModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        movementTable = new JTable(movementModel);
        movementTable.setRowHeight(30);
        movementTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        movementTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        movementTable.setAutoCreateRowSorter(true);

        panel.add(new JScrollPane(movementTable), BorderLayout.CENTER);
        return panel;
    }

    // ==================== TAB 3: INVENTORY AUDITS (KIỂM KHO) ====================
    private JPanel createAuditsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(245, 247, 250));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel desc = new JLabel("Lịch sử các đợt đối soát & cân bằng tồn kho thực tế");
        desc.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnStartAudit = createButton("+ Tạo đợt kiểm kho mới", new Color(142, 36, 170));
        JButton btnViewDetail = createButton("Xem chi tiết", new Color(30, 136, 229));
        JButton btnRefresh = createButton("Làm mới", new Color(120, 144, 156));

        btnStartAudit.addActionListener(e -> showStocktakeDialog());
        btnViewDetail.addActionListener(e -> showAuditDetailsDialog());
        btnRefresh.addActionListener(e -> loadAuditsAsync());

        btnBar.add(btnStartAudit);
        btnBar.add(btnViewDetail);
        btnBar.add(btnRefresh);

        topBar.add(desc, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Mã kiểm kho", "Người kiểm", "Ngày kiểm", "Trạng thái", "Ghi chú"};
        auditModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditModel);
        auditTable.setRowHeight(30);
        auditTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        auditTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        auditTable.setAutoCreateRowSorter(true);
        auditTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(new JScrollPane(auditTable), BorderLayout.CENTER);
        return panel;
    }

    // ==================== TAB 4: SUPPLIERS ====================
    private JPanel createSuppliersTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(245, 247, 250));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnAdd = createButton("+ Thêm Nhà cung cấp", new Color(46, 125, 50));
        JButton btnEdit = createButton("Sửa", new Color(230, 81, 0));
        JButton btnDelete = createButton("Xóa", new Color(211, 47, 47));
        JButton btnRefresh = createButton("Làm mới", new Color(120, 144, 156));

        btnAdd.addActionListener(e -> showAddSupplierDialog());
        btnEdit.addActionListener(e -> showEditSupplierDialog());
        btnDelete.addActionListener(e -> deleteSelectedSupplier());
        btnRefresh.addActionListener(e -> loadSuppliersAsync());

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        topBar.add(btnRefresh);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Tên Nhà Cung Cấp", "Người liên hệ", "Số điện thoại", "Email", "Địa chỉ"};
        supplierModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        supplierTable = new JTable(supplierModel);
        supplierTable.setRowHeight(30);
        supplierTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        supplierTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        supplierTable.setAutoCreateRowSorter(true);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(new JScrollPane(supplierTable), BorderLayout.CENTER);
        return panel;
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ==================== ASYNC LOADERS ====================
    public void loadAllDataAsync() {
        loadItemsAsync();
        loadMovementsAsync();
        loadAuditsAsync();
        loadSuppliersAsync();
    }

    private void loadItemsAsync() {
        new SwingWorker<List<InventoryItem>, Void>() {
            @Override protected List<InventoryItem> doInBackground() { return inventoryController.getActiveItems(); }
            @Override protected void done() {
                try {
                    itemsList = get();
                    itemModel.setRowCount(0);
                    int lowCount = 0;
                    for (InventoryItem i : itemsList) {
                        boolean isLow = i.isLowStock();
                        if (isLow) lowCount++;
                        itemModel.addRow(new Object[]{
                            Boolean.FALSE,
                            i.getId(),
                            i.getName(),
                            i.getCategory() != null ? i.getCategory() : "-",
                            i.getUnit(),
                            CurrencyFormat.format(i.getCostPrice()),
                            i.getStockQty() + " " + i.getUnit(),
                            i.getMinQty() + " " + i.getUnit(),
                            i.getSupplierName() != null ? i.getSupplierName() : "-",
                            isLow ? "SẮP HẾT HÀNG!" : "Bình thường"
                        });
                    }
                    if (lowCount > 0) {
                        lblLowStockWarning.setText("CẢNH BÁO: Có " + lowCount + " mặt hàng dưới định mức tồn tối thiểu!");
                    } else {
                        lblLowStockWarning.setText("Tất cả mặt hàng đang đủ số lượng an toàn.");
                        lblLowStockWarning.setForeground(new Color(46, 125, 50));
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadMovementsAsync() {
        new SwingWorker<List<StockMovement>, Void>() {
            @Override protected List<StockMovement> doInBackground() { return inventoryController.getAllMovements(); }
            @Override protected void done() {
                try {
                    List<StockMovement> list = get();
                    movementsList = list;
                    movementModel.setRowCount(0);
                    for (StockMovement sm : list) {
                        movementModel.addRow(new Object[]{
                            sm.getId(),
                            sm.getCreatedAt() != null ? DateUtils.format(sm.getCreatedAt()) : "-",
                            sm.getItemName(),
                            sm.getType().getDisplayName(),
                            sm.getQuantity(),
                            CurrencyFormat.format(sm.getUnitPrice()),
                            sm.getSupplierName() != null ? sm.getSupplierName() : "-",
                            sm.getUserFullName() != null ? sm.getUserFullName() : "-",
                            sm.getNote() != null ? sm.getNote() : ""
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadAuditsAsync() {
        new SwingWorker<List<InventoryAudit>, Void>() {
            @Override protected List<InventoryAudit> doInBackground() { return inventoryController.getAllAudits(); }
            @Override protected void done() {
                try {
                    List<InventoryAudit> list = get();
                    auditModel.setRowCount(0);
                    for (InventoryAudit a : list) {
                        auditModel.addRow(new Object[]{
                            a.getId(),
                            a.getAuditCode(),
                            a.getUserFullName(),
                            a.getAuditDate() != null ? DateUtils.format(a.getAuditDate()) : "-",
                            a.getStatus().getDisplayName(),
                            a.getNote() != null ? a.getNote() : ""
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadSuppliersAsync() {
        new SwingWorker<List<Supplier>, Void>() {
            @Override protected List<Supplier> doInBackground() { return inventoryController.getActiveSuppliers(); }
            @Override protected void done() {
                try {
                    suppliersList = get();
                    supplierModel.setRowCount(0);
                    for (Supplier s : suppliersList) {
                        supplierModel.addRow(new Object[]{
                            s.getId(),
                            s.getName(),
                            s.getContactPerson() != null ? s.getContactPerson() : "-",
                            s.getPhone() != null ? s.getPhone() : "-",
                            s.getEmail() != null ? s.getEmail() : "-",
                            s.getAddress() != null ? s.getAddress() : "-"
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ==================== DIALOGS & ACTIONS ====================
    private void showAddItemDialog() {
        JTextField txtName = new JTextField();
        JTextField txtCat = new JTextField("Đồ uống");
        JTextField txtUnit = new JTextField("lon");
        JTextField txtCost = new JTextField("15000");
        JTextField txtMin = new JTextField("5");
        JTextField txtInitQty = new JTextField("20");

        List<Supplier> sups = inventoryController.getActiveSuppliers();
        JComboBox<Supplier> cbSup = new JComboBox<>(sups.toArray(new Supplier[0]));

        JPanel p = new JPanel(new GridLayout(7, 2, 8, 8));
        p.add(new JLabel("Tên mặt hàng:")); p.add(txtName);
        p.add(new JLabel("Nhà cung cấp:")); p.add(cbSup);
        p.add(new JLabel("Phân loại danh mục:")); p.add(txtCat);
        p.add(new JLabel("Đơn vị tính:")); p.add(txtUnit);
        p.add(new JLabel("Giá vốn (VND):")); p.add(txtCost);
        p.add(new JLabel("Tồn ban đầu:")); p.add(txtInitQty);
        p.add(new JLabel("Định mức tồn tối thiểu:")); p.add(txtMin);

        int res = JOptionPane.showConfirmDialog(this, p, "Thêm mặt hàng kho mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = new InventoryItem();
                item.setName(txtName.getText().trim());
                if (cbSup.getSelectedItem() != null) {
                    item.setSupplierId(((Supplier) cbSup.getSelectedItem()).getId());
                }
                item.setCategory(txtCat.getText().trim());
                item.setUnit(txtUnit.getText().trim());
                item.setCostPrice(new BigDecimal(txtCost.getText().trim()));
                item.setStockQty(new BigDecimal(txtInitQty.getText().trim()));
                item.setMinQty(new BigDecimal(txtMin.getText().trim()));

                inventoryController.saveItem(item);
                JOptionPane.showMessageDialog(this, "Thêm mặt hàng thành công!");
                loadItemsAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditItemDialog() {
        int viewRow = itemTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng mặt hàng cần sửa.");
            return;
        }
        int r = itemTable.convertRowIndexToModel(viewRow);
        int id = (int) itemModel.getValueAt(r, 1);
        InventoryItem item = inventoryController.getItemById(id);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin mặt hàng.");
            return;
        }

        JTextField txtName = new JTextField(item.getName());
        JTextField txtCat = new JTextField(item.getCategory() != null ? item.getCategory() : "");
        JTextField txtUnit = new JTextField(item.getUnit());
        JTextField txtCost = new JTextField(item.getCostPrice().toPlainString());
        JTextField txtMin = new JTextField(item.getMinQty().toPlainString());

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Tên mặt hàng:")); p.add(txtName);
        p.add(new JLabel("Danh mục:")); p.add(txtCat);
        p.add(new JLabel("Đơn vị tính:")); p.add(txtUnit);
        p.add(new JLabel("Giá vốn:")); p.add(txtCost);
        p.add(new JLabel("Định mức tối thiểu:")); p.add(txtMin);

        int res = JOptionPane.showConfirmDialog(this, p, "Chỉnh sửa mặt hàng #" + item.getId(), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                item.setName(txtName.getText().trim());
                item.setCategory(txtCat.getText().trim());
                item.setUnit(txtUnit.getText().trim());
                item.setCostPrice(new BigDecimal(txtCost.getText().trim()));
                item.setMinQty(new BigDecimal(txtMin.getText().trim()));
                inventoryController.saveItem(item);
                loadItemsAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        }
    }

    private void deleteSelectedItem() {
        java.util.List<Integer> idsToDelete = new java.util.ArrayList<>();
        java.util.List<String> namesToDelete = new java.util.ArrayList<>();
        for (int r = 0; r < itemModel.getRowCount(); r++) {
            Object checked = itemModel.getValueAt(r, 0);
            if (Boolean.TRUE.equals(checked)) {
                idsToDelete.add((int) itemModel.getValueAt(r, 1));
                namesToDelete.add((String) itemModel.getValueAt(r, 2));
            }
        }
        if (idsToDelete.isEmpty()) {
            int viewRow = itemTable.getSelectedRow();
            if (viewRow >= 0) {
                int r = itemTable.convertRowIndexToModel(viewRow);
                idsToDelete.add((int) itemModel.getValueAt(r, 1));
                namesToDelete.add((String) itemModel.getValueAt(r, 2));
            }
        }
        if (idsToDelete.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tích chọn ô vuông checkbox hoặc chọn mặt hàng cần xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String msg = idsToDelete.size() == 1
            ? "Bạn có chắc muốn xóa mặt hàng '" + namesToDelete.get(0) + "'?"
            : "Bạn có chắc muốn xóa " + idsToDelete.size() + " mặt hàng đã được tích chọn khỏi kho?";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Xác nhận xóa hàng hóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            for (int id : idsToDelete) {
                inventoryController.deleteItem(id);
            }
            JOptionPane.showMessageDialog(this, "Đã xóa thành công " + idsToDelete.size() + " mặt hàng!");
            loadItemsAsync();
        }
    }

    private void showImportDialog() {
        JRadioButton rbExisting = new JRadioButton("Chọn mặt hàng có sẵn trong kho", itemsList != null && !itemsList.isEmpty());
        JRadioButton rbNew = new JRadioButton("Nhập mặt hàng MỚI (Tự động thêm vào kho)", itemsList == null || itemsList.isEmpty());
        ButtonGroup bgMode = new ButtonGroup();
        bgMode.add(rbExisting);
        bgMode.add(rbNew);

        if (itemsList == null || itemsList.isEmpty()) {
            rbExisting.setEnabled(false);
            rbNew.setSelected(true);
        }

        JComboBox<InventoryItem> cbItem = new JComboBox<>();
        if (itemsList != null) {
            for (InventoryItem i : itemsList) cbItem.addItem(i);
        }

        JTextField txtNewName = new JTextField();
        JTextField txtNewCat = new JTextField("Đồ uống");
        JTextField txtNewUnit = new JTextField("lon");
        JTextField txtNewMin = new JTextField("5");

        List<Supplier> sups = inventoryController.getActiveSuppliers();
        JComboBox<Supplier> cbSup = new JComboBox<>(sups.toArray(new Supplier[0]));
        JTextField txtQty = new JTextField("10");
        JTextField txtPrice = new JTextField("15000");
        JTextField txtNote = new JTextField("Nhập hàng từ NCC");

        Runnable updateFormState = () -> {
            boolean isExisting = rbExisting.isSelected();
            cbItem.setEnabled(isExisting);
            txtNewName.setEnabled(!isExisting);
            txtNewCat.setEnabled(!isExisting);
            txtNewUnit.setEnabled(!isExisting);
            txtNewMin.setEnabled(!isExisting);
        };
        rbExisting.addActionListener(e -> updateFormState.run());
        rbNew.addActionListener(e -> updateFormState.run());
        updateFormState.run();

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Mode selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JPanel modeP = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        modeP.add(rbExisting);
        modeP.add(rbNew);
        p.add(modeP, gbc);

        // Row 1: Existing item
        gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.gridx = 0; p.add(new JLabel("Mặt hàng có sẵn:"), gbc);
        gbc.gridx = 1; p.add(cbItem, gbc);

        // Row 2: New item name
        gbc.gridy = 2;
        gbc.gridx = 0; p.add(new JLabel("Tên mặt hàng MỚI:"), gbc);
        gbc.gridx = 1; p.add(txtNewName, gbc);

        // Row 3: New item category & unit
        gbc.gridy = 3;
        gbc.gridx = 0; p.add(new JLabel("Danh mục / Đơn vị tính:"), gbc);
        JPanel subP = new JPanel(new GridLayout(1, 2, 4, 0));
        subP.add(txtNewCat);
        subP.add(txtNewUnit);
        gbc.gridx = 1; p.add(subP, gbc);

        // Row 4: New item min qty
        gbc.gridy = 4;
        gbc.gridx = 0; p.add(new JLabel("Định mức tồn tối thiểu:"), gbc);
        gbc.gridx = 1; p.add(txtNewMin, gbc);

        // Row 5: Supplier
        gbc.gridy = 5;
        gbc.gridx = 0; p.add(new JLabel("Nhà cung cấp:"), gbc);
        gbc.gridx = 1; p.add(cbSup, gbc);

        // Row 6: Quantity & Price
        gbc.gridy = 6;
        gbc.gridx = 0; p.add(new JLabel("Số lượng & Đơn giá nhập:"), gbc);
        JPanel qp = new JPanel(new GridLayout(1, 2, 4, 0));
        qp.add(txtQty);
        qp.add(txtPrice);
        gbc.gridx = 1; p.add(qp, gbc);

        // Row 7: Note
        gbc.gridy = 7;
        gbc.gridx = 0; p.add(new JLabel("Ghi chú hóa đơn / chứng từ:"), gbc);
        gbc.gridx = 1; p.add(txtNote, gbc);

        int res = JOptionPane.showConfirmDialog(this, p, "Phiếu Nhập Hàng Vào Kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Supplier s = (Supplier) cbSup.getSelectedItem();
                BigDecimal qty = new BigDecimal(txtQty.getText().trim());
                BigDecimal price = new BigDecimal(txtPrice.getText().trim());
                int itemId;

                if (rbNew.isSelected()) {
                    String newName = txtNewName.getText().trim();
                    if (newName.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Vui lòng nhập tên mặt hàng mới!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    InventoryItem newItem = new InventoryItem();
                    newItem.setName(newName);
                    newItem.setCategory(txtNewCat.getText().trim().isEmpty() ? "Đồ uống" : txtNewCat.getText().trim());
                    newItem.setUnit(txtNewUnit.getText().trim().isEmpty() ? "lon" : txtNewUnit.getText().trim());
                    newItem.setCostPrice(price);
                    newItem.setStockQty(BigDecimal.ZERO);
                    newItem.setMinQty(new BigDecimal(txtNewMin.getText().trim().isEmpty() ? "5" : txtNewMin.getText().trim()));
                    if (s != null) newItem.setSupplierId(s.getId());
                    newItem.setActive(true);
                    itemId = inventoryController.saveItem(newItem);
                } else {
                    InventoryItem item = (InventoryItem) cbItem.getSelectedItem();
                    if (item == null) {
                        JOptionPane.showMessageDialog(this, "Chưa chọn mặt hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    itemId = item.getId();
                }

                inventoryController.importStock(itemId, s != null ? s.getId() : null, qty, price, currentUser, txtNote.getText().trim());
                loadAllDataAsync();
                int pOpt = JOptionPane.showConfirmDialog(this,
                    "Nhập kho thành công!\nBạn có muốn xem và in Phiếu Nhập Kho chứng từ giấy tờ ngay bây giờ không?",
                    "In Chứng Từ Giấy Tờ", JOptionPane.YES_NO_OPTION);
                if (pOpt == JOptionPane.YES_OPTION) {
                    List<StockMovement> movs = inventoryController.getAllMovements();
                    if (!movs.isEmpty()) {
                        showMovementSlipDialog(movs.get(0));
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showExportDialog() {
        if (itemsList == null || itemsList.isEmpty()) return;
        JComboBox<InventoryItem> cbItem = new JComboBox<>(itemsList.toArray(new InventoryItem[0]));
        JTextField txtQty = new JTextField("1");
        JTextField txtNote = new JTextField("Hỏng / Hết hạn");

        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Chọn mặt hàng:")); p.add(cbItem);
        p.add(new JLabel("Số lượng xuất:")); p.add(txtQty);
        p.add(new JLabel("Lý do xuất:")); p.add(txtNote);

        int res = JOptionPane.showConfirmDialog(this, p, "Phiếu Xuất Hàng Khỏi Kho", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = (InventoryItem) cbItem.getSelectedItem();
                BigDecimal qty = new BigDecimal(txtQty.getText().trim());
                inventoryController.exportStock(item.getId(), qty, currentUser, txtNote.getText().trim());
                JOptionPane.showMessageDialog(this, "Xuất kho thành công!");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAdjustDialog() {
        if (itemsList == null || itemsList.isEmpty()) return;
        JComboBox<InventoryItem> cbItem = new JComboBox<>(itemsList.toArray(new InventoryItem[0]));
        JTextField txtNewQty = new JTextField("0");
        JTextField txtNote = new JTextField("Cân bằng tồn kho");

        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Chọn mặt hàng:")); p.add(cbItem);
        p.add(new JLabel("Số lượng tồn mới:")); p.add(txtNewQty);
        p.add(new JLabel("Ghi chú:")); p.add(txtNote);

        int res = JOptionPane.showConfirmDialog(this, p, "Điều chỉnh số tồn kho", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = (InventoryItem) cbItem.getSelectedItem();
                BigDecimal newQty = new BigDecimal(txtNewQty.getText().trim());
                inventoryController.adjustStock(item.getId(), newQty, currentUser, txtNote.getText().trim());
                JOptionPane.showMessageDialog(this, "Điều chỉnh thành công!");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        }
    }

    // ==================== IN/XEM CHỨNG TỪ GIẤY TỜ NHẬP XUẤT ====================
    private void showMovementSlipDialog() {
        int viewRow = movementTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng lịch sử nhập/xuất kho để in phiếu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = movementTable.convertRowIndexToModel(viewRow);
        int movId = (int) movementModel.getValueAt(r, 0);
        StockMovement sm = movementsList != null ? movementsList.stream().filter(m -> m.getId() == movId).findFirst().orElse(null) : null;
        if (sm != null) showMovementSlipDialog(sm);
    }

    private void showMovementSlipDialog(StockMovement sm) {
        if (sm == null) return;
        String typeTitle = switch (sm.getType()) {
            case IMPORT -> "PHIẾU NHẬP KHO HÀNG HÓA";
            case EXPORT -> "PHIẾU XUẤT KHO / HAO HỤT";
            case RETURN -> "PHIẾU TRẢ HÀNG NHÀ CUNG CẤP";
            default -> "BIÊN BẢN ĐIỀU CHỈNH TỒN KHO";
        };
        String prefix = sm.getType() == MovementType.IMPORT ? "PNK-" : "PXK-";
        String slipCode = prefix + String.format("%05d", sm.getId());
        BigDecimal unitPrice = sm.getUnitPrice() != null ? sm.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalVal = sm.getQuantity().multiply(unitPrice);

        String slip =
            "======================================================================\n" +
            "                   HỆ THỐNG QUẢN LÝ NHÀ HÀNG POS                      \n" +
            "          Địa chỉ: 123 Đường Ẩm Thực, Quận 1, TP. Hồ Chí Minh         \n" +
            "                 Hotline: 1900 6868 - Email: kho@pos.vn               \n" +
            "----------------------------------------------------------------------\n" +
            "                       " + typeTitle + "\n" +
            "                           Mã số: " + slipCode + "\n\n" +
            "  * Ngày lập phiếu : " + (sm.getCreatedAt() != null ? DateUtils.format(sm.getCreatedAt()) : "-") + "\n" +
            "  * Người thực hiện: " + (sm.getUserFullName() != null ? sm.getUserFullName() : currentUser.getFullName()) + "\n" +
            "  * Đối tác / NCC  : " + (sm.getSupplierName() != null ? sm.getSupplierName() : "(Nội bộ nhà hàng)") + "\n" +
            "  * Lý do / Ghi chú: " + (sm.getNote() != null ? sm.getNote() : "-") + "\n" +
            "----------------------------------------------------------------------\n" +
            String.format("  %-4s %-25s %-10s %-12s %-14s\n", "STT", "Tên Hàng Hóa", "Số Lượng", "Đơn Giá", "Thành Tiền") +
            "----------------------------------------------------------------------\n" +
            String.format("  %-4d %-25s %-10s %-12s %-14s\n", 1, sm.getItemName(), sm.getQuantity(),
                CurrencyFormat.format(unitPrice), CurrencyFormat.format(totalVal)) +
            "----------------------------------------------------------------------\n" +
            "  TỔNG CỘNG TIỀN HÀNG: " + CurrencyFormat.format(totalVal) + "\n" +
            "======================================================================\n" +
            "                       CHỮ KÝ CÁC BÊN LIÊN QUAN                       \n\n" +
            "   Người Lập Phiếu       Thủ Kho        Người Giao/Nhận     Kế Toán   \n" +
            "      (Ký tên)           (Ký tên)           (Ký tên)        (Ký tên)  \n\n\n\n" +
            "======================================================================\n";

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chứng Từ Giấy Tờ - " + typeTitle, true);
        dlg.setSize(640, 530);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JTextArea area = new JTextArea(slip);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnPrint = createButton("🖨️ In Chứng Từ (Máy in)", new Color(16, 185, 129));
        JButton btnCopy = createButton("📋 Sao chép Text", new Color(59, 130, 246));
        JButton btnClose = createButton("Đóng", new Color(100, 116, 139));

        btnPrint.addActionListener(e -> {
            try {
                area.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi in: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCopy.addActionListener(e -> {
            area.selectAll();
            area.copy();
            JOptionPane.showMessageDialog(dlg, "Đã sao chép nội dung chứng từ vào Clipboard!");
        });

        btnClose.addActionListener(e -> dlg.dispose());

        btnBar.add(btnPrint);
        btnBar.add(btnCopy);
        btnBar.add(btnClose);

        dlg.add(new JScrollPane(area), BorderLayout.CENTER);
        dlg.add(btnBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ==================== STOCKTAKE (KIỂM KHO CÂN BẰNG) ====================
    private void showStocktakeDialog() {
        List<InventoryItem> items = inventoryController.getActiveItems();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kho chưa có mặt hàng nào để kiểm kê.");
            return;
        }

        String[] cols = {"ID", "Tên mặt hàng", "ĐVT", "Tồn hệ thống", "Số lượng đếm thực tế", "Lý do lệch"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4 || c == 5; }
        };

        for (InventoryItem i : items) {
            model.addRow(new Object[]{i.getId(), i.getName(), i.getUnit(), i.getStockQty(), i.getStockQty(), ""});
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(750, 400));
        panel.add(new JLabel("<html><b>ĐỢT KIỂM KÊ KHO TOÀN DIỆN</b><br>Nhập số lượng kiểm đếm thực tế vào cột 'Số lượng đếm thực tế'. Hệ thống sẽ tự động cân bằng tồn kho.</html>"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(this, panel, "Phiếu Kiểm Kê Kho Thực Tế", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
                List<InventoryAuditDetail> details = new ArrayList<>();
                for (int r = 0; r < model.getRowCount(); r++) {
                    int itemId = (int) model.getValueAt(r, 0);
                    BigDecimal sysQty = new BigDecimal(model.getValueAt(r, 3).toString().trim());
                    BigDecimal actQty = new BigDecimal(model.getValueAt(r, 4).toString().trim());
                    String reason = model.getValueAt(r, 5) != null ? model.getValueAt(r, 5).toString().trim() : "";

                    InventoryAuditDetail d = new InventoryAuditDetail();
                    d.setItemId(itemId);
                    d.setSystemQty(sysQty);
                    d.setActualQty(actQty);
                    d.setDifferenceQty(actQty.subtract(sysQty));
                    d.setReason(reason);
                    details.add(d);
                }

                inventoryController.performStocktake(currentUser, "Kiểm kê định kỳ bởi " + currentUser.getFullName(), details);
                JOptionPane.showMessageDialog(this, "Đợt kiểm kê đã được lưu thành công! Tồn kho đã được cân bằng chính xác.");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi kiểm kê: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAuditDetailsDialog() {
        int viewRow = auditTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đợt kiểm kho cần xem.");
            return;
        }
        int r = auditTable.convertRowIndexToModel(viewRow);
        int auditId = (int) auditModel.getValueAt(r, 0);
        List<InventoryAuditDetail> details = inventoryController.getAuditDetails(auditId);

        String[] cols = {"Hàng hóa", "ĐVT", "Tồn phần mềm", "Đếm thực tế", "Chênh lệch", "Lý do chênh lệch"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (InventoryAuditDetail d : details) {
            BigDecimal diff = d.getDifferenceQty() != null ? d.getDifferenceQty() : d.getActualQty().subtract(d.getSystemQty());
            model.addRow(new Object[]{
                d.getItemName(),
                d.getItemUnit(),
                d.getSystemQty(),
                d.getActualQty(),
                (diff.signum() > 0 ? "+" : "") + diff,
                d.getReason() != null ? d.getReason() : ""
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(650, 320));
        JOptionPane.showMessageDialog(this, scroll, "Chi tiết đợt kiểm kho #" + auditId, JOptionPane.PLAIN_MESSAGE);
    }

    // ==================== SUPPLIERS CRUD ====================
    private void showAddSupplierDialog() {
        JTextField txtName = new JTextField();
        JTextField txtContact = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtAddress = new JTextField();

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Tên Nhà Cung Cấp:")); p.add(txtName);
        p.add(new JLabel("Người đại diện:")); p.add(txtContact);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(new JLabel("Địa chỉ:")); p.add(txtAddress);

        int res = JOptionPane.showConfirmDialog(this, p, "Thêm Nhà Cung Cấp mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Supplier s = new Supplier();
                s.setName(txtName.getText().trim());
                s.setContactPerson(txtContact.getText().trim());
                s.setPhone(txtPhone.getText().trim());
                s.setEmail(txtEmail.getText().trim());
                s.setAddress(txtAddress.getText().trim());
                inventoryController.saveSupplier(s);
                JOptionPane.showMessageDialog(this, "Thêm Nhà cung cấp thành công!");
                loadSuppliersAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        }
    }

    private void showEditSupplierDialog() {
        int viewRow = supplierTable.getSelectedRow();
        if (viewRow < 0) return;
        int r = supplierTable.convertRowIndexToModel(viewRow);
        int supId = (int) supplierModel.getValueAt(r, 0);
        Supplier s = suppliersList != null ? suppliersList.stream().filter(x -> x.getId() == supId).findFirst().orElse(null) : null;
        if (s == null) return;

        JTextField txtName = new JTextField(s.getName());
        JTextField txtContact = new JTextField(s.getContactPerson() != null ? s.getContactPerson() : "");
        JTextField txtPhone = new JTextField(s.getPhone() != null ? s.getPhone() : "");
        JTextField txtEmail = new JTextField(s.getEmail() != null ? s.getEmail() : "");
        JTextField txtAddress = new JTextField(s.getAddress() != null ? s.getAddress() : "");

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Tên Nhà Cung Cấp:")); p.add(txtName);
        p.add(new JLabel("Người đại diện:")); p.add(txtContact);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(new JLabel("Địa chỉ:")); p.add(txtAddress);

        int res = JOptionPane.showConfirmDialog(this, p, "Chỉnh sửa NCC #" + s.getId(), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                s.setName(txtName.getText().trim());
                s.setContactPerson(txtContact.getText().trim());
                s.setPhone(txtPhone.getText().trim());
                s.setEmail(txtEmail.getText().trim());
                s.setAddress(txtAddress.getText().trim());
                inventoryController.saveSupplier(s);
                loadSuppliersAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        }
    }

    private void deleteSelectedSupplier() {
        int viewRow = supplierTable.getSelectedRow();
        if (viewRow < 0) return;
        int r = supplierTable.convertRowIndexToModel(viewRow);
        int supId = (int) supplierModel.getValueAt(r, 0);
        Supplier s = suppliersList != null ? suppliersList.stream().filter(x -> x.getId() == supId).findFirst().orElse(null) : null;
        if (s == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa NCC " + s.getName() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            inventoryController.deleteSupplier(s.getId());
            loadSuppliersAsync();
        }
    }
}
