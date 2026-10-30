package com.pos.view;

import com.pos.controller.InventoryController;
import com.pos.controller.StockObserver;
import com.pos.model.*;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DatabaseEncodingFixer;
import com.pos.utils.DateUtils;
import com.pos.utils.TableUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Quản Lý Kho Hàng Hóa Toàn Diện (Enterprise Restaurant Inventory).
 * Thiết kế giao diện thanh điều hướng Sidebar trực quan theo đúng chuẩn phần mềm quản lý kho F&B:
 * 1. Danh Sách Hàng Tồn (Tổng hợp tồn kho, KPI, cảnh báo định mức)
 * 2. Kho Hàng (Quản lý mặt hàng, đơn vị, giá vốn, định mức tối thiểu)
 * 3. Nhập Kho & Mua Hàng (Lập phiếu nhập kho, mua hàng, thêm mặt hàng mới)
 * 4. Xuất Kho & Xuất Hủy (Xuất cấp cho Bếp nấu, xuất hủy do ôi thiu hỏng)
 * 5. Trả Hàng NCC (Phiếu trả hàng lại cho nhà cung cấp)
 * 6. Hóa Đơn Đầu Vào (Chứng từ giấy tờ hóa đơn VAT đầu vào)
 * 7. Kiểm Kê Kho (Stocktake: Người kiểm kê đếm thực tế, hệ thống tính chênh lệch, cân bằng kho)
 * 8. Định Lượng Tiêu Hao (Hệ thống tự động tính toán định lượng lý thuyết từ các món đã bán)
 * 9. Nhà Cung Cấp (Quản lý danh bạ đối tác cung cấp)
 * 10. Lịch Sử Biến Động (Nhật ký Mua hàng - Nhập kho - Xuất kho - In phiếu chứng từ)
 */
public class InventoryPanel extends JPanel implements StockObserver {
    private final User currentUser;
    private final InventoryController inventoryController;

    // Card Layout
    private JPanel contentContainer;
    private CardLayout cardLayout;
    private final List<JButton> navButtons = new ArrayList<>();

    // Data lists
    private List<InventoryItem> itemsList = new ArrayList<>();
    private List<StockMovement> movementsList = new ArrayList<>();
    private List<InventoryAudit> auditsList = new ArrayList<>();
    private List<Supplier> suppliersList = new ArrayList<>();

    // 1. Danh sách hàng tồn & KPI
    private JTable currentStockTable;
    private DefaultTableModel currentStockModel;
    private JLabel lblKpiTotalItems;
    private JLabel lblKpiLowStock;
    private JLabel lblKpiTotalValuation;
    private JComboBox<String> cbStockCategoryFilter;
    private JTextField txtStockSearch;

    // 2. Kho hàng (Quản lý danh mục)
    private JTable itemTable;
    private DefaultTableModel itemModel;

    // 3. Lịch sử biến động & Lọc
    private JTable movementTable;
    private DefaultTableModel movementModel;
    private JComboBox<String> cbMovementTypeFilter;

    // 4. Kiểm kê kho (Audits)
    private JTable auditTable;
    private DefaultTableModel auditModel;

    // 5. Định lượng tiêu hao lý thuyết
    private JTable theoreticalTable;
    private DefaultTableModel theoreticalModel;

