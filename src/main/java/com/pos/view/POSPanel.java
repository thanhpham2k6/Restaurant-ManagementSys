package com.pos.view;

import com.pos.controller.*;
import com.pos.dao.*;
import com.pos.model.*;
import com.pos.model.MenuItem;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DishImageManager;
import com.pos.utils.TableUtils;
import com.pos.utils.WrapLayout;

import java.time.LocalDate;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Phân hệ Thu ngân (POS - Point of Sale).
 * Giao diện 2 cột hiện đại: Bên trái là Menu món ăn (ảnh lớn, trực quan),
 * Bên phải là Đơn hàng & Thanh toán (Tại bàn, Mang về, Giao hàng).
 */
public class POSPanel extends JPanel implements OrderObserver, StockObserver {
    private final User currentUser;
    private final OrderController orderController;
    private final KitchenController kitchenController;
    private final StockController stockController;
    private final ShiftController shiftController;
    private final CustomerController customerController;
    private final TableController tableController;

    private final TableDAO tableDAO = new TableDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    private Consumer<String> onNavigateTabRequested;

    // State
    private Order currentOrder;
    private DiningTable selectedTable;
    private Customer currentCustomer;
    private Voucher currentVoucher;
    private int usedPoints = 0;
    private OrderType currentOrderType = OrderType.DINE_IN;
    private List<DiningTable> cachedTables = new ArrayList<>();

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

    // Right - Order Header & Type
    private JComboBox<OrderType> cbOrderType;
    private JPanel contextRow;
    private JComboBox<DiningTable> cbDiningTable;
    private boolean isUpdatingTableCombo = false;
    private JButton btnOpenOrSwitchTable;
    private JButton btnViewFloorPlan;
    private JLabel lblOrderInfo;

    // Right - Invoice Table & Controls
    private DefaultTableModel invoiceTableModel;
    private JTable invoiceTable;
    private JButton btnQtyPlus, btnQtyMinus, btnRemoveItem, btnItemNote;

    // Right - Customer & Voucher
    private JTextField txtCustomerPhone;
    private JLabel lblCustomerInfo;
    private JButton btnApplyCustomer;
    private JButton btnUsePoints;
    private JComboBox<String> cbVoucherSelector;
    private List<Voucher> activeVouchers = new ArrayList<>();
    private boolean isUpdatingVoucherCombo = false;
    private JTextField txtVoucherCode;
    private JButton btnApplyVoucher;
    private JButton btnCancelVoucher;
    private JLabel lblVoucherAppliedInfo;
    private Runnable onTableStatusChanged;

    // Right - Totals & Actions
    private JLabel lblSubtotal, lblDiscount, lblTotal;
    private JComboBox<DiscountStrategy> cbDiscount;
    private JComboBox<String> cbPayment;
    private JButton btnPay, btnCancel, btnSendKitchen;

    public POSPanel(User currentUser, OrderController orderController,
                    KitchenController kitchenController, StockController stockController) {
        this.currentUser = currentUser;
        this.orderController = orderController;
        this.kitchenController = kitchenController;
        this.stockController = stockController;
        this.shiftController = new ShiftController();
        this.customerController = new CustomerController();
        this.tableController = new TableController();

        setLayout(new BorderLayout());
        initUI();
        loadTablesAsync();
        loadActiveVouchers();
    }

    public void setOnTableStatusChanged(Runnable onTableStatusChanged) {
        this.onTableStatusChanged = onTableStatusChanged;
    }

    public void setOnNavigateTabRequested(Consumer<String> onNavigateTabRequested) {
        this.onNavigateTabRequested = onNavigateTabRequested;
    }

    private void initUI() {
        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenuPanel(), buildInvoicePanel());
        splitMain.setDividerLocation(700);
        splitMain.setResizeWeight(0.62);
        splitMain.setBorder(null);

        add(splitMain, BorderLayout.CENTER);

