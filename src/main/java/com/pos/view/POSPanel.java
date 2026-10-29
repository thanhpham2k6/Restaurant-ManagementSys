package com.pos.view;

import com.pos.controller.*;
import com.pos.dao.*;
import com.pos.model.*;
import com.pos.model.MenuItem;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DishImageManager;
import com.pos.utils.WrapLayout;

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
    private JComboBox<DiningTable> cbDiningTable;
    private JButton btnOpenOrSwitchTable;
    private JButton btnViewFloorPlan;
    private JPanel deliveryInfoPanel;
    private JTextField txtDeliveryAddress;
    private JTextField txtDeliveryPhone;
    private JTextField txtDeliveryFee;
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
    private JTextField txtVoucherCode;
    private JButton btnApplyVoucher;

    // Right - Totals & Actions
    private JLabel lblSubtotal, lblDiscount, lblDeliveryFeeDisplay, lblTotal;
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
        menuTable.setAutoCreateRowSorter(true);
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

        cbOrderType = new JComboBox<>(new OrderType[]{OrderType.DINE_IN, OrderType.TAKEAWAY, OrderType.DELIVERY});
        cbOrderType.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbOrderType.setPreferredSize(new Dimension(170, 32));
        cbOrderType.addActionListener(e -> onOrderTypeChanged());

        btnViewFloorPlan = new JButton("🗺️ Sơ đồ bàn");
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

        // Row 2: Table Selector / Delivery info
        JPanel contextRow = new JPanel(new BorderLayout(6, 0));
        contextRow.setOpaque(false);

        cbDiningTable = new JComboBox<>();
        cbDiningTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbDiningTable.setPreferredSize(new Dimension(210, 32));

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

        // Delivery info container
        deliveryInfoPanel = new JPanel(new GridLayout(3, 2, 4, 4));
        deliveryInfoPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));
        deliveryInfoPanel.setOpaque(false);
        deliveryInfoPanel.setVisible(false);

        txtDeliveryPhone = new JTextField();
        txtDeliveryAddress = new JTextField();
        txtDeliveryFee = new JTextField("20000");

        deliveryInfoPanel.add(new JLabel("SĐT nhận:")); deliveryInfoPanel.add(txtDeliveryPhone);
        deliveryInfoPanel.add(new JLabel("Địa chỉ giao:")); deliveryInfoPanel.add(txtDeliveryAddress);
        deliveryInfoPanel.add(new JLabel("Phí ship (đ):")); deliveryInfoPanel.add(txtDeliveryFee);

        // Info Banner
        lblOrderInfo = new JLabel("Chưa chọn bàn / đơn hàng", SwingConstants.LEFT);
        lblOrderInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOrderInfo.setForeground(new Color(15, 118, 110));
        lblOrderInfo.setBorder(new EmptyBorder(4, 2, 4, 2));

        JPanel headerStack = new JPanel(new GridLayout(3, 1, 4, 4));
        headerStack.setOpaque(false);
        headerStack.add(typeRow);
        headerStack.add(contextRow);
        headerStack.add(lblOrderInfo);

        topOrderBox.add(headerStack, BorderLayout.NORTH);
        topOrderBox.add(deliveryInfoPanel, BorderLayout.SOUTH);
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
        invoiceTable.setAutoCreateRowSorter(true);

        invoiceTable.getColumnModel().getColumn(1).setMaxWidth(45);
        invoiceTable.getColumnModel().getColumn(4).setMaxWidth(70);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        invoiceTable.getColumnModel().getColumn(2).setCellRenderer(right);
        invoiceTable.getColumnModel().getColumn(3).setCellRenderer(right);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        invoiceTable.getColumnModel().getColumn(1).setCellRenderer(center);
        invoiceTable.getColumnModel().getColumn(4).setCellRenderer(center);

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

        // 2. Voucher row
        JPanel voucherRow = new JPanel(new BorderLayout(4, 0));
        voucherRow.setOpaque(false);
        voucherRow.setBorder(new EmptyBorder(4, 0, 4, 0));

        txtVoucherCode = new JTextField();
        txtVoucherCode.putClientProperty("JTextField.placeholderText", "Nhập mã Voucher...");
        btnApplyVoucher = new JButton("Áp dụng mã");
        btnApplyVoucher.addActionListener(e -> applyVoucherCode());
        txtVoucherCode.addActionListener(e -> applyVoucherCode());

        voucherRow.add(txtVoucherCode, BorderLayout.CENTER);
        voucherRow.add(btnApplyVoucher, BorderLayout.EAST);
        bottom.add(voucherRow);

        // 3. Totals Grid
        JPanel totalsGrid = new JPanel(new GridLayout(3, 2, 4, 2));
        totalsGrid.setOpaque(false);
        totalsGrid.setBorder(new CompoundBorder(
            new LineBorder(new Color(241, 245, 249), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));

        lblSubtotal = new JLabel("Tạm tính: 0 đ");
        lblDiscount = new JLabel("Giảm giá: 0 đ");
        lblDeliveryFeeDisplay = new JLabel("Phí ship: 0 đ");
        lblTotal = new JLabel("TỔNG: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTotal.setForeground(new Color(225, 29, 72));

        cbPayment = new JComboBox<>(new String[]{"CASH (Tiền mặt)", "TRANSFER (Chuyển khoản)"});
        cbPayment.setFont(new Font("Segoe UI", Font.BOLD, 12));

        totalsGrid.add(lblSubtotal);
        totalsGrid.add(lblDiscount);
        totalsGrid.add(lblDeliveryFeeDisplay);
        totalsGrid.add(new JLabel("Phương thức:"));
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
        boolean isDelivery = (currentOrderType == OrderType.DELIVERY);
        deliveryInfoPanel.setVisible(isDelivery);

        if (currentOrder != null) {
            currentOrder.setOrderType(currentOrderType);
            updateInvoiceHeader();
            updateTotal();
        }
    }

    public void loadTablesAsync() {
        new SwingWorker<List<DiningTable>, Void>() {
            @Override protected List<DiningTable> doInBackground() {
                return tableController.getAllTables();
            }
            @Override protected void done() {
                try {
                    cachedTables = get();
                    cbDiningTable.removeAllItems();
                    for (DiningTable t : cachedTables) {
                        cbDiningTable.addItem(t);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void selectDiningTable(DiningTable table) {
        this.selectedTable = table;
        this.currentOrderType = OrderType.DINE_IN;
        cbOrderType.setSelectedItem(OrderType.DINE_IN);

        for (int i = 0; i < cbDiningTable.getItemCount(); i++) {
            if (cbDiningTable.getItemAt(i).getId() == table.getId()) {
                cbDiningTable.setSelectedIndex(i);
                break;
            }
        }

        if (table.getStatus() == TableStatus.OCCUPIED) {
            currentOrder = orderDAO.findOpenByTable(table.getId());
        } else {
            try {
                currentOrder = tableController.openTable(table.getId(), currentUser);
                table.setStatus(TableStatus.OCCUPIED);
            } catch (Exception ex) {
                currentOrder = orderDAO.findOpenByTable(table.getId());
            }
        }

        resetDiscounts();
        updateInvoiceHeader();
        loadInvoice();
    }

    private void onOpenOrSwitchTableClicked() {
        if (currentOrderType == OrderType.DINE_IN) {
            DiningTable t = (DiningTable) cbDiningTable.getSelectedItem();
            if (t == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một bàn ăn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectDiningTable(t);
        } else {
            // Takeaway or Delivery
            String note = JOptionPane.showInputDialog(this, "Ghi chú đơn " + currentOrderType.getDisplayName() + ":", "");
            Shift curShift = shiftController.getCurrentShift();
            currentOrder = orderController.createOrder(null, currentUser, curShift, currentOrderType, currentCustomer, "", "", "", note != null ? note : "");
            selectedTable = null;
            resetDiscounts();
            updateInvoiceHeader();
            loadInvoice();
        }
    }

    private void quickAddMenuItem(MenuItem item, int qty, String note) {
        if (currentOrder == null) {
            if (currentOrderType == OrderType.DINE_IN) {
                DiningTable t = (DiningTable) cbDiningTable.getSelectedItem();
                if (t != null) {
                    selectDiningTable(t);
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn ăn hoặc chọn loại đơn Mang về trước khi chọn món!", "Chưa chọn bàn", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                onOpenOrSwitchTableClicked();
            }
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
                    if (d.getItem() != null && d.getItem().getId() == item.getId() && d.getStatus() == ItemStatus.PENDING) {
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
        String note = JOptionPane.showInputDialog(this, "Nhập ghi chú cho món '" + itemName + "':", d.getNote() != null ? d.getNote() : "");
        if (note != null) {
            d.setNote(note.trim());
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
            invoiceTableModel.addRow(new Object[]{
                name,
                d.getQuantity(),
                CurrencyFormat.format(d.getUnitPrice()),
                CurrencyFormat.format(d.getSubtotal()),
                d.getStatus() != null ? d.getStatus().name() : "PENDING"
            });
        }
        updateTotal();
    }

    private void updateTotal() {
        if (currentOrder == null) {
            lblSubtotal.setText("Tạm tính: 0 đ");
            lblDiscount.setText("Giảm giá: 0 đ");
            lblDeliveryFeeDisplay.setText("Phí ship: 0 đ");
            lblTotal.setText("TỔNG: 0 đ");
            return;
        }

        BigDecimal sub = currentOrder.getDetails().stream()
            .map(OrderDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal delFee = BigDecimal.ZERO;
        if (currentOrderType == OrderType.DELIVERY) {
            try {
                delFee = new BigDecimal(txtDeliveryFee.getText().trim());
            } catch (Exception ignored) {
                delFee = new BigDecimal("20000");
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (currentVoucher != null) {
            discount = currentVoucher.computeDiscount(sub);
        } else if (usedPoints > 0) {
            discount = new BigDecimal(usedPoints * 100);
        }

        BigDecimal total = sub.subtract(discount).max(BigDecimal.ZERO).add(delFee);

        lblSubtotal.setText("Tạm tính: " + CurrencyFormat.format(sub));
        lblDiscount.setText("Giảm giá: " + CurrencyFormat.format(discount));
        lblDeliveryFeeDisplay.setText("Phí ship: " + CurrencyFormat.format(delFee));
        lblTotal.setText("TỔNG: " + CurrencyFormat.format(total));
    }

    private void updateInvoiceHeader() {
        if (currentOrder != null) {
            String typeStr = currentOrder.getOrderType() != null ? currentOrder.getOrderType().getDisplayName() : "Tại bàn";
            String loc = selectedTable != null ? selectedTable.getName() : "Mang về / Giao hàng";
            lblOrderInfo.setText("Đơn #" + currentOrder.getId() + " • " + loc + " • " + typeStr);
        } else {
            lblOrderInfo.setText("Chưa có đơn hàng đang mở");
        }
    }

    private void resetDiscounts() {
        currentCustomer = null;
        currentVoucher = null;
        usedPoints = 0;
        txtCustomerPhone.setText("");
        txtVoucherCode.setText("");
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

    private void applyVoucherCode() {
        String code = txtVoucherCode.getText().trim();
        if (code.isEmpty()) return;
        Voucher v = voucherDAO.findByCode(code);
        if (v == null || !v.isActive()) {
            JOptionPane.showMessageDialog(this, "Mã Voucher '" + code + "' không tồn tại hoặc đã hết hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal sub = currentOrder != null ? currentOrder.getDetails().stream().map(OrderDetail::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        if (!v.isValid(sub)) {
            JOptionPane.showMessageDialog(this, "Đơn hàng chưa đạt giá trị tối thiểu " + CurrencyFormat.format(v.getMinOrderAmount()) + " để dùng Voucher!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.currentVoucher = v;
        updateTotal();
        JOptionPane.showMessageDialog(this, "Đã áp dụng Voucher [" + v.getCode() + "] thành công!");
    }

    private void sendToKitchen() {
        if (currentOrder == null || currentOrder.getDetails().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có món nào để gửi bếp!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        orderController.sendToKitchen(currentOrder.getId());
        loadInvoice();
        JOptionPane.showMessageDialog(this, "Đã gửi thông báo chế biến tới Màn hình Bếp!");
    }

    private void cancelOrder() {
        if (currentOrder == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn hủy đơn #" + currentOrder.getId() + "?", "Xác nhận hủy", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            orderController.cancelOrder(currentOrder.getId());
            if (selectedTable != null) {
                tableController.clearTable(selectedTable.getId());
                selectedTable = null;
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

    @Override public void onOrderChanged(OrderEvent event) { SwingUtilities.invokeLater(this::loadInvoice); }
    @Override public void onStockChanged(int menuItemId) { SwingUtilities.invokeLater(this::loadMenuItems); }
}