    // 6. Nhà cung cấp
    private JTable supplierTable;
    private DefaultTableModel supplierModel;

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
        // === LEFT SIDEBAR MENU (THEO THIẾT KẾ ẢNH THAM KHẢO) ===
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(225, 0));
        sidebar.setBackground(new Color(23, 55, 41)); // Xanh đậm rừng đặc trưng
        sidebar.setBorder(new EmptyBorder(10, 8, 10, 8));

        // Header Sidebar
        JPanel sideHeader = new JPanel(new BorderLayout(5, 5));
        sideHeader.setOpaque(false);
        sideHeader.setBorder(new EmptyBorder(6, 10, 14, 10));
        JLabel lblTitle = new JLabel("QUẢN LÝ KHO");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(236, 253, 245));
        JLabel lblSub = new JLabel("Hàng hóa & Nguyên vật liệu");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(167, 243, 208));
        sideHeader.add(lblTitle, BorderLayout.NORTH);
        sideHeader.add(lblSub, BorderLayout.SOUTH);
        sidebar.add(sideHeader, BorderLayout.NORTH);

        // Menu items (6 groups)
        JPanel menuPanel = new JPanel(new GridLayout(6, 1, 0, 5));
        menuPanel.setOpaque(false);

        JButton btnNavStockList = createNavButton("• Danh Sách Hàng Tồn", "CARD_STOCK_LIST");
        JButton btnNavCatalog = createNavButton("• Kho Hàng", "CARD_CATALOG");
        JButton btnNavHistory = createNavButton("• Nhập - Xuất & Lịch Sử", "CARD_HISTORY");
        JButton btnNavStocktake = createNavButton("• Kiểm Kê Kho", "CARD_STOCKTAKE");
        JButton btnNavBOM = createNavButton("• Định Lượng Tiêu Hao", "CARD_BOM");
        JButton btnNavSuppliers = createNavButton("• Nhà Cung Cấp", "CARD_SUPPLIERS");

        menuPanel.add(btnNavStockList);
        menuPanel.add(btnNavCatalog);
        menuPanel.add(btnNavHistory);
        menuPanel.add(btnNavStocktake);
        menuPanel.add(btnNavBOM);
        menuPanel.add(btnNavSuppliers);

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // Footer button: Làm mới nhanh
        JButton btnSyncAll = new JButton("Làm mới dữ liệu kho");
        btnSyncAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSyncAll.setBackground(new Color(16, 185, 129));
        btnSyncAll.setForeground(Color.WHITE);
        btnSyncAll.setFocusPainted(false);
        btnSyncAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSyncAll.addActionListener(e -> {
            DatabaseEncodingFixer.autoFixDatabaseAsync();
            loadAllDataAsync();
            JOptionPane.showMessageDialog(this, "Đã làm mới và đồng bộ toàn bộ dữ liệu kho hàng!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        JPanel sideFooter = new JPanel(new BorderLayout());
        sideFooter.setOpaque(false);
        sideFooter.setBorder(new EmptyBorder(10, 4, 4, 4));
        sideFooter.add(btnSyncAll, BorderLayout.CENTER);
        sidebar.add(sideFooter, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);

        // === RIGHT CONTENT PANELS (CARD LAYOUT) ===
        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);

        contentContainer.add(createStockListPanel(), "CARD_STOCK_LIST");
        contentContainer.add(createCatalogPanel(), "CARD_CATALOG");
        contentContainer.add(createUnifiedMovementPanel(), "CARD_HISTORY");
        contentContainer.add(createStocktakePanel(), "CARD_STOCKTAKE");
        contentContainer.add(createTheoreticalBOMPanel(), "CARD_BOM");
        contentContainer.add(createSuppliersPanel(), "CARD_SUPPLIERS");

        add(contentContainer, BorderLayout.CENTER);

        // Mặc định chọn mục đầu tiên
        selectNavButton(btnNavStockList, "CARD_STOCK_LIST");
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setForeground(new Color(241, 245, 249));
        btn.setBackground(new Color(23, 55, 41));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> selectNavButton(btn, cardName));
        navButtons.add(btn);
        return btn;
    }

    private void selectNavButton(JButton activeBtn, String cardName) {
        for (JButton b : navButtons) {
            b.setBackground(new Color(23, 55, 41));
            b.setForeground(new Color(241, 245, 249));
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
        activeBtn.setBackground(new Color(64, 100, 80)); // Highlight pill giống ảnh
        activeBtn.setForeground(Color.WHITE);
        activeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cardLayout.show(contentContainer, cardName);
    }

    // =========================================================================
    // 1. DANH SÁCH HÀNG TỒN (CURRENT STOCK & KPI VALUATION)
    // =========================================================================
    private JPanel createStockListPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Top KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 3, 12, 0));
        kpiGrid.setOpaque(false);

        lblKpiTotalItems = new JLabel("0 mặt hàng", SwingConstants.CENTER);
        lblKpiLowStock = new JLabel("0 cần nhập", SwingConstants.CENTER);
        lblKpiTotalValuation = new JLabel("0 đ", SwingConstants.CENTER);

        kpiGrid.add(createMetricCard("TỔNG MẶT HÀNG TRONG KHO", lblKpiTotalItems, new Color(59, 130, 246)));
        kpiGrid.add(createMetricCard("CẢNH BÁO DƯỚI ĐỊNH MỨC", lblKpiLowStock, new Color(239, 68, 68)));
        kpiGrid.add(createMetricCard("TỔNG GIÁ TRỊ TỒN KHO", lblKpiTotalValuation, new Color(16, 185, 129)));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setOpaque(false);

        filterBar.add(new JLabel("Tìm kiếm:"));
        txtStockSearch = new JTextField(16);
        txtStockSearch.putClientProperty("JTextField.placeholderText", "Nhập tên hàng hóa...");
        txtStockSearch.addActionListener(e -> filterStockListTable());
        filterBar.add(txtStockSearch);

        filterBar.add(new JLabel("Danh mục:"));
        cbStockCategoryFilter = new JComboBox<>(new String[]{"Tất cả danh mục", "Thịt & Hải sản", "Rau củ", "Bột & Mì", "Gia vị & Dầu", "Đồ uống", "Bao bì", "Hàng hóa"});
        cbStockCategoryFilter.addActionListener(e -> filterStockListTable());
        filterBar.add(cbStockCategoryFilter);

        JButton btnFilterSearch = createActionBtn("Lọc dữ liệu", new Color(30, 136, 229));
        btnFilterSearch.addActionListener(e -> filterStockListTable());
        filterBar.add(btnFilterSearch);

        JButton btnShowOnlyLow = createActionBtn("Chỉ xem sắp hết", new Color(225, 29, 72));
        btnShowOnlyLow.addActionListener(e -> {
            txtStockSearch.setText("");
            cbStockCategoryFilter.setSelectedIndex(0);
            applyOnlyLowStockFilter();
        });
        filterBar.add(btnShowOnlyLow);

        JButton btnExportExcel = createActionBtn("📄 In Báo Cáo Tồn Kho", new Color(100, 116, 139));
        btnExportExcel.addActionListener(e -> printCurrentStockReport());
        filterBar.add(btnExportExcel);

        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);
        topContainer.add(kpiGrid, BorderLayout.NORTH);
        topContainer.add(filterBar, BorderLayout.SOUTH);
        p.add(topContainer, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Tên Mặt Hàng", "Danh Mục", "ĐVT", "Giá Vốn (VND)", "Tồn Kho Hiện Tại", "Định Mức Tối Thiểu", "Thành Tiền Tồn Kho", "Nhà Cung Cấp", "Trạng Thái"};
        currentStockModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        currentStockTable = new JTable(currentStockModel);
        currentStockTable.setRowHeight(30);
        currentStockTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        currentStockTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(currentStockTable);
        currentStockTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        currentStockTable.getColumnModel().getColumn(1).setPreferredWidth(180);

        // Highlight alert column
        currentStockTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null && value.toString().contains("SẮP HẾT")) {
                    c.setForeground(new Color(220, 38, 38));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(new Color(22, 101, 52));
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        p.add(new JScrollPane(currentStockTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel createMetricCard(String title, JLabel valLbl, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            new EmptyBorder(10, 14, 10, 14)
        ));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.setForeground(new Color(100, 116, 139));
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valLbl.setForeground(new Color(15, 23, 42));
        card.add(t, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // 2. KHO HÀNG (CATALOG & SỬA / XÓA THEO YÊU CẦU CŨ)
    // =========================================================================
    private JPanel createCatalogPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("DANH MỤC THÔNG TIN MẶT HÀNG TRONG KHO");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(30, 41, 59));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        JButton btnEdit = createActionBtn("Chỉnh sửa mặt hàng", new Color(230, 81, 0));
        JButton btnDelete = createActionBtn("Xóa đã chọn (Checkbox)", new Color(211, 47, 47));
        JButton btnRefresh = createActionBtn("Làm mới", new Color(120, 144, 156));

        btnEdit.addActionListener(e -> showEditItemDialog());
        btnDelete.addActionListener(e -> deleteSelectedItem());
        btnRefresh.addActionListener(e -> loadItemsAsync());

        btnBar.add(btnEdit);
        btnBar.add(btnDelete);
        btnBar.add(btnRefresh);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Chọn", "ID", "Tên Hàng Hóa", "Danh Mục", "Đơn Vị Tính", "Giá Vốn (VND)", "Tồn Kho", "Định Mức Tối Thiểu", "Nhà Cung Cấp", "Cảnh Báo"};
        itemModel = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int col) {
                if (col == 0) return Boolean.class;
                return super.getColumnClass(col);
            }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        itemTable = new JTable(itemModel);
        itemTable.setRowHeight(30);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(itemTable);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        itemTable.getColumnModel().getColumn(0).setMaxWidth(60);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        p.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // 3. KIỂM KÊ KHO (STOCKTAKE: DO NGƯỜI KIỂM KÊ THỰC HIỆN)
    // =========================================================================
    private JPanel createStocktakePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel desc = new JLabel("<html><b>KIỂM KÊ KHO THỰC TẾ & CÂN BẰNG TỒN KHO</b><br>" +
            "<span style='color:#64748b;font-size:11px;'>Người kiểm kê kiểm đếm số lượng thực tế trong kho để chốt số liệu và xử lý hao hụt.</span></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnStartAudit = createActionBtn("Tạo đợt kiểm kho mới", new Color(142, 36, 170));
        JButton btnViewDetail = createActionBtn("Xem chi tiết đợt kiểm", new Color(30, 136, 229));
        JButton btnRefresh = createActionBtn("Làm mới", new Color(120, 144, 156));

        btnStartAudit.addActionListener(e -> showStocktakeDialog());
        btnViewDetail.addActionListener(e -> showAuditDetailsDialog());
        btnRefresh.addActionListener(e -> loadAuditsAsync());

        btnBar.add(btnStartAudit);
        btnBar.add(btnViewDetail);
        btnBar.add(btnRefresh);

        topBar.add(desc, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Mã Kiểm Kho", "Người Kiểm", "Ngày Giờ Kiểm", "Trạng Thái", "Ghi Chú Đợt Kiểm"};
        auditModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditModel);
        auditTable.setRowHeight(30);
        auditTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        auditTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(auditTable);
        auditTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        p.add(new JScrollPane(auditTable), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // 8. ĐỊNH LƯỢNG TIÊU HAO LÝ THUYẾT (HỆ THỐNG TỰ ĐỘNG TÍNH)
    // =========================================================================
    private JPanel createTheoreticalBOMPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel desc = new JLabel("<html><b>TÍNH TOÁN ĐỊNH LƯỢNG TIÊU HAO TỰ ĐỘNG (THEORETICAL USAGE)</b><br>" +
            "<span style='color:#64748b;font-size:11px;'>Hệ thống tự động tính lượng nguyên liệu lý thuyết đã dùng dựa trên các món đã bán trong ngày để người kiểm kê đối chiếu.</span></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        JButton btnApplyToStocktake = createActionBtn("Đưa vào Phiếu Kiểm Kê", new Color(16, 185, 129));
        JButton btnRefreshBOM = createActionBtn("Tính toán lại hôm nay", new Color(30, 136, 229));

        btnApplyToStocktake.addActionListener(e -> showStocktakeDialog());
        btnRefreshBOM.addActionListener(e -> loadTheoreticalUsageAsync());

        btnBar.add(btnApplyToStocktake);
        btnBar.add(btnRefreshBOM);
        topBar.add(desc, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID Hàng Hóa", "Tên Nguyên Liệu", "ĐVT", "Tồn Sổ Sách Hiện Tại", "Số Món Đã Bán Hôm Nay", "Tiêu Hao Lý Thuyết Ước Tính", "Tồn Lý Thuyết Còn Lại", "Ghi Chú Đối Soát"};
        theoreticalModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        theoreticalTable = new JTable(theoreticalModel);
        theoreticalTable.setRowHeight(30);
        theoreticalTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        theoreticalTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(theoreticalTable);

        p.add(new JScrollPane(theoreticalTable), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // 9. NHÀ CUNG CẤP (SUPPLIERS)
    // =========================================================================
    private JPanel createSuppliersPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("DANH SÁCH NHÀ CUNG CẤP ĐỐI TÁC");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        JButton btnAdd = createActionBtn("+ Thêm Nhà cung cấp", new Color(46, 125, 50));
        JButton btnEdit = createActionBtn("Sửa", new Color(230, 81, 0));
        JButton btnDelete = createActionBtn("Xóa", new Color(211, 47, 47));
        JButton btnRefresh = createActionBtn("Làm mới", new Color(120, 144, 156));

        btnAdd.addActionListener(e -> showAddSupplierDialog());
        btnEdit.addActionListener(e -> showEditSupplierDialog());
        btnDelete.addActionListener(e -> deleteSelectedSupplier());
        btnRefresh.addActionListener(e -> loadSuppliersAsync());

        btnBar.add(btnAdd);
        btnBar.add(btnEdit);
        btnBar.add(btnDelete);
        btnBar.add(btnRefresh);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(btnBar, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Tên Nhà Cung Cấp", "Người Liên Hệ", "Số Điện Thoại", "Email", "Địa Chỉ"};
        supplierModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        supplierTable = new JTable(supplierModel);
        supplierTable.setRowHeight(30);
        supplierTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        supplierTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(supplierTable);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        p.add(new JScrollPane(supplierTable), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // 3. NHẬP - XUẤT KHO & LỊCH SỬ BIẾN ĐỘNG (UNIFIED MOVEMENTS)
    // =========================================================================
    private JPanel createUnifiedMovementPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Top Action Bar
        JPanel topBox = new JPanel(new BorderLayout(8, 8));
        topBox.setOpaque(false);

        JLabel title = new JLabel("QUẢN LÝ NHẬP - XUẤT KHO & LỊCH SỬ BIẾN ĐỘNG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(30, 41, 59));

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionBtns.setOpaque(false);
        JButton btnImport = createActionBtn("+ Nhập Kho / Mua Hàng", new Color(16, 185, 129));
        JButton btnExport = createActionBtn("Xuất Cấp Bếp", new Color(234, 88, 12));
        JButton btnWaste = createActionBtn("Xuất Hủy (Hỏng/Hết hạn)", new Color(220, 38, 38));
        JButton btnReturn = createActionBtn("Trả Hàng NCC", new Color(180, 83, 9));

        btnImport.addActionListener(e -> showImportDialog());
        btnExport.addActionListener(e -> showExportKitchenDialog());
        btnWaste.addActionListener(e -> showExportWasteDialog());
        btnReturn.addActionListener(e -> showReturnSupplierDialog());

        actionBtns.add(btnImport);
        actionBtns.add(btnExport);
        actionBtns.add(btnWaste);
        actionBtns.add(btnReturn);

        topBox.add(title, BorderLayout.WEST);
        topBox.add(actionBtns, BorderLayout.EAST);

        // Filter Bar
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setOpaque(false);

        JPanel filterP = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterP.setOpaque(false);
        filterP.add(new JLabel("Lọc theo loại:"));
        cbMovementTypeFilter = new JComboBox<>(new String[]{
            "Tất cả biến động", "Chỉ xem Nhập kho (Mua hàng)", "Chỉ xem Xuất kho (Bếp)", "Chỉ xem Xuất hủy", "Chỉ xem Trả hàng NCC", "Chỉ xem Cân bằng kiểm kê"
        });
        cbMovementTypeFilter.addActionListener(e -> filterMovementsTable());
        filterP.add(cbMovementTypeFilter);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        JButton btnPrintSlip = createActionBtn("In / Xem Phiếu Chứng Từ", new Color(139, 92, 246));
        JButton btnRefresh = createActionBtn("Làm mới", new Color(120, 144, 156));

        btnPrintSlip.addActionListener(e -> showMovementSlipDialog());
        btnRefresh.addActionListener(e -> loadMovementsAsync());

        btnBar.add(btnPrintSlip);
        btnBar.add(btnRefresh);

        filterBar.add(filterP, BorderLayout.WEST);
        filterBar.add(btnBar, BorderLayout.EAST);

        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);
        topContainer.add(topBox, BorderLayout.NORTH);
        topContainer.add(filterBar, BorderLayout.SOUTH);

        p.add(topContainer, BorderLayout.NORTH);

        String[] cols = {"ID", "Thời Gian", "Mặt Hàng", "Loại Biến Động", "Số Lượng", "Đơn Giá", "Thành Tiền", "Nhà Cung Cấp", "Người Thực Hiện", "Ghi Chú"};
        movementModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        movementTable = new JTable(movementModel);
        movementTable.setRowHeight(30);
        movementTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        movementTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        TableUtils.applySmartSorting(movementTable);

        p.add(new JScrollPane(movementTable), BorderLayout.CENTER);
        return p;
    }

    private JButton createActionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // =========================================================================
    // ASYNC LOADERS & FILTERS
    // =========================================================================
    public void loadAllDataAsync() {
        loadItemsAsync();
        loadMovementsAsync();
        loadAuditsAsync();
        loadSuppliersAsync();
        loadTheoreticalUsageAsync();
    }

    private void loadItemsAsync() {
        new SwingWorker<List<InventoryItem>, Void>() {
            @Override protected List<InventoryItem> doInBackground() { return inventoryController.getActiveItems(); }
            @Override protected void done() {
                try {
                    itemsList = get();
                    filterStockListTable();

                    // Load Tab Catalog
                    itemModel.setRowCount(0);
                    for (InventoryItem i : itemsList) {
                        boolean isLow = i.isLowStock();
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
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterStockListTable() {
        if (itemsList == null) return;
        String keyword = txtStockSearch != null ? txtStockSearch.getText().trim().toLowerCase() : "";
        String catFilter = cbStockCategoryFilter != null ? (String) cbStockCategoryFilter.getSelectedItem() : "Tất cả danh mục";

        currentStockModel.setRowCount(0);
        int lowCount = 0;
        BigDecimal totalValuation = BigDecimal.ZERO;

        for (InventoryItem i : itemsList) {
            boolean matchKey = keyword.isEmpty() || i.getName().toLowerCase().contains(keyword);
            boolean matchCat = catFilter == null || catFilter.equals("Tất cả danh mục") || (i.getCategory() != null && i.getCategory().equalsIgnoreCase(catFilter));

            boolean isLow = i.isLowStock();
            if (isLow) lowCount++;

            BigDecimal itemVal = i.getStockQty().multiply(i.getCostPrice() != null ? i.getCostPrice() : BigDecimal.ZERO);
            totalValuation = totalValuation.add(itemVal);

            if (matchKey && matchCat) {
                currentStockModel.addRow(new Object[]{
                    i.getId(),
                    i.getName(),
                    i.getCategory() != null ? i.getCategory() : "-",
                    i.getUnit(),
                    CurrencyFormat.format(i.getCostPrice()),
                    i.getStockQty(),
                    i.getMinQty(),
                    CurrencyFormat.format(itemVal),
                    i.getSupplierName() != null ? i.getSupplierName() : "-",
                    isLow ? "[!] SẮP HẾT HÀNG" : "Đủ dùng"
                });
            }
        }

        if (lblKpiTotalItems != null) lblKpiTotalItems.setText(itemsList.size() + " mặt hàng");
        if (lblKpiLowStock != null) lblKpiLowStock.setText(lowCount + " cần nhập");
        if (lblKpiTotalValuation != null) lblKpiTotalValuation.setText(CurrencyFormat.format(totalValuation));
    }

    private void applyOnlyLowStockFilter() {
        if (itemsList == null) return;
        currentStockModel.setRowCount(0);
        for (InventoryItem i : itemsList) {
            if (i.isLowStock()) {
                BigDecimal itemVal = i.getStockQty().multiply(i.getCostPrice() != null ? i.getCostPrice() : BigDecimal.ZERO);
                currentStockModel.addRow(new Object[]{
                    i.getId(),
                    i.getName(),
                    i.getCategory() != null ? i.getCategory() : "-",
                    i.getUnit(),
                    CurrencyFormat.format(i.getCostPrice()),
                    i.getStockQty(),
                    i.getMinQty(),
                    CurrencyFormat.format(itemVal),
                    i.getSupplierName() != null ? i.getSupplierName() : "-",
                    "[!] SẮP HẾT HÀNG"
                });
            }
        }
    }

    private void loadMovementsAsync() {
        new SwingWorker<List<StockMovement>, Void>() {
            @Override protected List<StockMovement> doInBackground() { return inventoryController.getAllMovements(); }
            @Override protected void done() {
                try {
                    movementsList = get();
                    filterMovementsTable();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterMovementsTable() {
        if (movementsList == null) return;
        String filter = cbMovementTypeFilter != null ? (String) cbMovementTypeFilter.getSelectedItem() : "Tất cả biến động";
        movementModel.setRowCount(0);

        for (StockMovement sm : movementsList) {
            boolean keep = true;
            if (filter != null) {
                if (filter.contains("Nhập kho") && sm.getType() != MovementType.IMPORT) keep = false;
                else if (filter.contains("Xuất kho") && sm.getType() != MovementType.EXPORT) keep = false;
                else if (filter.contains("Xuất hủy") && sm.getType() != MovementType.WASTE) keep = false;
                else if (filter.contains("Trả hàng") && sm.getType() != MovementType.RETURN) keep = false;
                else if (filter.contains("Cân bằng") && sm.getType() != MovementType.ADJUSTMENT && sm.getType() != MovementType.ADJUST) keep = false;
            }
            if (keep) {
                BigDecimal total = sm.getQuantity().abs().multiply(sm.getUnitPrice() != null ? sm.getUnitPrice() : BigDecimal.ZERO);
                movementModel.addRow(new Object[]{
                    sm.getId(),
                    sm.getCreatedAt() != null ? DateUtils.format(sm.getCreatedAt()) : "-",
                    sm.getItemName(),
                    sm.getType().getDisplayName(),
                    sm.getQuantity(),
                    CurrencyFormat.format(sm.getUnitPrice()),
                    CurrencyFormat.format(total),
                    sm.getSupplierName() != null ? sm.getSupplierName() : "-",
                    sm.getUserFullName() != null ? sm.getUserFullName() : "-",
                    sm.getNote() != null ? sm.getNote() : ""
                });
            }
        }
    }

    private void loadAuditsAsync() {
        new SwingWorker<List<InventoryAudit>, Void>() {
            @Override protected List<InventoryAudit> doInBackground() { return inventoryController.getAllAudits(); }
            @Override protected void done() {
                try {
                    auditsList = get();
                    auditModel.setRowCount(0);
                    for (InventoryAudit a : auditsList) {
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

    private void loadTheoreticalUsageAsync() {
        new SwingWorker<List<TheoreticalUsage>, Void>() {
            @Override protected List<TheoreticalUsage> doInBackground() {
                return inventoryController.getTheoreticalUsagesToday();
            }
            @Override protected void done() {
                try {
                    List<TheoreticalUsage> list = get();
                    theoreticalModel.setRowCount(0);
                    for (TheoreticalUsage tu : list) {
                        String note = tu.getDishesSoldCount() > 0
                            ? "Đã bán " + tu.getDishesSoldCount() + " món liên quan hôm nay"
                            : "Chưa phát sinh món bán trong ngày";
                        theoreticalModel.addRow(new Object[]{
                            tu.getItemId(),
                            tu.getItemName(),
                            tu.getUnit(),
                            tu.getCurrentStock(),
                            tu.getDishesSoldCount(),
                            tu.getTheoreticalUsedQty() + " " + tu.getUnit(),
                            tu.getEstimatedRemaining() + " " + tu.getUnit(),
                            note
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

    // =========================================================================
    // DIALOGS: SỬA HÀNG HÓA & XÓA CHECKBOX
    // =========================================================================
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
        List<Integer> idsToDelete = new ArrayList<>();
        List<String> namesToDelete = new ArrayList<>();
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

    // =========================================================================
    // DIALOGS: NHẬP KHO, XUẤT KHO, XUẤT HỦY, TRẢ HÀNG
    // =========================================================================
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
        JTextField txtInvoiceNo = new JTextField("HD-" + System.currentTimeMillis() % 100000);
        JTextField txtNote = new JTextField("Nhập hàng từ nhà cung cấp");

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

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JPanel modeP = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        modeP.add(rbExisting); modeP.add(rbNew);
        p.add(modeP, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.gridx = 0; p.add(new JLabel("Mặt hàng có sẵn:"), gbc);
        gbc.gridx = 1; p.add(cbItem, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0; p.add(new JLabel("Tên mặt hàng MỚI:"), gbc);
        gbc.gridx = 1; p.add(txtNewName, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0; p.add(new JLabel("Danh mục / Đơn vị tính:"), gbc);
        JPanel subP = new JPanel(new GridLayout(1, 2, 4, 0));
        subP.add(txtNewCat); subP.add(txtNewUnit);
        gbc.gridx = 1; p.add(subP, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0; p.add(new JLabel("Định mức tồn tối thiểu:"), gbc);
        gbc.gridx = 1; p.add(txtNewMin, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0; p.add(new JLabel("Nhà cung cấp:"), gbc);
        gbc.gridx = 1; p.add(cbSup, gbc);

        gbc.gridy = 6;
        gbc.gridx = 0; p.add(new JLabel("Số lượng & Đơn giá nhập:"), gbc);
        JPanel qp = new JPanel(new GridLayout(1, 2, 4, 0));
        qp.add(txtQty); qp.add(txtPrice);
        gbc.gridx = 1; p.add(qp, gbc);

        gbc.gridy = 7;
        gbc.gridx = 0; p.add(new JLabel("Số hóa đơn / Chứng từ:"), gbc);
        gbc.gridx = 1; p.add(txtInvoiceNo, gbc);

        gbc.gridy = 8;
        gbc.gridx = 0; p.add(new JLabel("Ghi chú nhập kho:"), gbc);
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

                String fullNote = "[HĐ: " + txtInvoiceNo.getText().trim() + "] " + txtNote.getText().trim();
                inventoryController.importStock(itemId, s != null ? s.getId() : null, qty, price, currentUser, fullNote);
                loadAllDataAsync();

                int pOpt = JOptionPane.showConfirmDialog(this,
                    "Nhập kho thành công!\nBạn có muốn xem và in Phiếu Nhập Kho chứng từ giấy tờ ngay bây giờ không?",
                    "In Chứng Từ Giấy Tờ", JOptionPane.YES_NO_OPTION);
                if (pOpt == JOptionPane.YES_OPTION) {
                    List<StockMovement> movs = inventoryController.getAllMovements();
                    if (!movs.isEmpty()) showMovementSlipDialog(movs.get(0));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showExportKitchenDialog() {
        if (itemsList == null || itemsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kho chưa có mặt hàng nào.");
            return;
        }
        JComboBox<InventoryItem> cbItem = new JComboBox<>(itemsList.toArray(new InventoryItem[0]));
        JTextField txtQty = new JTextField("5");
        JTextField txtNote = new JTextField("Cấp nguyên liệu cho Bếp nấu ca sáng");

        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Mặt hàng xuất cho Bếp:")); p.add(cbItem);
        p.add(new JLabel("Số lượng xuất:")); p.add(txtQty);
        p.add(new JLabel("Ghi chú xuất ca:")); p.add(txtNote);

        int res = JOptionPane.showConfirmDialog(this, p, "Phiếu Xuất Kho Cấp Cho Bếp", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = (InventoryItem) cbItem.getSelectedItem();
                BigDecimal qty = new BigDecimal(txtQty.getText().trim());
                inventoryController.exportStock(item.getId(), qty, currentUser, "Xuất bếp: " + txtNote.getText().trim());
                JOptionPane.showMessageDialog(this, "Xuất kho cấp cho Bếp thành công!");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showExportWasteDialog() {
        if (itemsList == null || itemsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kho chưa có mặt hàng nào.");
            return;
        }
        JComboBox<InventoryItem> cbItem = new JComboBox<>(itemsList.toArray(new InventoryItem[0]));
        JTextField txtQty = new JTextField("1");
        JTextField txtReason = new JTextField("Hỏng / ôi thiu / hết hạn sử dụng");

        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Mặt hàng xuất hủy:")); p.add(cbItem);
        p.add(new JLabel("Số lượng hủy:")); p.add(txtQty);
        p.add(new JLabel("Lý do xuất hủy:")); p.add(txtReason);

        int res = JOptionPane.showConfirmDialog(this, p, "Biên Bản Xuất Hủy Hàng Hóa (Waste)", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = (InventoryItem) cbItem.getSelectedItem();
                BigDecimal qty = new BigDecimal(txtQty.getText().trim());
                inventoryController.exportWaste(item.getId(), qty, currentUser, txtReason.getText().trim());
                JOptionPane.showMessageDialog(this, "Đã ghi nhận xuất hủy hàng hóa!");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showReturnSupplierDialog() {
        if (itemsList == null || itemsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kho chưa có mặt hàng nào.");
            return;
        }
        JComboBox<InventoryItem> cbItem = new JComboBox<>(itemsList.toArray(new InventoryItem[0]));
        List<Supplier> sups = inventoryController.getActiveSuppliers();
        JComboBox<Supplier> cbSup = new JComboBox<>(sups.toArray(new Supplier[0]));
        JTextField txtQty = new JTextField("2");
        JTextField txtPrice = new JTextField("0");
        JTextField txtReason = new JTextField("Hàng không đúng quy cách / trả lại NCC");

        cbItem.addActionListener(e -> {
            InventoryItem it = (InventoryItem) cbItem.getSelectedItem();
            if (it != null) txtPrice.setText(it.getCostPrice().toPlainString());
        });
        if (cbItem.getItemCount() > 0) {
            InventoryItem first = cbItem.getItemAt(0);
            txtPrice.setText(first.getCostPrice().toPlainString());
        }

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Chọn mặt hàng hoàn trả:")); p.add(cbItem);
        p.add(new JLabel("Trả cho Nhà cung cấp:")); p.add(cbSup);
        p.add(new JLabel("Số lượng hoàn trả:")); p.add(txtQty);
        p.add(new JLabel("Đơn giá hoàn tiền (VND):")); p.add(txtPrice);
        p.add(new JLabel("Lý do hoàn trả:")); p.add(txtReason);

        int res = JOptionPane.showConfirmDialog(this, p, "Phiếu Trả Hàng Cho Nhà Cung Cấp", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                InventoryItem it = (InventoryItem) cbItem.getSelectedItem();
                Supplier sp = (Supplier) cbSup.getSelectedItem();
                BigDecimal qty = new BigDecimal(txtQty.getText().trim());
                BigDecimal price = new BigDecimal(txtPrice.getText().trim());

                inventoryController.returnStockToSupplier(it.getId(), sp != null ? sp.getId() : null, qty, price, currentUser, txtReason.getText().trim());
                JOptionPane.showMessageDialog(this, "Trả hàng nhà cung cấp thành công!");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // DIALOG: KIỂM KÊ KHO CÂN BẰNG TỒN (STOCKTAKE)
    // =========================================================================
    private void showStocktakeDialog() {
        List<InventoryItem> items = inventoryController.getActiveItems();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kho chưa có mặt hàng nào để kiểm kê.");
            return;
        }

        String[] cols = {"ID", "Tên Mặt Hàng", "ĐVT", "Tồn Sổ Sách", "Số Lượng Đếm Thực Tế", "Chênh Lệch (+/-)", "Lý Do Hao Hụt / Chênh Lệch"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4 || c == 6; }
        };

        for (InventoryItem i : items) {
            model.addRow(new Object[]{i.getId(), i.getName(), i.getUnit(), i.getStockQty(), i.getStockQty(), "0.00", ""});
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
        TableUtils.applySmartSorting(table);

        // Lắng nghe thay đổi cột "Số Lượng Đếm Thực Tế" để tự động tính ngay cột "Chênh Lệch"
        model.addTableModelListener(e -> {
            if (e.getColumn() == 4) {
                int row = e.getFirstRow();
                try {
                    BigDecimal sysQty = new BigDecimal(model.getValueAt(row, 3).toString().trim());
                    BigDecimal actQty = new BigDecimal(model.getValueAt(row, 4).toString().trim());
                    BigDecimal diff = actQty.subtract(sysQty);
                    String diffStr = (diff.signum() > 0 ? "+" : "") + diff.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
                    model.setValueAt(diffStr, row, 5);
                } catch (Exception ignored) {}
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(850, 460));
        panel.add(new JLabel("<html><b style='color:#7e22ce;font-size:13px;'>BIÊN BẢN KIỂM KÊ ĐỐI SOÁT & CÂN BẰNG TỒN KHO THỰC TẾ</b><br>" +
            "<span style='color:#475569;font-size:11px;'>1. Nhập số lượng kiểm đếm thực tế vào cột <b>'Số Lượng Đếm Thực Tế'</b>.<br>" +
            "2. Hệ thống tự động tính <b>Chênh lệch (Hao hụt/Dôi dư)</b>.<br>" +
            "3. Khi bấm 'Xác nhận', tồn kho phần mềm sẽ được cân bằng chính xác theo số thực tế của bạn.</span></html>"), BorderLayout.NORTH);
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
                    String reason = model.getValueAt(r, 6) != null ? model.getValueAt(r, 6).toString().trim() : "";

                    InventoryAuditDetail d = new InventoryAuditDetail();
                    d.setItemId(itemId);
                    d.setSystemQty(sysQty);
                    d.setActualQty(actQty);
                    d.setDifferenceQty(actQty.subtract(sysQty));
                    d.setReason(reason);
                    details.add(d);
                }

                inventoryController.performStocktake(currentUser, "Kiểm kê định kỳ bởi " + currentUser.getFullName(), details);
                JOptionPane.showMessageDialog(this, "Đợt kiểm kê đã được lưu thành công! Tồn kho đã được cân bằng chính xác theo số đếm thực tế.");
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi kiểm kê: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAuditDetailsDialog() {
        int viewRow = auditTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đợt kiểm kho cần xem chi tiết.");
            return;
        }
        int r = auditTable.convertRowIndexToModel(viewRow);
        int auditId = (int) auditModel.getValueAt(r, 0);
        String auditCode = (String) auditModel.getValueAt(r, 1);
        String auditor = (String) auditModel.getValueAt(r, 2);
        String date = (String) auditModel.getValueAt(r, 3);
        String note = (String) auditModel.getValueAt(r, 5);

        List<InventoryAuditDetail> details = inventoryController.getAuditDetails(auditId);

        String[] cols = {"Hàng Hóa", "ĐVT", "Tồn Sổ Sách Lúc Kiểm", "Đếm Thực Tế", "Chênh Lệch", "Lý Do Hao Hụt / Chênh Lệch"};
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
        TableUtils.applySmartSorting(table);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setPreferredSize(new Dimension(750, 420));
        p.add(new JLabel("<html><b>CHI TIẾT ĐỢT KIỂM KÊ: " + auditCode + "</b><br>" +
            "Người kiểm: <b>" + auditor + "</b> | Ngày kiểm: <b>" + date + "</b> | Ghi chú: <i>" + note + "</i></html>"), BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, p, "Chi Tiết Đợt Kiểm Kho #" + auditId, JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================================
    // IN / XEM CHỨNG TỪ GIẤY TỜ NHẬP XUẤT
    // =========================================================================
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
            case EXPORT -> "PHIẾU XUẤT KHO CẤP CHO BẾP";
            case WASTE -> "BIÊN BẢN XUẤT HỦY HÀNG HÓA";
            case RETURN -> "PHIẾU TRẢ HÀNG NHÀ CUNG CẤP";
            default -> "BIÊN BẢN ĐIỀU CHỈNH / CÂN BẰNG TỒN KHO";
        };
        String prefix = sm.getType() == MovementType.IMPORT ? "PNK-" : "PXK-";
        String slipCode = prefix + String.format("%05d", sm.getId());
        BigDecimal unitPrice = sm.getUnitPrice() != null ? sm.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalVal = sm.getQuantity().abs().multiply(unitPrice);

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
            String.format("  %-4d %-25s %-10s %-12s %-14s\n", 1, sm.getItemName(), sm.getQuantity().abs(),
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
        JButton btnPrint = createActionBtn("🖨️ In Chứng Từ (Máy in)", new Color(16, 185, 129));
        JButton btnCopy = createActionBtn("📋 Sao chép Text", new Color(59, 130, 246));
        JButton btnClose = createActionBtn("Đóng", new Color(100, 116, 139));

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

    private void printCurrentStockReport() {
        if (itemsList == null || itemsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kho hiện không có dữ liệu để in.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("                           BÁO CÁO TỔNG HỢP TỒN KHO HÀNG HÓA                            \n");
        sb.append("                     Thời điểm xuất báo cáo: ").append(DateUtils.format(java.time.LocalDateTime.now())).append("\n");
        sb.append("                     Người lập: ").append(currentUser.getFullName()).append("\n");
        sb.append("----------------------------------------------------------------------------------------\n");
        sb.append(String.format(" %-4s %-24s %-14s %-8s %-12s %-12s %-14s\n", "ID", "Tên Mặt Hàng", "Danh Mục", "ĐVT", "Tồn Kho", "Định Mức", "Giá Vốn"));
        sb.append("----------------------------------------------------------------------------------------\n");

        BigDecimal totalVal = BigDecimal.ZERO;
        for (InventoryItem i : itemsList) {
            BigDecimal val = i.getStockQty().multiply(i.getCostPrice() != null ? i.getCostPrice() : BigDecimal.ZERO);
            totalVal = totalVal.add(val);
            sb.append(String.format(" %-4d %-24s %-14s %-8s %-12s %-12s %-14s\n",
                i.getId(),
                i.getName(),
                i.getCategory() != null ? i.getCategory() : "-",
                i.getUnit(),
                i.getStockQty(),
                i.getMinQty(),
                CurrencyFormat.format(i.getCostPrice())
            ));
        }
        sb.append("----------------------------------------------------------------------------------------\n");
        sb.append(" TỔNG GIÁ TRỊ TOÀN BỘ KHO: ").append(CurrencyFormat.format(totalVal)).append("\n");
        sb.append("========================================================================================\n");

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Báo Cáo Tồn Kho", true);
        dlg.setSize(750, 550);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton pBtn = createActionBtn("🖨️ In Báo Cáo", new Color(16, 185, 129));
        JButton cBtn = createActionBtn("Đóng", new Color(100, 116, 139));
        pBtn.addActionListener(e -> {
            try { area.print(); } catch (Exception ex) { JOptionPane.showMessageDialog(dlg, "Lỗi in: " + ex.getMessage()); }
        });
        cBtn.addActionListener(e -> dlg.dispose());
        bar.add(pBtn); bar.add(cBtn);

        dlg.add(new JScrollPane(area), BorderLayout.CENTER);
        dlg.add(bar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // =========================================================================
    // SUPPLIER CRUD DIALOGS
    // =========================================================================
    private void showAddSupplierDialog() {
        JTextField txtName = new JTextField();
        JTextField txtContact = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtAddr = new JTextField();

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Tên Nhà Cung Cấp:")); p.add(txtName);
        p.add(new JLabel("Người liên hệ:")); p.add(txtContact);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(new JLabel("Địa chỉ:")); p.add(txtAddr);

        int res = JOptionPane.showConfirmDialog(this, p, "Thêm Nhà Cung Cấp mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            String addr = txtAddr.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên nhà cung cấp không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (addr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Địa chỉ nhà cung cấp là bắt buộc, không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Supplier s = new Supplier();
            s.setName(name);
            s.setContactPerson(txtContact.getText().trim());
            s.setPhone(txtPhone.getText().trim());
            s.setEmail(txtEmail.getText().trim());
            s.setAddress(addr);
            s.setActive(true);
            inventoryController.saveSupplier(s);
            JOptionPane.showMessageDialog(this, "Đã thêm nhà cung cấp thành công!");
            loadSuppliersAsync();
        }
    }

    private void showEditSupplierDialog() {
        int viewRow = supplierTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 nhà cung cấp cần sửa.");
            return;
        }
        int r = supplierTable.convertRowIndexToModel(viewRow);
        int id = (int) supplierModel.getValueAt(r, 0);
        Supplier s = suppliersList.stream().filter(sup -> sup.getId() == id).findFirst().orElse(null);
        if (s == null) return;

        JTextField txtName = new JTextField(s.getName());
        JTextField txtContact = new JTextField(s.getContactPerson() != null ? s.getContactPerson() : "");
        JTextField txtPhone = new JTextField(s.getPhone() != null ? s.getPhone() : "");
        JTextField txtEmail = new JTextField(s.getEmail() != null ? s.getEmail() : "");
        JTextField txtAddr = new JTextField(s.getAddress() != null ? s.getAddress() : "");

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Tên Nhà Cung Cấp:")); p.add(txtName);
        p.add(new JLabel("Người liên hệ:")); p.add(txtContact);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(new JLabel("Địa chỉ:")); p.add(txtAddr);

        int res = JOptionPane.showConfirmDialog(this, p, "Chỉnh sửa Nhà Cung Cấp #" + s.getId(), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            String addr = txtAddr.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên nhà cung cấp không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (addr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Địa chỉ nhà cung cấp là bắt buộc, không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            s.setName(name);
            s.setContactPerson(txtContact.getText().trim());
            s.setPhone(txtPhone.getText().trim());
            s.setEmail(txtEmail.getText().trim());
            s.setAddress(addr);
            inventoryController.saveSupplier(s);
            loadSuppliersAsync();
        }
    }

    private void deleteSelectedSupplier() {
        int viewRow = supplierTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 nhà cung cấp cần xóa.");
            return;
        }
        int r = supplierTable.convertRowIndexToModel(viewRow);
        int id = (int) supplierModel.getValueAt(r, 0);
        String name = (String) supplierModel.getValueAt(r, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa Nhà Cung Cấp '" + name + "'?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            inventoryController.deleteSupplier(id);
            JOptionPane.showMessageDialog(this, "Đã xóa nhà cung cấp thành công!");
            loadSuppliersAsync();
        }
    }
}