        // F9 Shortcut for checkout
        registerKeyboardAction(e -> checkout(),
            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // =========================================================================
    // 1. MENU PANEL (BÊN TRÁI)
    // =========================================================================
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new CompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                "Thực đơn món ăn & đồ uống", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(30, 41, 59)),
            new EmptyBorder(6, 8, 8, 8)
        ));
        panel.setBackground(new Color(248, 250, 252));

        // Filter Bar (Category dropdown + Search bar + View Switcher)
        JPanel filterBar = new JPanel(new BorderLayout(8, 0));
        filterBar.setOpaque(false);

        JPanel leftFilters = new JPanel(new BorderLayout(8, 0));
        leftFilters.setOpaque(false);

        cbCategory = new JComboBox<>();
        cbCategory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbCategory.setPreferredSize(new Dimension(170, 34));
        cbCategory.addItem(new Category(0, "Tất cả danh mục", CategoryType.FOOD));
        new SwingWorker<List<Category>, Void>() {
            @Override protected List<Category> doInBackground() { return categoryDAO.findAll(); }
            @Override protected void done() {
                try {
                    for (Category c : get()) cbCategory.addItem(c);
                } catch (Exception ignored) {}
            }
        }.execute();
        cbCategory.addActionListener(e -> loadMenuItems());

        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(220, 34));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.putClientProperty("JTextField.placeholderText", "🔍 Tìm món ăn theo tên...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { loadMenuItems(); }
        });

        leftFilters.add(cbCategory, BorderLayout.WEST);
        leftFilters.add(txtSearch, BorderLayout.CENTER);

        JPanel viewTogglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        viewTogglePanel.setOpaque(false);

        btnToggleCardView = new JToggleButton("Thẻ ảnh", true);
        btnToggleTableView = new JToggleButton("Bảng", false);
        btnToggleCardView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggleTableView.setFont(new Font("Segoe UI", Font.BOLD, 12));

        ButtonGroup bg = new ButtonGroup();
        bg.add(btnToggleCardView);
        bg.add(btnToggleTableView);

        btnToggleCardView.addActionListener(e -> menuCardLayout.show(menuContentPanel, "CARD_VIEW"));
        btnToggleTableView.addActionListener(e -> menuCardLayout.show(menuContentPanel, "TABLE_VIEW"));

        viewTogglePanel.add(btnToggleCardView);
        viewTogglePanel.add(btnToggleTableView);

        filterBar.add(leftFilters, BorderLayout.CENTER);
        filterBar.add(viewTogglePanel, BorderLayout.EAST);
        panel.add(filterBar, BorderLayout.NORTH);

        // Center: Card Layout (Card View vs Table View)
        menuCardLayout = new CardLayout();
        menuContentPanel = new JPanel(menuCardLayout);
        menuContentPanel.setOpaque(false);

        // 1. Card View
        menuCardContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        menuCardContainer.setBackground(new Color(248, 250, 252));
        JScrollPane cardScroll = new JScrollPane(menuCardContainer);
        cardScroll.getVerticalScrollBar().setUnitIncrement(16);
        cardScroll.setBorder(null);

        // 2. Table View
        menuTableModel = new DefaultTableModel(new Object[]{"ID", "Tên món", "Danh mục", "Đơn giá", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        menuTable = new JTable(menuTableModel);
        menuTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menuTable.setRowHeight(32);
        menuTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(menuTable);
        menuTable.getColumnModel().getColumn(0).setMaxWidth(50);
        menuTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                if (val instanceof BigDecimal bd) setText(CurrencyFormat.format(bd));
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        });
        menuTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedMenuItemToOrder();
            }
        });
        JScrollPane tableScroll = new JScrollPane(menuTable);
        tableScroll.setBorder(null);

        menuContentPanel.add(cardScroll, "CARD_VIEW");
        menuContentPanel.add(tableScroll, "TABLE_VIEW");
        panel.add(menuContentPanel, BorderLayout.CENTER);

        loadMenuItems();
        return panel;
    }

    private void loadMenuItems() {
        Category selCat = (Category) cbCategory.getSelectedItem();
        String keyword = txtSearch.getText().trim();

        new SwingWorker<List<MenuItem>, Void>() {
            @Override protected List<MenuItem> doInBackground() {
                if (!keyword.isEmpty()) return menuDAO.search(keyword);
                if (selCat != null && selCat.getId() > 0) return menuDAO.findByCategory(selCat.getId());
                return menuDAO.findAll();
            }

            @Override protected void done() {
                try {
                    List<MenuItem> items = get();
                    menuTableModel.setRowCount(0);
                    menuCardContainer.removeAll();

                    for (MenuItem item : items) {
                        menuTableModel.addRow(new Object[]{
                            item.getId(),
                            item.getName(),
                            item.getCategory() != null ? item.getCategory().getName() : "",
                            item.getPrice(),
                            item.isAvailable() ? "Sẵn sàng" : "Hết hàng"
                        });
                        menuCardContainer.add(createMenuItemCard(item));
                    }
                    menuCardContainer.revalidate();
                    menuCardContainer.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JPanel createMenuItemCard(MenuItem item) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setPreferredSize(new Dimension(150, 185));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Dish Photo
        JLabel lblImg = new JLabel("", SwingConstants.CENTER);
        lblImg.setPreferredSize(new Dimension(138, 100));
        ImageIcon icon = DishImageManager.getDishIcon(
            item.getId(),
            item.getImagePath(),
            item.getCategory() != null ? item.getCategory().getName() : "",
            item.getName(),
            138, 100
        );
        if (icon != null) lblImg.setIcon(icon);
        card.add(lblImg, BorderLayout.NORTH);

        // Info: Name & Price
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel("<html><center><b>" + item.getName() + "</b></center></html>");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblName);

        JLabel lblPrice = new JLabel(CurrencyFormat.format(item.getPrice()));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrice.setForeground(new Color(225, 29, 72));
        lblPrice.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblPrice);

        card.add(infoPanel, BorderLayout.CENTER);

        // Hover & Click
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                quickAddMenuItem(item, 1, "");
            }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                    new LineBorder(new Color(16, 185, 129), 2, true),
                    new EmptyBorder(5, 5, 5, 5)
                ));
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                    new LineBorder(new Color(226, 232, 240), 1, true),
                    new EmptyBorder(6, 6, 6, 6)
                ));
            }
        };

        card.addMouseListener(ma);
        lblImg.addMouseListener(ma);
        lblName.addMouseListener(ma);
        lblPrice.addMouseListener(ma);
        return card;
    }

    private void addSelectedMenuItemToOrder() {
        int viewRow = menuTable.getSelectedRow();
        if (viewRow < 0) return;
        int r = menuTable.convertRowIndexToModel(viewRow);
        int id = (int) menuTableModel.getValueAt(r, 0);
        MenuItem item = menuDAO.findById(id);
        if (item != null) quickAddMenuItem(item, 1, "");
    }

    // =========================================================================
    // 2. INVOICE / ORDER PANEL (BÊN PHẢI)
    // =========================================================================
    private JPanel buildInvoicePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new CompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                "Hóa đơn & Thanh toán", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(30, 41, 59)),
            new EmptyBorder(6, 8, 8, 8)
        ));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(420, 0));

        // Top Order Control Section
        JPanel topOrderBox = new JPanel(new BorderLayout(4, 6));
        topOrderBox.setOpaque(false);

        // Row 1: Order Type Selector + Floor Plan button
        JPanel typeRow = new JPanel(new BorderLayout(6, 0));
        typeRow.setOpaque(false);

        cbOrderType = new JComboBox<>(new OrderType[]{OrderType.DINE_IN, OrderType.TAKEAWAY});
        cbOrderType.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbOrderType.setPreferredSize(new Dimension(170, 32));
        cbOrderType.addActionListener(e -> onOrderTypeChanged());

        btnViewFloorPlan = new JButton("Sơ đồ bàn");
        btnViewFloorPlan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewFloorPlan.setBackground(new Color(241, 245, 249));
        btnViewFloorPlan.setForeground(new Color(51, 65, 85));
        btnViewFloorPlan.setFocusPainted(false);
        btnViewFloorPlan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewFloorPlan.addActionListener(e -> {
            if (onNavigateTabRequested != null) {
                onNavigateTabRequested.accept("Sơ đồ & Đặt bàn");
            }
        });

        typeRow.add(new JLabel("Hình thức:"), BorderLayout.WEST);
        typeRow.add(cbOrderType, BorderLayout.CENTER);
        typeRow.add(btnViewFloorPlan, BorderLayout.EAST);

        // Row 2: Table Selector
        contextRow = new JPanel(new BorderLayout(6, 0));
        contextRow.setOpaque(false);

        cbDiningTable = new JComboBox<>();
        cbDiningTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbDiningTable.setPreferredSize(new Dimension(210, 32));
        cbDiningTable.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DiningTable dt) {
                    String stText = switch (dt.getStatus()) {
                        case AVAILABLE, EMPTY -> "[Trống]";
                        case OCCUPIED  -> "[Có khách]";
                        case RESERVED  -> "[Đặt trước]";
                        default -> "[Trống]";
                    };
                    setText(dt.getName() + " (" + dt.getCapacity() + " chỗ) — " + stText);
                }
                return this;
            }
        });
        cbDiningTable.addActionListener(e -> {
            if (isUpdatingTableCombo) return;
            DiningTable dt = (DiningTable) cbDiningTable.getSelectedItem();
            if (dt != null && currentOrderType == OrderType.DINE_IN) {
                switchTableOrder(dt);
            }
        });

        btnOpenOrSwitchTable = new JButton("Bắt đầu đơn");
        btnOpenOrSwitchTable.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenOrSwitchTable.setBackground(new Color(16, 185, 129));
        btnOpenOrSwitchTable.setForeground(Color.WHITE);
        btnOpenOrSwitchTable.setFocusPainted(false);
        btnOpenOrSwitchTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpenOrSwitchTable.addActionListener(e -> onOpenOrSwitchTableClicked());

        contextRow.add(new JLabel("Bàn ăn:"), BorderLayout.WEST);
        contextRow.add(cbDiningTable, BorderLayout.CENTER);
        contextRow.add(btnOpenOrSwitchTable, BorderLayout.EAST);

        // Info Banner
        lblOrderInfo = new JLabel("Chưa chọn bàn / đơn hàng", SwingConstants.LEFT);
        lblOrderInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOrderInfo.setForeground(new Color(15, 118, 110));
        lblOrderInfo.setBorder(new EmptyBorder(4, 2, 4, 2));

        JPanel headerStack = new JPanel();
        headerStack.setLayout(new BoxLayout(headerStack, BoxLayout.Y_AXIS));
        headerStack.setOpaque(false);
        headerStack.add(typeRow);
        headerStack.add(Box.createVerticalStrut(4));
        headerStack.add(contextRow);
        headerStack.add(Box.createVerticalStrut(4));
        headerStack.add(lblOrderInfo);

        topOrderBox.add(headerStack, BorderLayout.CENTER);
        panel.add(topOrderBox, BorderLayout.NORTH);

        // Invoice Items Table
        String[] invoiceCols = {"Món", "SL", "Đơn giá", "Thành tiền", "Bếp"};
        invoiceTableModel = new DefaultTableModel(invoiceCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceTable = new JTable(invoiceTableModel);
        invoiceTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        invoiceTable.setRowHeight(30);
        invoiceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        invoiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableUtils.applySmartSorting(invoiceTable);

        invoiceTable.getColumnModel().getColumn(1).setPreferredWidth(45);
        invoiceTable.getColumnModel().getColumn(4).setPreferredWidth(95);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        invoiceTable.getColumnModel().getColumn(2).setCellRenderer(right);
        invoiceTable.getColumnModel().getColumn(3).setCellRenderer(right);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        invoiceTable.getColumnModel().getColumn(1).setCellRenderer(center);

        invoiceTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = value != null ? value.toString() : "";
                if (!isSelected) {
                    if ("Chưa gửi bếp".equals(val)) {
                        c.setForeground(new Color(220, 38, 38)); // Đỏ cảnh báo
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if ("Đã xong".equals(val)) {
                        c.setForeground(new Color(16, 185, 129)); // Xanh lá
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if ("Đang nấu".equals(val)) {
                        c.setForeground(new Color(234, 88, 12)); // Cam
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    } else if ("Chờ nấu".equals(val)) {
                        c.setForeground(new Color(2, 132, 199)); // Xanh dương
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    } else {
                        c.setForeground(new Color(100, 116, 139));
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                }
                return c;
            }
        });

        JScrollPane invoiceScroll = new JScrollPane(invoiceTable);
        invoiceScroll.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));

        // Quick Row Actions (+ / - / Xóa / Ghi chú)
        JPanel itemCtrlBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        itemCtrlBar.setOpaque(false);

        btnQtyPlus = new JButton("+1");
        btnQtyMinus = new JButton("-1");
        btnRemoveItem = new JButton("Xóa");
        btnItemNote = new JButton("Ghi chú");

        btnQtyPlus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnQtyMinus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRemoveItem.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnItemNote.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        btnQtyPlus.addActionListener(e -> modifySelectedItemQty(1));
        btnQtyMinus.addActionListener(e -> modifySelectedItemQty(-1));
        btnRemoveItem.addActionListener(e -> removeSelectedItem());
        btnItemNote.addActionListener(e -> editSelectedItemNote());

        itemCtrlBar.add(btnQtyMinus);
        itemCtrlBar.add(btnQtyPlus);
        itemCtrlBar.add(btnItemNote);
        itemCtrlBar.add(btnRemoveItem);

        JPanel centerInvoiceWrapper = new JPanel(new BorderLayout(0, 4));
        centerInvoiceWrapper.setOpaque(false);
        centerInvoiceWrapper.add(invoiceScroll, BorderLayout.CENTER);
        centerInvoiceWrapper.add(itemCtrlBar, BorderLayout.SOUTH);

        panel.add(centerInvoiceWrapper, BorderLayout.CENTER);

        // Bottom Controls: CRM + Voucher + Totals + Checkout
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);

        // 1. Customer row
        JPanel custRow = new JPanel(new BorderLayout(4, 0));
        custRow.setOpaque(false);
        custRow.setBorder(new EmptyBorder(4, 0, 2, 0));

        txtCustomerPhone = new JTextField();
        txtCustomerPhone.putClientProperty("JTextField.placeholderText", "SĐT khách hàng tích điểm...");
        btnApplyCustomer = new JButton("Tìm khách");
        btnUsePoints = new JButton("Dùng điểm");
        btnUsePoints.setEnabled(false);

        btnApplyCustomer.addActionListener(e -> lookupCustomer());
        txtCustomerPhone.addActionListener(e -> lookupCustomer());
        btnUsePoints.addActionListener(e -> useCustomerPoints());

        JPanel custBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        custBtns.setOpaque(false);
        custBtns.add(btnApplyCustomer);
        custBtns.add(btnUsePoints);

        custRow.add(txtCustomerPhone, BorderLayout.CENTER);
        custRow.add(custBtns, BorderLayout.EAST);
        bottom.add(custRow);

        lblCustomerInfo = new JLabel("Khách vãng lai");
        lblCustomerInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblCustomerInfo.setForeground(new Color(100, 116, 139));
        bottom.add(lblCustomerInfo);

        // 2. Voucher row (Kết hợp cả Box chọn mã ưu đãi có sẵn & Nhập mã tay)
        JPanel voucherContainer = new JPanel(new BorderLayout(0, 4));
        voucherContainer.setOpaque(false);
        voucherContainer.setBorder(new EmptyBorder(4, 0, 4, 0));

        cbVoucherSelector = new JComboBox<>();
        cbVoucherSelector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbVoucherSelector.addActionListener(e -> onVoucherSelectedFromDropdown());

        JPanel voucherInputRow = new JPanel(new BorderLayout(4, 0));
        voucherInputRow.setOpaque(false);

        txtVoucherCode = new JTextField();
        txtVoucherCode.putClientProperty("JTextField.placeholderText", "Hoặc tự gõ mã Voucher...");
        btnApplyVoucher = new JButton("Áp dụng mã");
        btnApplyVoucher.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApplyVoucher.addActionListener(e -> applyVoucherCode());
        txtVoucherCode.addActionListener(e -> applyVoucherCode());

        btnCancelVoucher = new JButton("Hủy mã");
        btnCancelVoucher.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelVoucher.setForeground(new Color(225, 29, 72));
        btnCancelVoucher.addActionListener(e -> removeVoucher());
        btnCancelVoucher.setVisible(false);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnBox.setOpaque(false);
        btnBox.add(btnApplyVoucher);
        btnBox.add(btnCancelVoucher);

        voucherInputRow.add(txtVoucherCode, BorderLayout.CENTER);
        voucherInputRow.add(btnBox, BorderLayout.EAST);

        lblVoucherAppliedInfo = new JLabel();
        lblVoucherAppliedInfo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblVoucherAppliedInfo.setForeground(new Color(16, 185, 129));
        lblVoucherAppliedInfo.setVisible(false);

        voucherContainer.add(cbVoucherSelector, BorderLayout.NORTH);
        voucherContainer.add(voucherInputRow, BorderLayout.CENTER);
        voucherContainer.add(lblVoucherAppliedInfo, BorderLayout.SOUTH);
        bottom.add(voucherContainer);

        // 3. Totals Grid
        JPanel totalsGrid = new JPanel(new GridLayout(2, 2, 4, 4));
        totalsGrid.setOpaque(false);
        totalsGrid.setBorder(new CompoundBorder(
            new LineBorder(new Color(241, 245, 249), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));

        lblSubtotal = new JLabel("Tạm tính: 0 đ");
        lblDiscount = new JLabel("Giảm giá: 0 đ");
        lblTotal = new JLabel("TỔNG: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTotal.setForeground(new Color(225, 29, 72));

        cbPayment = new JComboBox<>(new String[]{"CASH (Tiền mặt)", "TRANSFER (Chuyển khoản)"});
        cbPayment.setFont(new Font("Segoe UI", Font.BOLD, 12));

        totalsGrid.add(lblSubtotal);
        totalsGrid.add(lblDiscount);
        totalsGrid.add(lblTotal);
        totalsGrid.add(cbPayment);
        bottom.add(totalsGrid);

        // 4. Action Buttons
        JPanel actionRow = new JPanel(new GridLayout(1, 3, 8, 0));
        actionRow.setOpaque(false);
        actionRow.setBorder(new EmptyBorder(8, 0, 4, 0));

        btnSendKitchen = new JButton("Gửi Bếp");
        btnSendKitchen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSendKitchen.setBackground(new Color(2, 132, 199));
        btnSendKitchen.setForeground(Color.WHITE);
        btnSendKitchen.setFocusPainted(false);
        btnSendKitchen.addActionListener(e -> sendToKitchen());

        btnCancel = new JButton("Hủy đơn");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setBackground(new Color(239, 68, 68));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> cancelOrder());

        btnPay = new JButton("THANH TOÁN (F9)");
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPay.setBackground(new Color(16, 185, 129));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFocusPainted(false);
        btnPay.addActionListener(e -> checkout());

        actionRow.add(btnSendKitchen);
        actionRow.add(btnCancel);
        actionRow.add(btnPay);
        bottom.add(actionRow);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // LOGIC & ACTIONS
    // =========================================================================
    private void onOrderTypeChanged() {
        currentOrderType = (OrderType) cbOrderType.getSelectedItem();
        boolean isDineIn = (currentOrderType == OrderType.DINE_IN);
        if (contextRow != null) {
            contextRow.setVisible(isDineIn);
        }
        if (btnViewFloorPlan != null) {
            btnViewFloorPlan.setVisible(isDineIn);
        }

        if (!isDineIn) {
            selectedTable = null;
            List<Order> takeaways = orderDAO.findActiveTakeawayOrders();
            currentOrder = takeaways.isEmpty() ? null : takeaways.get(0);
        } else {
            DiningTable dt = (DiningTable) cbDiningTable.getSelectedItem();
            if (dt != null) {
                switchTableOrder(dt);
            }
        }

        updateInvoiceHeader();
        loadInvoice();
        revalidate();
        repaint();
    }

    public void loadTablesAsync() {
        new SwingWorker<List<DiningTable>, Void>() {
            @Override protected List<DiningTable> doInBackground() {
                return tableController.getAllTables();
            }
            @Override protected void done() {
                try {
                    DiningTable prev = (DiningTable) cbDiningTable.getSelectedItem();
                    int prevId = prev != null ? prev.getId() : (selectedTable != null ? selectedTable.getId() : -1);
                    cachedTables = get();
                    isUpdatingTableCombo = true;
                    cbDiningTable.removeAllItems();
                    int selectIdx = -1;
                    for (int i = 0; i < cachedTables.size(); i++) {
                        DiningTable t = cachedTables.get(i);
                        cbDiningTable.addItem(t);
                        if (t.getId() == prevId) {
                            selectIdx = i;
                        }
                    }
                    if (selectIdx >= 0) {
                        cbDiningTable.setSelectedIndex(selectIdx);
                    }
                    isUpdatingTableCombo = false;

                    DiningTable currentSelected = (DiningTable) cbDiningTable.getSelectedItem();
                    if (currentSelected != null && currentOrderType == OrderType.DINE_IN) {
                        switchTableOrder(currentSelected);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void switchTableOrder(DiningTable dt) {
        if (dt == null) return;
        DiningTable fresh = tableDAO.findById(dt.getId());
        if (fresh != null) dt = fresh;
        this.selectedTable = dt;

        // Tự động hiển thị đơn hàng tương ứng của bàn này
        this.currentOrder = orderDAO.findOpenByTable(dt.getId());

        resetDiscounts();
        updateInvoiceHeader();
        loadInvoice();
        updateTableActionButtonText(dt);
    }

    private void updateTableActionButtonText(DiningTable dt) {
        if (dt == null || btnOpenOrSwitchTable == null) return;
        if (currentOrder != null || dt.getStatus() == TableStatus.OCCUPIED) {
            btnOpenOrSwitchTable.setText("Xem đơn bàn");
            btnOpenOrSwitchTable.setBackground(new Color(239, 68, 68));
        } else if (dt.getStatus() == TableStatus.RESERVED) {
            btnOpenOrSwitchTable.setText("Nhận bàn đặt");
            btnOpenOrSwitchTable.setBackground(new Color(245, 158, 11));
        } else {
            btnOpenOrSwitchTable.setText("Mở bàn mới");
            btnOpenOrSwitchTable.setBackground(new Color(16, 185, 129));
        }
    }

    public void selectDiningTable(DiningTable table) {
        // Query fresh status from DB to ensure complete sync
        DiningTable fresh = tableDAO.findById(table.getId());
        if (fresh != null) table = fresh;

        this.selectedTable = table;
        this.currentOrderType = OrderType.DINE_IN;
        cbOrderType.setSelectedItem(OrderType.DINE_IN);
        if (contextRow != null) contextRow.setVisible(true);
        if (btnViewFloorPlan != null) btnViewFloorPlan.setVisible(true);

        isUpdatingTableCombo = true;
        for (int i = 0; i < cbDiningTable.getItemCount(); i++) {
            if (cbDiningTable.getItemAt(i).getId() == table.getId()) {
                cbDiningTable.setSelectedIndex(i);
                break;
            }
        }
        isUpdatingTableCombo = false;

        if (table.getStatus() == TableStatus.OCCUPIED) {
            currentOrder = orderDAO.findOpenByTable(table.getId());
        } else if (table.getStatus() == TableStatus.RESERVED) {
            int opt = JOptionPane.showConfirmDialog(this,
                "Bàn '" + table.getName() + "' hiện đang có lịch ĐẶT TRƯỚC.\nBạn có muốn mở bàn để nhận khách vào ngồi ngay không?",
                "Bàn đã đặt trước", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                try {
                    currentOrder = tableController.openTable(table.getId(), currentUser);
                    table.setStatus(TableStatus.OCCUPIED);
                    if (onTableStatusChanged != null) onTableStatusChanged.run();
                } catch (Exception ex) {
                    currentOrder = orderDAO.findOpenByTable(table.getId());
                }
            } else {
                switchTableOrder(table);
                return;
            }
        } else {
            try {
                currentOrder = tableController.openTable(table.getId(), currentUser);
                table.setStatus(TableStatus.OCCUPIED);
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            } catch (Exception ex) {
                currentOrder = orderDAO.findOpenByTable(table.getId());
            }
        }

        resetDiscounts();
        updateInvoiceHeader();
        loadInvoice();
        updateTableActionButtonText(table);
    }

    private void onOpenOrSwitchTableClicked() {
        if (currentOrderType == OrderType.DINE_IN) {
            DiningTable t = (DiningTable) cbDiningTable.getSelectedItem();
            if (t == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một bàn ăn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectDiningTable(t);
            if (currentOrder != null && currentOrder.getDetails() != null && !currentOrder.getDetails().isEmpty()) {
                showOrderDetailPopup(t, currentOrder);
            }
        } else {
            // Takeaway
            String note = JOptionPane.showInputDialog(this, "Ghi chú đơn Mang về:", "");
            Shift curShift = shiftController.getCurrentShift();
            currentOrder = orderController.createOrder(null, currentUser, curShift, OrderType.TAKEAWAY, currentCustomer, "", "", "", note != null ? note : "");
            selectedTable = null;
            resetDiscounts();
            updateInvoiceHeader();
            loadInvoice();
        }
    }

    private void showOrderDetailPopup(DiningTable t, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("       CHI TIẾT ĐƠN HÀNG - ").append(t.getName()).append("\n");
        sb.append("=========================================\n");
        sb.append("Mã đơn hàng  : #").append(order.getId()).append("\n");
        sb.append("Trạng thái   : ").append(order.getStatus()).append("\n");
        sb.append("Loại đơn     : ").append(order.getOrderType().getDisplayName()).append("\n");
        sb.append("-----------------------------------------\n");
        sb.append(String.format("%-22s %4s %12s\n", "Tên món", "SL", "Tiến độ bếp"));
        sb.append("-----------------------------------------\n");
        for (OrderDetail d : order.getDetails()) {
            String mName = d.getItem() != null ? d.getItem().getName() : "Món #" + d.getId();
            if (mName.length() > 20) mName = mName.substring(0, 19) + "...";
            String kStatus;
            if (!d.isSentToKitchen()) {
                kStatus = "Chưa gửi bếp";
            } else {
                kStatus = switch (d.getStatus() != null ? d.getStatus() : ItemStatus.PENDING) {
                    case PENDING -> "Chờ nấu";
                    case ACCEPTED -> "Đã nhận";
                    case COOKING -> "Đang nấu";
                    case DONE -> "Đã xong";
                    case REJECTED -> "Từ chối";
                };
            }
            sb.append(String.format("%-22s %4d %12s\n", mName, d.getQuantity(), kStatus));
            if (d.getNote() != null && !d.getNote().trim().isEmpty()) {
                sb.append("  ↳ Ghi chú: ").append(d.getNote().trim()).append("\n");
            }
        }
        sb.append("-----------------------------------------\n");
        sb.append("Tổng tiền tạm tính: ").append(CurrencyFormat.format(order.getTotalAmount())).append("\n");
        sb.append("=========================================\n");

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Chi Tiết Đơn Hàng - " + t.getName(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void quickAddMenuItem(MenuItem item, int qty, String note) {
        if (currentOrder == null) {
            if (currentOrderType == OrderType.DINE_IN) {
                DiningTable t = selectedTable != null ? selectedTable : (DiningTable) cbDiningTable.getSelectedItem();
                if (t != null) {
                    try {
                        currentOrder = tableController.openTable(t.getId(), currentUser);
                        t.setStatus(TableStatus.OCCUPIED);
                        selectedTable = t;
                        if (onTableStatusChanged != null) onTableStatusChanged.run();
                    } catch (Exception ex) {
                        currentOrder = orderDAO.findOpenByTable(t.getId());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn ăn trước khi chọn món!", "Chưa chọn bàn", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                Shift curShift = shiftController.getCurrentShift();
                currentOrder = orderController.createOrder(null, currentUser, curShift, OrderType.TAKEAWAY, currentCustomer, "", "", "", note != null ? note : "");
            }
            updateInvoiceHeader();
        }

        if (currentOrder == null) {
            JOptionPane.showMessageDialog(this, "Chưa thể tạo đơn hàng. Vui lòng mở bàn hoặc tạo đơn mới trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Check if item already in details
            OrderDetail existing = null;
            if (currentOrder.getDetails() != null) {
                for (OrderDetail d : currentOrder.getDetails()) {
                    if (d.getItem() != null && d.getItem().getId() == item.getId() && !d.isSentToKitchen() && d.getStatus() == ItemStatus.PENDING) {
                        existing = d;
                        break;
                    }
                }
            }

            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + qty);
                orderController.updateItemQuantity(existing.getId(), existing.getQuantity());
            } else {
                orderController.addItem(currentOrder, item, qty, note);
            }

            loadInvoice();
            if (selectedTable != null) {
                updateTableActionButtonText(selectedTable);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thêm món vào đơn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modifySelectedItemQty(int delta) {
        int viewRow = invoiceTable.getSelectedRow();
        if (viewRow < 0 || currentOrder == null) return;
        int r = invoiceTable.convertRowIndexToModel(viewRow);
        if (r >= currentOrder.getDetails().size()) return;
        OrderDetail d = currentOrder.getDetails().get(r);
        if (delta < 0 && (d.getStatus() == ItemStatus.COOKING || d.getStatus() == ItemStatus.DONE)) {
            JOptionPane.showMessageDialog(this, "Không thể giảm số lượng món đang nấu hoặc đã hoàn thành!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int newQty = d.getQuantity() + delta;
        if (newQty <= 0) {
            removeSelectedItem();
        } else {
            orderController.updateItemQuantity(d.getId(), newQty);
            loadInvoice();
        }
    }

    private void removeSelectedItem() {
        int viewRow = invoiceTable.getSelectedRow();
        if (viewRow < 0 || currentOrder == null) return;
        int r = invoiceTable.convertRowIndexToModel(viewRow);
        if (r >= currentOrder.getDetails().size()) return;
        OrderDetail d = currentOrder.getDetails().get(r);
        if (d.getStatus() == ItemStatus.COOKING || d.getStatus() == ItemStatus.DONE) {
            JOptionPane.showMessageDialog(this, "Không thể xóa món đang nấu hoặc đã hoàn thành từ bếp!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        orderController.removeItemFromOrder(d.getId());
        loadInvoice();
    }

    private void editSelectedItemNote() {
        int viewRow = invoiceTable.getSelectedRow();
        if (viewRow < 0 || currentOrder == null) return;
        int r = invoiceTable.convertRowIndexToModel(viewRow);
        if (r >= currentOrder.getDetails().size()) return;
        OrderDetail d = currentOrder.getDetails().get(r);
        String itemName = d.getItem() != null ? d.getItem().getName() : "";
        String note = JOptionPane.showInputDialog(this, "Nhập ghi chú chế biến cho món '" + itemName + "' (VD: không hành, ít cay, ít ngọt...):", d.getNote() != null ? d.getNote() : "");
        if (note != null) {
            String clean = note.trim();
            d.setNote(clean);
            orderController.updateDetailNote(d.getId(), clean);
            loadInvoice();
        }
    }

    private void loadInvoice() {
        invoiceTableModel.setRowCount(0);
        if (currentOrder == null) {
            updateTotal();
            return;
        }

        currentOrder = orderDAO.findById(currentOrder.getId());
        if (currentOrder == null) {
            updateTotal();
            return;
        }

        for (OrderDetail d : currentOrder.getDetails()) {
            String name = d.getItem() != null ? d.getItem().getName() : "Món #" + d.getId();
            if (d.getNote() != null && !d.getNote().trim().isEmpty()) {
                name += " [" + d.getNote().trim() + "]";
            }
            String kitchenStatus;
            if (!d.isSentToKitchen()) {
                kitchenStatus = "Chưa gửi bếp";
            } else {
                kitchenStatus = switch (d.getStatus() != null ? d.getStatus() : ItemStatus.PENDING) {
                    case PENDING -> "Chờ nấu";
                    case ACCEPTED -> "Đã nhận";
                    case COOKING -> "Đang nấu";
                    case DONE -> "Đã xong";
                    case REJECTED -> "Từ chối";
                };
            }
            invoiceTableModel.addRow(new Object[]{
                name,
                d.getQuantity(),
                CurrencyFormat.format(d.getUnitPrice()),
                CurrencyFormat.format(d.getSubtotal()),
                kitchenStatus
            });
        }
        updateTotal();
    }

    private void updateTotal() {
        if (currentOrder == null) {
            lblSubtotal.setText("Tạm tính: 0 đ");
            lblDiscount.setText("Giảm giá: 0 đ");
            lblTotal.setText("TỔNG: 0 đ");
            return;
        }

        BigDecimal sub = currentOrder.getDetails().stream()
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (currentVoucher != null) {
            discount = currentVoucher.computeDiscount(sub);
        } else if (usedPoints > 0) {
            discount = new BigDecimal(usedPoints * 100);
        }

        BigDecimal total = sub.subtract(discount).max(BigDecimal.ZERO);

        lblSubtotal.setText("Tạm tính: " + CurrencyFormat.format(sub));
        lblDiscount.setText("Giảm giá: " + CurrencyFormat.format(discount));
        lblTotal.setText("TỔNG: " + CurrencyFormat.format(total));
    }

    private void updateInvoiceHeader() {
        if (currentOrder != null) {
            String typeStr = currentOrder.getOrderType() != null ? currentOrder.getOrderType().getDisplayName() : "Tại bàn";
            String loc = (currentOrder.getOrderType() == OrderType.DINE_IN && selectedTable != null)
                ? selectedTable.getName()
                : "Mang về";
            lblOrderInfo.setText("Đơn #" + currentOrder.getId() + " • " + loc + " • " + typeStr);
        } else {
            if (currentOrderType == OrderType.DINE_IN && selectedTable != null) {
                lblOrderInfo.setText(selectedTable.getName() + " • Trống (Chọn món để bắt đầu)");
            } else if (currentOrderType == OrderType.TAKEAWAY) {
                lblOrderInfo.setText("Đơn Mang về (Chọn món để bắt đầu)");
            } else {
                lblOrderInfo.setText("Chưa có đơn hàng đang mở");
            }
        }
    }

    private void resetDiscounts() {
        currentCustomer = null;
        currentVoucher = null;
        usedPoints = 0;
        txtCustomerPhone.setText("");
        txtVoucherCode.setText("");
        if (cbVoucherSelector != null && cbVoucherSelector.getItemCount() > 0) {
            isUpdatingVoucherCombo = true;
            cbVoucherSelector.setSelectedIndex(0);
            isUpdatingVoucherCombo = false;
        }
        if (lblVoucherAppliedInfo != null) lblVoucherAppliedInfo.setVisible(false);
        if (btnCancelVoucher != null) btnCancelVoucher.setVisible(false);
        lblCustomerInfo.setText("Khách vãng lai");
        btnUsePoints.setEnabled(false);
    }

    private void lookupCustomer() {
        String phone = txtCustomerPhone.getText().trim();
        if (phone.isEmpty()) return;
        new SwingWorker<Customer, Void>() {
            @Override protected Customer doInBackground() { return customerController.findByPhone(phone); }
            @Override protected void done() {
                try {
                    currentCustomer = get();
                    if (currentCustomer != null) {
                        lblCustomerInfo.setText("<html><b>" + currentCustomer.getName() + "</b> ("
                                + currentCustomer.getMembershipTier().getDisplayName() + ") • "
                                + currentCustomer.getLoyaltyPoints() + " điểm</html>");
                        btnUsePoints.setEnabled(currentCustomer.getLoyaltyPoints() > 0);
                        if (currentOrder != null) {
                            currentOrder.setCustomerId(currentCustomer.getId());
                        }
                    } else {
                        lblCustomerInfo.setText("Chưa tìm thấy khách (SĐT mới)");
                        btnUsePoints.setEnabled(false);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void useCustomerPoints() {
        if (currentCustomer == null || currentCustomer.getLoyaltyPoints() <= 0) return;
        String pStr = JOptionPane.showInputDialog(this,
            "Khách hiện có " + currentCustomer.getLoyaltyPoints() + " điểm.\nNhập số điểm muốn sử dụng (100 điểm = 10.000đ):",
            String.valueOf(currentCustomer.getLoyaltyPoints()));
        if (pStr == null) return;
        try {
            int p = Integer.parseInt(pStr.trim());
            if (p < 0 || p > currentCustomer.getLoyaltyPoints()) throw new Exception();
            this.usedPoints = p;
            updateTotal();
            JOptionPane.showMessageDialog(this, "Đã áp dụng giảm " + CurrencyFormat.format(new BigDecimal(p * 100)) + " từ điểm thưởng!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Số điểm không hợp lệ!");
        }
    }

    public void loadActiveVouchers() {
        new SwingWorker<List<Voucher>, Void>() {
            @Override protected List<Voucher> doInBackground() {
                List<Voucher> all = voucherDAO.findAll();
                LocalDate today = LocalDate.now();
                List<Voucher> valid = new ArrayList<>();
                for (Voucher v : all) {
                    if (v.isActive()) {
                        if (v.getEndDate() != null && v.getEndDate().isBefore(today)) continue;
                        if (v.getUsageLimit() > 0 && v.getUsedCount() >= v.getUsageLimit()) continue;
                        valid.add(v);
                    }
                }
                return valid;
            }
            @Override protected void done() {
                try {
                    activeVouchers = get();
                    isUpdatingVoucherCombo = true;
                    cbVoucherSelector.removeAllItems();
                    cbVoucherSelector.addItem("-- Chọn mã Voucher có sẵn --");
                    for (Voucher v : activeVouchers) {
                        String desc = v.getCode() + " • Giảm " + 
                            (v.getDiscountType() == DiscountType.PERCENT ? v.getDiscountVal() + "%" : CurrencyFormat.format(v.getDiscountVal())) +
                            " (đơn ≥ " + CurrencyFormat.format(v.getMinOrderAmount()) + ")";
                        cbVoucherSelector.addItem(desc);
                    }
                    if (currentVoucher != null) {
                        for (int i = 0; i < activeVouchers.size(); i++) {
                            if (activeVouchers.get(i).getCode().equalsIgnoreCase(currentVoucher.getCode())) {
                                cbVoucherSelector.setSelectedIndex(i + 1);
                                break;
                            }
                        }
                    }
                    isUpdatingVoucherCombo = false;
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void onVoucherSelectedFromDropdown() {
        if (isUpdatingVoucherCombo || cbVoucherSelector == null) return;
        int idx = cbVoucherSelector.getSelectedIndex();
        if (idx <= 0) {
            removeVoucher();
            return;
        }
        if (activeVouchers != null && idx - 1 < activeVouchers.size()) {
            Voucher v = activeVouchers.get(idx - 1);
            txtVoucherCode.setText(v.getCode());
            applyVoucher(v);
        }
    }

    private void applyVoucher(Voucher v) {
        if (v == null || !v.isActive()) {
            JOptionPane.showMessageDialog(this, "Mã Voucher không tồn tại hoặc đã hết hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal sub = currentOrder != null ? currentOrder.getDetails().stream().map(OrderDetail::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        if (!v.isValid(sub)) {
            JOptionPane.showMessageDialog(this,
                "Đơn hàng hiện tại (" + CurrencyFormat.format(sub) + ") chưa đạt giá trị tối thiểu " +
                CurrencyFormat.format(v.getMinOrderAmount()) + " để dùng Voucher [" + v.getCode() + "]!",
                "Chưa đủ điều kiện", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.currentVoucher = v;
        updateTotal();

        BigDecimal disc = v.computeDiscount(sub);
        if (lblVoucherAppliedInfo != null) {
            lblVoucherAppliedInfo.setText("✓ Đã áp dụng: " + v.getCode() + " (-" + CurrencyFormat.format(disc) + ")");
            lblVoucherAppliedInfo.setVisible(true);
        }
        if (btnCancelVoucher != null) btnCancelVoucher.setVisible(true);

        // Sync dropdown selection if not matched
        if (activeVouchers != null) {
            for (int i = 0; i < activeVouchers.size(); i++) {
                if (activeVouchers.get(i).getCode().equalsIgnoreCase(v.getCode())) {
                    isUpdatingVoucherCombo = true;
                    cbVoucherSelector.setSelectedIndex(i + 1);
                    isUpdatingVoucherCombo = false;
                    break;
                }
            }
        }
        JOptionPane.showMessageDialog(this, "Đã áp dụng Voucher [" + v.getCode() + "] thành công!");
    }

    private void removeVoucher() {
        this.currentVoucher = null;
        txtVoucherCode.setText("");
        if (lblVoucherAppliedInfo != null) lblVoucherAppliedInfo.setVisible(false);
        if (btnCancelVoucher != null) btnCancelVoucher.setVisible(false);
        if (cbVoucherSelector != null && cbVoucherSelector.getItemCount() > 0) {
            isUpdatingVoucherCombo = true;
            cbVoucherSelector.setSelectedIndex(0);
            isUpdatingVoucherCombo = false;
        }
        updateTotal();
    }

    private void applyVoucherCode() {
        String code = txtVoucherCode.getText().trim();
        if (code.isEmpty()) return;
        Voucher v = voucherDAO.findByCode(code);
        if (v == null || !v.isActive()) {
            JOptionPane.showMessageDialog(this, "Mã Voucher '" + code + "' không tồn tại hoặc đã hết hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        applyVoucher(v);
    }

    private void sendToKitchen() {
        if (currentOrder == null || currentOrder.getDetails().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có món nào để gửi bếp!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long unsentCount = currentOrder.getDetails().stream()
            .filter(d -> !d.isSentToKitchen() && d.getStatus() != ItemStatus.REJECTED)
            .count();
        if (unsentCount == 0) {
            JOptionPane.showMessageDialog(this, "Tất cả các món trong đơn đã được gửi sang Bếp từ trước!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        orderController.sendToKitchen(currentOrder.getId());
        loadInvoice();
        JOptionPane.showMessageDialog(this, "Đã gửi thành công " + unsentCount + " món sang Màn hình Bếp!", "Gửi Bếp thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelOrder() {
        if (currentOrder == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn hủy đơn #" + currentOrder.getId() + "?", "Xác nhận hủy", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            orderController.cancelOrder(currentOrder.getId());
            if (selectedTable != null) {
                tableController.clearTable(selectedTable.getId());
                selectedTable = null;
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            }
            currentOrder = null;
            resetDiscounts();
            updateInvoiceHeader();
            loadInvoice();
            loadTablesAsync();
            JOptionPane.showMessageDialog(this, "Đã hủy đơn hàng!");
        }
    }

    private void checkout() {
        if (currentOrder == null || currentOrder.getDetails().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có đơn hàng hoặc chưa có món ăn để thanh toán!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Kiểm tra xem có món nào chưa gửi sang bếp không -> BẮT BUỘC CHẶN THANH TOÁN
        int unsentCount = 0;
        try (java.sql.Connection conn = com.pos.utils.DatabaseConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM order_details WHERE order_id = ? AND (sent_to_kitchen = 0 OR sent_to_kitchen IS NULL) AND status != 'REJECTED'")) {
            ps.setInt(1, currentOrder.getId());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) unsentCount = rs.getInt(1);
            }
        } catch (Exception ignored) {}

        if (unsentCount > 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Đơn hàng đang có " + unsentCount + " món CHƯA GỬI sang Bếp chế biến!\n" +
                "Bạn có muốn gửi ngay sang Bếp bây giờ không?",
                "Chưa gửi món sang Bếp", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                sendToKitchen();
            }
            return; // Chặn thanh toán hoàn toàn!
        }

        // 2. Kiểm tra xem bếp đã hoàn thành tất cả các món chưa
        try (java.sql.Connection conn = com.pos.utils.DatabaseConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM order_details WHERE order_id = ? AND sent_to_kitchen = 1 AND status NOT IN ('DONE', 'REJECTED')")) {
            ps.setInt(1, currentOrder.getId());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    int pendingCount = rs.getInt(1);
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Còn " + pendingCount + " món chưa được bếp báo hoàn thành!\n" +
                        "Bạn có chắc muốn thanh toán ngay không?",
                        "Cảnh báo - Bếp chưa xong", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) return;
                }
            }
        } catch (Exception ignored) {}

        String payType = ((String) cbPayment.getSelectedItem()).startsWith("CASH") ? "CASH" : "TRANSFER";

        int confirm = JOptionPane.showConfirmDialog(this,
            lblTotal.getText() + "\n\nXác nhận thanh toán phương thức: " + payType + "?\n(Hệ thống sẽ tự động in Bill nhiệt ESC/POS và xuất PDF)",
            "Xác nhận thanh toán", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            DiscountStrategy strategy = new NoDiscount();
            if (currentVoucher != null) {
                strategy = new VoucherDiscountStrategy(currentVoucher);
            } else if (usedPoints > 0) {
                strategy = new LoyaltyPointsDiscount(usedPoints);
            }

            String pdfPath = orderController.checkout(currentOrder, payType, strategy, customerController, shiftController);

            if (selectedTable != null) {
                tableController.clearTable(selectedTable.getId());
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            }

            JOptionPane.showMessageDialog(this,
                "Thanh toán thành công!\n" +
                "Đã gửi lệnh in Bill nhiệt ESC/POS.\n" +
                (pdfPath != null ? "Hóa đơn PDF: " + pdfPath : "Đã xuất hóa đơn"),
                "Thanh toán thành công", JOptionPane.INFORMATION_MESSAGE);

            currentOrder = null;
            selectedTable = null;
            resetDiscounts();
            updateInvoiceHeader();
            loadInvoice();
            loadTablesAsync();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thanh toán: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override public void onOrderChanged(OrderEvent event) {
        SwingUtilities.invokeLater(() -> {
            loadInvoice();
            loadTablesAsync();
        });
    }
    @Override public void onStockChanged(int menuItemId) { SwingUtilities.invokeLater(this::loadMenuItems); }
}
