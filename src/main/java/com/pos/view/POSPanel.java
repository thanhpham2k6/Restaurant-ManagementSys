package com.pos.view;

import com.pos.controller.*;
import com.pos.dao.CategoryDAO;
import com.pos.dao.MenuDAO;
import com.pos.dao.OrderDAO;
import com.pos.dao.TableDAO;
import com.pos.model.*;
import com.pos.model.MenuItem;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DishImageManager;
import com.pos.utils.WrapLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class POSPanel extends JPanel implements OrderObserver, StockObserver {
    private final User currentUser;
    private final OrderController orderController;
    private final KitchenController kitchenController;
    private final StockController stockController;
    private final TableController tableController = new TableController();
    private final MenuDAO menuDAO = new MenuDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final TableDAO tableDAO = new TableDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // State
    private Order currentOrder;
    private DiningTable selectedTable;

    // Left - Table Map
    private JPanel tableMapPanel;
    private final List<JButton> tableButtons = new ArrayList<>();

    // Center - Menu
    private JComboBox<Category> cbCategory;
    private JTextField txtSearch;
    private DefaultTableModel menuTableModel;
    private JTable menuTable;
    private JPanel menuCardContainer;
    private CardLayout menuCardLayout;
    private JPanel menuContentPanel;
    private JToggleButton btnToggleCardView;
    private JToggleButton btnToggleTableView;

    // Right - Invoice
    private JLabel lblOrderInfo;
    private DefaultTableModel invoiceTableModel;
    private JTable invoiceTable;
    private JLabel lblSubtotal, lblDiscount, lblVat, lblTotal;
    private JComboBox<DiscountStrategy> cbDiscount;
    private JComboBox<String> cbPayment;
    private JButton btnPay, btnCancel, btnSendKitchen;

    public POSPanel(User currentUser, OrderController orderController,
                    KitchenController kitchenController, StockController stockController) {
        this.currentUser = currentUser;
        this.orderController = orderController;
        this.kitchenController = kitchenController;
        this.stockController = stockController;
        setLayout(new BorderLayout());
        initUI();
        loadTables();
    }

    private void initUI() {
        JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenuPanel(), buildInvoicePanel());
        splitRight.setDividerLocation(520);
        splitRight.setResizeWeight(0.55);

        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTablePanel(), splitRight);
        splitMain.setDividerLocation(230);
        splitMain.setResizeWeight(0.18);

        add(splitMain, BorderLayout.CENTER);
    }

    // ================= TABLE MAP =================
    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            "Sơ đồ bàn ăn", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), new Color(30, 41, 59)));
        panel.setPreferredSize(new Dimension(230, 0));

        tableMapPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        tableMapPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(tableMapPanel);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottomTableBar = new JPanel(new GridLayout(2, 1, 4, 4));
        bottomTableBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        JButton btnMoveTable = new JButton("Chuyển / Gộp bàn");
        btnMoveTable.addActionListener(e -> handleTableMoveOrMerge());
        bottomTableBar.add(btnMoveTable);

        JButton btnRefresh = new JButton("Làm mới sơ đồ");
        btnRefresh.addActionListener(e -> loadTables());
        bottomTableBar.add(btnRefresh);

        panel.add(bottomTableBar, BorderLayout.SOUTH);
        return panel;
    }

    private void loadTables() {
        tableMapPanel.removeAll();
        tableButtons.clear();
        List<DiningTable> tables = tableController.getAllTables();
        for (DiningTable table : tables) {
            String statusText = switch (table.getStatus()) {
                case EMPTY -> "Trống";
                case OCCUPIED -> "Có khách";
                case RESERVED -> "Đặt trước";
            };

            JButton btn = new JButton("<html><center><b>" + table.getName() + "</b><br><small>"
                    + table.getCapacity() + " chỗ • " + statusText + "</small></center></html>");
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(95, 55));

            switch (table.getStatus()) {
                case EMPTY -> {
                    btn.setBackground(new Color(46, 125, 50));
                    btn.setForeground(Color.WHITE);
                }
                case OCCUPIED -> {
                    btn.setBackground(new Color(198, 40, 40));
                    btn.setForeground(Color.WHITE);
                }
                case RESERVED -> {
                    btn.setBackground(new Color(239, 108, 0));
                    btn.setForeground(Color.WHITE);
                }
            }

            btn.addActionListener(e -> onTableClick(table));
            tableMapPanel.add(btn);
            tableButtons.add(btn);
        }
        tableMapPanel.revalidate();
        tableMapPanel.repaint();
    }

    private void onTableClick(DiningTable table) {
        if (table.getStatus() == TableStatus.EMPTY) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bàn '" + table.getName() + "' đang trống. Bạn có muốn mở bàn mới?",
                "Mở bàn", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                currentOrder = tableController.openTable(table.getId(), currentUser);
                selectedTable = table;
                updateInvoiceHeader();
                loadInvoice();
                loadTables();
                JOptionPane.showMessageDialog(this, "Đã mở bàn '" + table.getName() + "' (Mã đơn #" + currentOrder.getId() + ")");
            } catch (TableController.TableBusyException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Bàn đang bận", JOptionPane.WARNING_MESSAGE);
            }
        } else if (table.getStatus() == TableStatus.OCCUPIED) {
            Order existingOrder = orderDAO.findOpenByTable(table.getId());
            if (existingOrder != null) {
                currentOrder = existingOrder;
                selectedTable = table;
                updateInvoiceHeader();
                loadInvoice();
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy đơn hàng đang mở cho bàn này!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Bàn đang ở trạng thái: " + table.getStatus(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleTableMoveOrMerge() {
        if (currentOrder == null || selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn đang phục vụ trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = {"Chuyển sang bàn trống", "Gộp vào bàn khác"};
        int choice = JOptionPane.showOptionDialog(this,
            "Chọn thao tác cho bàn '" + selectedTable.getName() + "' (Đơn #" + currentOrder.getId() + "):",
            "Thao tác bàn", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        List<DiningTable> allTables = tableController.getAllTables();

        if (choice == 0) {
            List<DiningTable> emptyTables = allTables.stream().filter(t -> t.getStatus() == TableStatus.EMPTY).toList();
            if (emptyTables.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Hiện không có bàn trống nào để chuyển!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            DiningTable target = (DiningTable) JOptionPane.showInputDialog(this,
                "Chọn bàn trống đích:", "Chuyển bàn", JOptionPane.QUESTION_MESSAGE, null,
                emptyTables.toArray(), emptyTables.get(0));
            if (target != null) {
                try {
                    tableController.moveTable(currentOrder.getId(), target.getId(), currentUser);
                    selectedTable = target;
                    currentOrder.setTable(target);
                    updateInvoiceHeader();
                    loadTables();
                    JOptionPane.showMessageDialog(this, "Đã chuyển đơn sang bàn " + target.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi chuyển bàn", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (choice == 1) {
            List<DiningTable> occupiedTables = allTables.stream()
                .filter(t -> t.getStatus() == TableStatus.OCCUPIED && t.getId() != selectedTable.getId()).toList();
            if (occupiedTables.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có bàn có khách khác để gộp vào!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            DiningTable target = (DiningTable) JOptionPane.showInputDialog(this,
                "Chọn bàn đích muốn gộp vào:", "Gộp bàn", JOptionPane.QUESTION_MESSAGE, null,
                occupiedTables.toArray(), occupiedTables.get(0));
            if (target != null) {
                Order targetOrder = orderDAO.findOpenByTable(target.getId());
                if (targetOrder != null) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Xác nhận gộp đơn bàn " + selectedTable.getName() + " vào bàn " + target.getName() + "?",
                        "Xác nhận", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        tableController.mergeTables(currentOrder.getId(), targetOrder.getId());
                        currentOrder = targetOrder;
                        selectedTable = target;
                        updateInvoiceHeader();
                        loadInvoice();
                        loadTables();
                        JOptionPane.showMessageDialog(this, "Gộp bàn thành công!");
                    }
                }
            }
        }
    }

    // ================= MENU PANEL =================
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            "Thực đơn món ăn", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), new Color(30, 41, 59)));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        // Thanh công cụ lọc, tìm kiếm và chuyển chế độ xem
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        cbCategory = new JComboBox<>();
        cbCategory.addItem(new Category(0, "-- Tất cả danh mục --", CategoryType.FOOD));
        categoryDAO.findAll().forEach(cbCategory::addItem);
        cbCategory.addActionListener(e -> loadMenuItems());
        filterBar.add(new JLabel("Loại:"));
        filterBar.add(cbCategory);

        txtSearch = new JTextField(10);
        txtSearch.addActionListener(e -> loadMenuItems());
        filterBar.add(new JLabel("Tìm:"));
        filterBar.add(txtSearch);

        JButton btnSearch = new JButton("Tìm");
        btnSearch.addActionListener(e -> loadMenuItems());
        filterBar.add(btnSearch);

        // Nút chuyển đổi giữa Thẻ ảnh và Bảng danh sách
        btnToggleCardView = new JToggleButton("🖼️ Thẻ ảnh", true);
        btnToggleTableView = new JToggleButton("📋 Danh sách", false);
        btnToggleCardView.setFocusPainted(false);
        btnToggleTableView.setFocusPainted(false);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(btnToggleCardView);
        viewGroup.add(btnToggleTableView);

        btnToggleCardView.addActionListener(e -> menuCardLayout.show(menuContentPanel, "CARDS"));
        btnToggleTableView.addActionListener(e -> menuCardLayout.show(menuContentPanel, "TABLE"));

        filterBar.add(new JSeparator(SwingConstants.VERTICAL));
        filterBar.add(btnToggleCardView);
        filterBar.add(btnToggleTableView);

        panel.add(filterBar, BorderLayout.NORTH);

        // Nội dung hiển thị món ăn: CardLayout (Thẻ ảnh hoặc Bảng)
        menuCardLayout = new CardLayout();
        menuContentPanel = new JPanel(menuCardLayout);

        // 1. Giao diện Thẻ ảnh (Cards Grid với WrapLayout)
        menuCardContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        menuCardContainer.setBackground(new Color(248, 250, 252));
        JScrollPane scrollCards = new JScrollPane(menuCardContainer);
        scrollCards.setBorder(null);
        scrollCards.getVerticalScrollBar().setUnitIncrement(18);
        scrollCards.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        menuContentPanel.add(scrollCards, "CARDS");

        // 2. Giao diện Bảng danh sách (Table View kèm ảnh nhỏ)
        menuTableModel = new DefaultTableModel(new Object[]{"ID", "Ảnh", "Tên món", "Giá", "Danh mục", "Suất còn"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return Icon.class;
                return Object.class;
            }
        };
        menuTable = new JTable(menuTableModel);
        menuTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menuTable.setRowHeight(38);
        menuTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        menuTable.getColumnModel().getColumn(0).setMaxWidth(0);
        menuTable.getColumnModel().getColumn(0).setMinWidth(0);
        menuTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        menuTable.getColumnModel().getColumn(1).setPreferredWidth(48);
        menuTable.getColumnModel().getColumn(1).setMaxWidth(55);
        menuTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                if (value instanceof Icon icon) {
                    lbl.setIcon(icon);
                } else {
                    lbl.setIcon(null);
                }
                return lbl;
            }
        });

        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedMenuItemToOrder();
            }
        });
        JScrollPane scrollTable = new JScrollPane(menuTable);
        scrollTable.setBorder(null);
        menuContentPanel.add(scrollTable, "TABLE");

        panel.add(menuContentPanel, BorderLayout.CENTER);

        // Thanh chân trang
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel hint = new JLabel("* Nhấp vào thẻ ảnh hoặc nhấp đúp vào món để thêm vào hóa đơn");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(100, 116, 139));
        bottomBar.add(hint);
        JButton btnAdd = new JButton("Thêm món");
        btnAdd.addActionListener(e -> addSelectedMenuItemToOrder());
        bottomBar.add(btnAdd);
        panel.add(bottomBar, BorderLayout.SOUTH);

        loadMenuItems();
        return panel;
    }

    private void loadMenuItems() {
        menuTableModel.setRowCount(0);
        menuCardContainer.removeAll();

        Category selected = (Category) cbCategory.getSelectedItem();
        String keyword = txtSearch.getText().trim();
        List<MenuItem> items;
        if (!keyword.isEmpty()) {
            items = menuDAO.search(keyword);
        } else if (selected != null && selected.getId() > 0) {
            items = menuDAO.findByCategory(selected.getId());
        } else {
            items = menuDAO.findAll();
        }

        for (MenuItem item : items) {
            if (!item.isAvailable()) continue;
            int servings = stockController.getRemainingServings(item.getId());
            String servingStr;
            Color badgeBg;
            Color badgeFg;

            if (servings == 0) {
                servingStr = "Hết hàng";
                badgeBg = new Color(254, 226, 226);
                badgeFg = new Color(185, 28, 28);
            } else if (servings != Integer.MAX_VALUE && servings <= 10) {
                servingStr = "Còn " + servings + " suất";
                badgeBg = new Color(254, 243, 199);
                badgeFg = new Color(180, 83, 9);
            } else if (servings != Integer.MAX_VALUE) {
                servingStr = "Còn " + servings + " suất";
                badgeBg = new Color(220, 252, 231);
                badgeFg = new Color(21, 128, 61);
            } else {
                servingStr = "Luôn sẵn có";
                badgeBg = new Color(219, 234, 254);
                badgeFg = new Color(29, 78, 216);
            }

            // 1. Thêm vào Table Model (kèm ảnh thumbnail 38x28px)
            ImageIcon thumb = DishImageManager.getDishIcon(item.getId(), item.getCategory().getName(), item.getName(), 38, 28, 4);
            menuTableModel.addRow(new Object[]{
                item.getId(),
                thumb,
                item.getName(),
                CurrencyFormat.format(item.getPrice()),
                item.getCategory().getName(),
                servingStr
            });

            // 2. Thêm vào Card View trực quan
            JPanel card = createDishCard(item, servings, servingStr, badgeBg, badgeFg);
            menuCardContainer.add(card);
        }

        menuCardContainer.revalidate();
        menuCardContainer.repaint();
    }

    private JPanel createDishCard(MenuItem item, int servings, String servingStr, Color badgeBg, Color badgeFg) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setPreferredSize(new Dimension(142, 190));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hình ảnh món ăn sắc nét bo góc 130x88px
        ImageIcon icon = DishImageManager.getDishIcon(item.getId(), item.getCategory().getName(), item.getName(), 130, 88, 8);
        JLabel lblImg = new JLabel(icon);
        lblImg.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblImg, BorderLayout.NORTH);

        // Thông tin: Tên, Giá, Suất còn
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel("<html><center><b>" + item.getName() + "</b></center></html>");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblName);

        JLabel lblPrice = new JLabel(CurrencyFormat.format(item.getPrice()));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPrice.setForeground(new Color(225, 29, 72));
        lblPrice.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblPrice);

        JLabel lblBadge = new JLabel(servingStr);
        lblBadge.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        lblBadge.setForeground(badgeFg);
        lblBadge.setHorizontalAlignment(SwingConstants.CENTER);
        lblBadge.setBorder(new EmptyBorder(2, 4, 2, 4));
        infoPanel.add(lblBadge);

        card.add(infoPanel, BorderLayout.CENTER);

        // Hiệu ứng hover và sự kiện click để thêm món
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                handleSelectMenuItem(item, servings);
            }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(37, 99, 235), 2, true),
                    new EmptyBorder(5, 5, 5, 5)
                ));
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                    new EmptyBorder(6, 6, 6, 6)
                ));
            }
        };

        card.addMouseListener(ma);
        lblImg.addMouseListener(ma);
        lblName.addMouseListener(ma);
        lblPrice.addMouseListener(ma);
        lblBadge.addMouseListener(ma);

        return card;
    }

    private void handleSelectMenuItem(MenuItem item, int servings) {
        if (currentOrder == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn đang mở hoặc mở bàn mới trước khi chọn món!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (servings == 0) {
            JOptionPane.showMessageDialog(this, "Món '" + item.getName() + "' hiện đã hết nguyên liệu!", "Hết hàng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this, "Nhập số lượng cho món '" + item.getName() + "':", "1");
        if (qtyStr == null) return;
        int qty;
        try {
            qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương lớn hơn 0!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (servings != Integer.MAX_VALUE && qty > servings) {
            JOptionPane.showMessageDialog(this, "Số lượng yêu cầu (" + qty + ") vượt quá số suất nguyên liệu còn lại (" + servings + ")!", "Cảnh báo kho", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String note = JOptionPane.showInputDialog(this, "Ghi chú cho món (ít đá, cay, không hành...):", "");
        if (note == null) note = "";

        orderController.addItem(currentOrder, item, qty, note);
        loadInvoice();
        updateTotal();
    }

    private void addSelectedMenuItemToOrder() {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món trong danh sách!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int itemId = (int) menuTableModel.getValueAt(row, 0);
        MenuItem item = menuDAO.findById(itemId);
        if (item == null) return;
        int servings = stockController.getRemainingServings(item.getId());
        handleSelectMenuItem(item, servings);
    }

    // ================= INVOICE PANEL =================
    private JPanel buildInvoicePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));

        lblOrderInfo = new JLabel("  Chưa có bàn nào được chọn");
        lblOrderInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOrderInfo.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(lblOrderInfo, BorderLayout.NORTH);

        invoiceTableModel = new DefaultTableModel(
            new Object[]{"ID", "Tên món", "SL", "Đơn giá", "Thành tiền", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceTable = new JTable(invoiceTableModel);
        invoiceTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        invoiceTable.setRowHeight(26);
        invoiceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        invoiceTable.getColumnModel().getColumn(0).setMaxWidth(0);
        invoiceTable.getColumnModel().getColumn(0).setMinWidth(0);
        invoiceTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        panel.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setBorder(new EmptyBorder(4, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 4, 2, 4);

        lblSubtotal = new JLabel("Tạm tính: 0 đ");
        lblDiscount = new JLabel("Giảm giá: 0 đ");
        lblVat      = new JLabel("VAT (10%): 0 đ");
        lblTotal    = new JLabel("TỔNG TIỀN: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotal.setForeground(new Color(225, 29, 72));

        for (JLabel lbl : new JLabel[]{lblSubtotal, lblDiscount, lblVat}) {
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }

        cbDiscount = new JComboBox<>(new DiscountStrategy[]{new NoDiscount(), new VipDiscount(), new HolidayDiscount()});
        cbDiscount.addActionListener(e -> updateTotal());
        cbDiscount.addItem(new BirthdayDiscount());

        cbPayment = new JComboBox<>(new String[]{"CASH", "TRANSFER","MOMO"});

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; bottom.add(lblSubtotal, gbc);
        gbc.gridy = 1; bottom.add(lblDiscount, gbc);
        gbc.gridy = 2; bottom.add(lblVat, gbc);
        gbc.gridy = 3; bottom.add(lblTotal, gbc);

        gbc.gridwidth = 1; gbc.gridy = 4; gbc.gridx = 0;
        bottom.add(new JLabel("Chiết khấu:"), gbc);
        gbc.gridx = 1; bottom.add(cbDiscount, gbc);

        gbc.gridy = 5; gbc.gridx = 0;
        bottom.add(new JLabel("Thanh toán:"), gbc);
        gbc.gridx = 1; bottom.add(cbPayment, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));

        btnSendKitchen = new JButton("Gửi bếp");
        btnSendKitchen.setBackground(new Color(25, 118, 210));
        btnSendKitchen.setForeground(Color.WHITE);
        btnSendKitchen.setFocusPainted(false);
        btnSendKitchen.addActionListener(e -> sendToKitchen());

        JButton btnRemoveItem = new JButton("Hủy món");
        btnRemoveItem.addActionListener(e -> removeSelectedItem());
        JButton btnPrintAgain = new JButton("In lại hóa đơn");
        btnPrintAgain.setBackground(new Color(30, 144, 255));
        btnPrintAgain.setForeground(Color.WHITE);
        btnPrintAgain.setFocusPainted(false);
        btnPrintAgain.addActionListener(e -> {
            if (currentOrder == null) {
                JOptionPane.showMessageDialog(this, "Chưa chọn đơn hàng nào!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(this, "Đang in lại hóa đơn cho đơn #" + currentOrder.getId());
        });
        btnRow.add(btnPrintAgain);


        btnPay = new JButton("Thanh toán");
        btnPay.setBackground(new Color(46, 125, 50));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFocusPainted(false);
        btnPay.addActionListener(e -> checkout());

        btnCancel = new JButton("Hủy đơn");
        btnCancel.setBackground(new Color(198, 40, 40));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> cancelOrder());

        btnRow.add(btnSendKitchen);
        btnRow.add(btnRemoveItem);
        btnRow.add(btnPay);
        btnRow.add(btnCancel);
        
        gbc.gridy = 6; gbc.gridx = 0; gbc.gridwidth = 2; bottom.add(btnRow, gbc);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void updateInvoiceHeader() {
        if (currentOrder != null && selectedTable != null) {
            lblOrderInfo.setText("  Bàn: " + selectedTable.getName() +
                "  |  Mã đơn #" + currentOrder.getId() +
                "  |  Thu ngân: " + currentUser.getFullName());
        } else {
            lblOrderInfo.setText("  Chưa có bàn nào được chọn");
        }
    }

    private void loadInvoice() {
        invoiceTableModel.setRowCount(0);
        if (currentOrder == null) return;
        List<OrderDetail> details = orderController.getOrderDetails(currentOrder.getId());
        currentOrder.setDetails(details);
        for (OrderDetail d : details) {
            String statusLabel = switch (d.getStatus()) {
                case PENDING  -> d.isSentToKitchen() ? "Chờ bếp" : "Chưa gửi bếp";
                case ACCEPTED -> "Bếp đã nhận";
                case COOKING  -> "Đang nấu";
                case DONE     -> "Nấu xong";
                case REJECTED -> "Đã hủy";
            };
            invoiceTableModel.addRow(new Object[]{
                d.getId(),
                d.getItem() != null ? d.getItem().getName() : "?",
                d.getQuantity(),
                CurrencyFormat.format(d.getUnitPrice()),
                CurrencyFormat.format(d.getSubtotal()),
                statusLabel
            });
        }
        updateTotal();
    }

    private void updateTotal() {
        if (currentOrder == null) {
            lblSubtotal.setText("Tạm tính: 0 đ");
            lblDiscount.setText("Giảm giá: 0 đ");
            lblVat.setText("VAT (10%): 0 đ");
            lblTotal.setText("TỔNG TIỀN: 0 đ");
            return;
        }
        DiscountStrategy strategy = (DiscountStrategy) cbDiscount.getSelectedItem();
        orderController.setDiscountStrategy(strategy != null ? strategy : new NoDiscount());

        List<OrderDetail> details = currentOrder.getDetails();
        BigDecimal subtotal = details.stream()
            .filter(d -> d.getStatus() != ItemStatus.REJECTED)
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = strategy != null ? strategy.apply(subtotal) : BigDecimal.ZERO;
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal vat = afterDiscount.multiply(new BigDecimal("0.10")).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal total = afterDiscount.add(vat);

        lblSubtotal.setText("Tạm tính: " + CurrencyFormat.format(subtotal));
        lblDiscount.setText("Giảm giá: -" + CurrencyFormat.format(discount));
        lblVat.setText("VAT (10%): +" + CurrencyFormat.format(vat));
        lblTotal.setText("TỔNG TIỀN: " + CurrencyFormat.format(total));
    }

    private void sendToKitchen() {
        if (currentOrder == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn đơn hàng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (invoiceTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Đơn hàng chưa có món nào! Hãy chọn món trước.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        orderController.sendToKitchen(currentOrder.getId());
        loadInvoice();
        JOptionPane.showMessageDialog(this, "Đã gửi đơn hàng #" + currentOrder.getId() + " sang Nhà Bếp!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void checkout() {
        if (currentOrder == null) {
            JOptionPane.showMessageDialog(this, "Chưa có đơn hàng nào được chọn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (invoiceTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Đơn hàng chưa có món nào! Không thể thanh toán.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DiscountStrategy strategy = (DiscountStrategy) cbDiscount.getSelectedItem();
        String payType = (String) cbPayment.getSelectedItem();

        int confirm = JOptionPane.showConfirmDialog(this,
            lblTotal.getText() + "\n\nXác nhận thanh toán phương thức: " + payType + "?",
            "Xác nhận thanh toán", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            String pdfPath = orderController.checkout(currentOrder, payType,
                strategy != null ? strategy : new NoDiscount(), stockController);

            int tableId = currentOrder.getTable().getId();
            tableController.clearTable(tableId);

            JOptionPane.showMessageDialog(this,
                "Thanh toán thành công và đã giải phóng bàn!\n" +
                (pdfPath != null ? "Hóa đơn PDF: " + pdfPath : "Không thể tạo file PDF"),
                "Thanh toán thành công", JOptionPane.INFORMATION_MESSAGE);

            currentOrder = null;
            selectedTable = null;
            invoiceTableModel.setRowCount(0);
            updateInvoiceHeader();
            updateTotal();
            loadTables();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Chưa thể thanh toán", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cancelOrder() {
        if (currentOrder == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xác nhận hủy toàn bộ đơn #" + currentOrder.getId() + " và giải phóng bàn?",
            "Xác nhận hủy đơn", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            orderController.cancelOrder(currentOrder.getId(), currentOrder.getTable().getId(),
                currentOrder.getDetails(), stockController);
            tableController.clearTable(currentOrder.getTable().getId());
            currentOrder = null;
            selectedTable = null;
            invoiceTableModel.setRowCount(0);
            updateInvoiceHeader();
            updateTotal();
            loadTables();
            JOptionPane.showMessageDialog(this, "Đã hủy đơn hàng thành công!");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Không thể hủy đơn", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void removeSelectedItem() {
        if (currentOrder == null) return;
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn món cần hủy trong bảng hóa đơn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int detailId = (int) invoiceTableModel.getValueAt(row, 0);
        String name = (String) invoiceTableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xác nhận hủy món '" + name + "' khỏi đơn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            orderController.cancelItem(detailId);
            loadInvoice();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Không thể hủy món", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ================= OBSERVERS =================
    @Override
    public void onOrderChanged(OrderEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (currentOrder != null && event.getOrderId() == currentOrder.getId()) {
                loadInvoice();
            }
            loadTables();
        });
    }

    @Override
    public void onStockChanged(int menuItemId) {
        SwingUtilities.invokeLater(this::loadMenuItems);
    }
}
