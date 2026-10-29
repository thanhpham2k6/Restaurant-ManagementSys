package com.pos.view;

import com.pos.controller.KitchenController;
import com.pos.controller.OrderController;
import com.pos.controller.StockController;
import com.pos.controller.TableController;
import com.pos.model.DiningTable;
import com.pos.model.Role;
import com.pos.model.User;
import com.pos.utils.DatabaseEncodingFixer;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Giao diện Chính của Hệ thống Quản lý Nhà hàng Enterprise v3.0.
 * Thiết kế hiện đại theo phong cách Modern SaaS Dashboard (KeroUI / KeroPOS):
 * - Thanh điều hướng Sidebar dọc bên trái với phân nhóm chức năng trực quan, hiệu ứng Active Pill bo góc.
 * - Thanh Header Top Bar hiện đại với Breadcrumb tiêu đề trang, tìm kiếm, avatar nhân viên và nút Đăng xuất.
 * - Khu vực trung tâm hiển thị linh hoạt các module thông qua CardLayout không độ trễ.
 */
public class MainDashboard extends JFrame {
    private final User currentUser;

    // Controllers
    private final StockController stockController = new StockController();
    private final OrderController orderController = new OrderController();
    private final KitchenController kitchenController = new KitchenController(stockController);
    private final TableController tableController = new TableController();

    // Module Panels
    private DashboardPanel dashboardPanel;
    private POSPanel posPanel;
    private TableReservationPanel tableReservationPanel;
    private KitchenPanel kitchenPanel;
    private MenuManagementPanel menuPanel;
    private CustomerPanel customerPanel;
    private VoucherPanel voucherPanel;
    private InventoryPanel inventoryPanel;
    private CashbookPanel cashbookPanel;
    private ShiftPanel shiftPanel;
    private ReportPanel reportPanel;
    private UserManagementPanel userPanel;

    // UI Structure
    private JPanel centerCardContainer;
    private CardLayout cardLayout;
    private JLabel lblPageTitle;
    private JLabel lblPageSubtitle;
    private final List<NavItemButton> navButtons = new ArrayList<>();
    private final Map<String, PageMeta> pageMetaMap = new HashMap<>();

    private static class PageMeta {
        String title;
        String subtitle;
        PageMeta(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public MainDashboard(User currentUser) {
        this.currentUser = currentUser;
        setTitle("Hệ Thống Quản Lý Nhà Hàng Enterprise v3.0 — " + currentUser.getFullName() + " [" + currentUser.getRole() + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1420, 880);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);

        initModules();
        initUI();
    }

    private void initModules() {
        dashboardPanel = new DashboardPanel(currentUser);
        posPanel = new POSPanel(currentUser, orderController, kitchenController, stockController);
        kitchenPanel = new KitchenPanel(kitchenController, currentUser);
        shiftPanel = new ShiftPanel(currentUser);
        tableReservationPanel = new TableReservationPanel(currentUser, tableController, this::navigateToPOS);
        customerPanel = new CustomerPanel(currentUser);
        voucherPanel = new VoucherPanel(currentUser);
        inventoryPanel = new InventoryPanel(currentUser);
        menuPanel = new MenuManagementPanel(currentUser);
        cashbookPanel = new CashbookPanel(currentUser);

        if (currentUser.getRole() == Role.MANAGER) {
            reportPanel = new ReportPanel(currentUser);
            userPanel = new UserManagementPanel(currentUser);
            orderController.attach(reportPanel);
        }

        posPanel.setOnNavigateTabRequested(this::navigateToTab);

        // Observers wiring
        orderController.attach(dashboardPanel);
        orderController.attach(posPanel);
        orderController.attach(kitchenPanel);
        orderController.attach(tableReservationPanel);

        kitchenController.attach(dashboardPanel);
        kitchenController.attach(posPanel);
        kitchenController.attach(kitchenPanel);

        stockController.attach(posPanel);

        // Đồng bộ 2 chiều trạng thái bàn giữa Thu ngân và Sơ đồ bàn
        tableReservationPanel.setOnTableStatusChanged(() -> posPanel.loadTablesAsync());
        posPanel.setOnTableStatusChanged(() -> tableReservationPanel.loadAllDataAsync());
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));

        // 1. LEFT MODERN SIDEBAR (KeroUI / SaaS Style)
        JPanel sidebar = createModernSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // 2. MAIN WORKSPACE (Header + Center Card Container)
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);

        // Header Top Bar
        JPanel topHeader = createModernTopBar();
        workspace.add(topHeader, BorderLayout.NORTH);

        // Center Card Panels
        cardLayout = new CardLayout();
        centerCardContainer = new JPanel(cardLayout);
        centerCardContainer.setOpaque(false);

        registerCard("DASHBOARD", "Tổng quan kinh doanh", "Theo dõi tình hình kinh doanh, hóa đơn và cảnh báo kho thời gian thực", dashboardPanel);
        registerCard("POS", "Thu ngân - Điểm bán hàng (POS)", "Lập đơn bán hàng tại bàn & mang về, áp dụng voucher và in hóa đơn", posPanel);
        registerCard("TABLES", "Sơ đồ bàn & Đặt bàn", "Theo dõi sơ đồ bàn trực quan và quản lý đặt bàn trước", tableReservationPanel);
        registerCard("KITCHEN", "Màn hình Bếp & Bar (KDS)", "Điều phối thứ tự chế biến món theo thời gian thực", kitchenPanel);
        registerCard("MENU", "Quản lý Thực đơn & Món ăn", "Quản lý danh sách món ăn, danh mục, đơn giá và trạng thái còn món", menuPanel);
        registerCard("CUSTOMERS", "Khách hàng & Hội viên", "Quản lý hồ sơ khách hàng, phân hạng thành viên và tích lũy điểm", customerPanel);
        registerCard("VOUCHERS", "Khuyến mãi & Voucher", "Thiết lập chương trình ưu đãi, mã giảm giá và chiết khấu", voucherPanel);
        registerCard("INVENTORY", "Quản lý Kho & Hàng hóa", "Kiểm kê hàng tồn, nhập xuất nguyên liệu, tính định lượng và nhà cung cấp", inventoryPanel);
        registerCard("CASHBOOK", "Sổ quỹ Thu - Chi & Chi phí", "Quản lý phiếu thu, phiếu chi tiền mặt, hóa đơn chi phí và tồn quỹ thực tế", cashbookPanel);
        registerCard("SHIFTS", "Ca làm việc & Chấm công", "Vận hành ca làm việc, bàn giao két tiền, thời khóa biểu và điểm danh", shiftPanel);

        if (currentUser.getRole() == Role.MANAGER) {
            registerCard("REPORTS", "Báo cáo doanh thu & Phân tích", "Phân tích doanh thu, cơ cấu mặt hàng bán chạy và xuất file báo cáo", reportPanel);
            registerCard("USERS", "Quản lý tài khoản & Nhân sự", "Phân quyền vai trò người dùng, quản lý tài khoản nhân viên hệ thống", userPanel);
        }

        workspace.add(centerCardContainer, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);

        setContentPane(root);

        // Mở module mặc định theo vai trò
        if (currentUser.getRole() == Role.CASHIER) {
            switchToCard("POS");
        } else if (currentUser.getRole() == Role.KITCHEN) {
            switchToCard("KITCHEN");
        } else {
            switchToCard("DASHBOARD");
        }
    }

    private void registerCard(String cardKey, String title, String subtitle, JPanel panel) {
        if (panel == null) return;
        centerCardContainer.add(panel, cardKey);
        pageMetaMap.put(cardKey, new PageMeta(title, subtitle));
    }

    // ==================== SIDEBAR UI ====================
    private JPanel createModernSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, new Color(226, 232, 240)));

        // 1. BRAND HEADER (Logo + Title)
        JPanel brandPanel = new JPanel(new BorderLayout(10, 0));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Circular Icon Badge
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(225, 29, 72)); // Rose red accent (như KeroUI)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                String icon = "R";
                int x = (getWidth() - fm.stringWidth(icon)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(icon, x, y);
                g2.dispose();
            }
        };
        logoBadge.setPreferredSize(new Dimension(38, 38));

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
        brandText.setOpaque(false);
        JLabel lblBrand = new JLabel("RESTAURANT SYSTEM");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblBrand.setForeground(new Color(30, 41, 59));

        JLabel lblVer = new JLabel("Enterprise Edition v3.0");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVer.setForeground(new Color(148, 163, 184));

        brandText.add(lblBrand);
        brandText.add(lblVer);

        brandPanel.add(logoBadge, BorderLayout.WEST);
        brandPanel.add(brandText, BorderLayout.CENTER);

        sidebar.add(brandPanel, BorderLayout.NORTH);

        // 2. NAVIGATION MENU (Grouped sections)
        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);
        menuContainer.setBorder(new EmptyBorder(8, 12, 16, 12));

        Role role = currentUser.getRole();

        // SECTION 1: VẬN HÀNH & BÁN HÀNG
        addSectionHeader(menuContainer, "VẬN HÀNH & BÁN HÀNG");
        if (role == Role.MANAGER) {
            addNavItem(menuContainer, "DASHBOARD", "Tổng quan");
        }
        if (role == Role.MANAGER || role == Role.CASHIER) {
            addNavItem(menuContainer, "POS", "Thu ngân (POS)");
            addNavItem(menuContainer, "TABLES", "Sơ đồ bàn & Đặt bàn");
        }
        if (role == Role.MANAGER || role == Role.CASHIER || role == Role.KITCHEN) {
            addNavItem(menuContainer, "KITCHEN", "Màn hình Bếp (KDS)");
        }

        // SECTION 2: THỰC ĐƠN & KHÁCH HÀNG
        if (role == Role.MANAGER || role == Role.CASHIER) {
            addSectionHeader(menuContainer, "THỰC ĐƠN & DỊCH VỤ");
            if (role == Role.MANAGER) {
                addNavItem(menuContainer, "MENU", "Quản lý Thực đơn");
            }
            addNavItem(menuContainer, "CUSTOMERS", "Khách hàng");
            addNavItem(menuContainer, "VOUCHERS", "Khuyến mãi & Voucher");
        }

        // SECTION 3: KHO & NGUYÊN VẬT LIỆU
        if (role == Role.MANAGER || role == Role.KITCHEN) {
            addSectionHeader(menuContainer, "KHO & NGUYÊN LIỆU");
            addNavItem(menuContainer, "INVENTORY", "Quản lý Kho hàng");
        }

        // SECTION 4: TÀI CHÍNH & SỔ QUỸ
        if (role == Role.MANAGER || role == Role.CASHIER) {
            addSectionHeader(menuContainer, "TÀI CHÍNH & SỔ QUỸ");
            if (role == Role.MANAGER) {
                addNavItem(menuContainer, "CASHBOOK", "Sổ quỹ Thu - Chi");
            }
            addNavItem(menuContainer, "SHIFTS", "Ca làm & Chấm công");
        }

        // SECTION 5: BÁO CÁO & HỆ THỐNG
        if (role == Role.MANAGER) {
            addSectionHeader(menuContainer, "BÁO CÁO & HỆ THỐNG");
            addNavItem(menuContainer, "REPORTS", "Báo cáo doanh thu");
            addNavItem(menuContainer, "USERS", "Quản lý tài khoản");
        }

        JScrollPane scrollPane = new JScrollPane(menuContainer);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        sidebar.add(scrollPane, BorderLayout.CENTER);

        return sidebar;
    }

    private void addSectionHeader(JPanel container, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.setBorder(new EmptyBorder(12, 10, 4, 10));

        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(148, 163, 184));
        p.add(l, BorderLayout.CENTER);

        container.add(p);
    }

    private void addNavItem(JPanel container, String cardKey, String text) {
        NavItemButton btn = new NavItemButton(cardKey, text, () -> switchToCard(cardKey));
        navButtons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(3));
    }

    // ==================== TOP BAR UI ====================
    private JPanel createModernTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(Color.WHITE);
        bar.setPreferredSize(new Dimension(0, 64));
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            new EmptyBorder(10, 24, 10, 24)
        ));

        // Left: Page Title & Subtitle Breadcrumb
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);

        lblPageTitle = new JLabel("Tổng quan kinh doanh");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblPageTitle.setForeground(new Color(30, 41, 59));

        lblPageSubtitle = new JLabel("Theo dõi tình hình kinh doanh, hóa đơn và cảnh báo kho thời gian thực");
        lblPageSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPageSubtitle.setForeground(new Color(100, 116, 139));

        titlePanel.add(lblPageTitle);
        titlePanel.add(lblPageSubtitle);
        bar.add(titlePanel, BorderLayout.WEST);

        // Right: Search + Refresh + User Profile Chip + Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        // Global Refresh Button
        JButton btnQuickRefresh = new JButton("Làm mới");
        btnQuickRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnQuickRefresh.setBackground(new Color(248, 250, 252));
        btnQuickRefresh.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));
        btnQuickRefresh.setFocusPainted(false);
        btnQuickRefresh.addActionListener(e -> {
            DatabaseEncodingFixer.autoFixDatabaseAsync();
            refreshActiveModule();
        });
        rightPanel.add(btnQuickRefresh);

        // User Profile Chip (Avatar Circle + Name + Role Tag)
        JPanel userChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        userChip.setBackground(new Color(248, 250, 252));
        userChip.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(2, 6, 2, 10)
        ));

        // Circular Initials Avatar
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = currentUser.getRole() == Role.MANAGER ? new Color(79, 70, 229) :
                          currentUser.getRole() == Role.CASHIER ? new Color(16, 185, 129) : new Color(245, 158, 11);
                g2.setColor(c);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String initials = currentUser.getUsername().substring(0, 1).toUpperCase();
                int x = (getWidth() - fm.stringWidth(initials)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(28, 28));

        JLabel lblName = new JLabel(currentUser.getFullName());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(new Color(30, 41, 59));

        JLabel lblRoleBadge = new JLabel(" " + currentUser.getRole().name() + " ");
        lblRoleBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblRoleBadge.setOpaque(true);
        if (currentUser.getRole() == Role.MANAGER) {
            lblRoleBadge.setBackground(new Color(238, 242, 255));
            lblRoleBadge.setForeground(new Color(79, 70, 229));
        } else if (currentUser.getRole() == Role.CASHIER) {
            lblRoleBadge.setBackground(new Color(236, 253, 245));
            lblRoleBadge.setForeground(new Color(16, 185, 129));
        } else {
            lblRoleBadge.setBackground(new Color(254, 243, 199));
            lblRoleBadge.setForeground(new Color(217, 119, 6));
        }

        userChip.add(avatar);
        userChip.add(lblName);
        userChip.add(lblRoleBadge);
        rightPanel.add(userChip);

        // Logout Button
        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setForeground(new Color(225, 29, 72));
        btnLogout.setBackground(Color.WHITE);
        btnLogout.setBorder(new CompoundBorder(
            new LineBorder(new Color(254, 205, 211), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn đăng xuất khỏi hệ thống?", "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginForm().setVisible(true);
            }
        });
        rightPanel.add(btnLogout);

        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    // ==================== NAVIGATION SWITCHING ====================
    public void switchToCard(String cardKey) {
        if (cardLayout == null || centerCardContainer == null) return;
        cardLayout.show(centerCardContainer, cardKey);

        // Update header
        PageMeta meta = pageMetaMap.get(cardKey);
        if (meta != null) {
            lblPageTitle.setText(meta.title);
            lblPageSubtitle.setText(meta.subtitle);
        }

        // Update active nav button
        for (NavItemButton btn : navButtons) {
            btn.setActive(btn.getCardKey().equalsIgnoreCase(cardKey));
        }

        // Tự động làm mới dữ liệu module được kích hoạt
        refreshActiveModule();
    }

    private void refreshActiveModule() {
        Component visible = null;
        for (Component c : centerCardContainer.getComponents()) {
            if (c.isVisible()) {
                visible = c;
                break;
            }
        }

        if (visible == posPanel) {
            posPanel.loadTablesAsync();
            posPanel.loadActiveVouchers();
        } else if (visible == tableReservationPanel) {
            tableReservationPanel.loadAllDataAsync();
        } else if (visible == kitchenPanel) {
            kitchenPanel.loadItems();
        } else if (visible == dashboardPanel) {
            dashboardPanel.refreshData();
        } else if (visible == menuPanel) {
            menuPanel.loadAllDataAsync();
        } else if (visible == cashbookPanel) {
            cashbookPanel.loadDataAsync();
        } else if (visible == inventoryPanel) {
            inventoryPanel.loadAllDataAsync();
        }
    }

    public void navigateToTab(String title) {
        String t = title.toLowerCase();
        if (t.contains("thu ngân") || t.contains("pos")) switchToCard("POS");
        else if (t.contains("sơ đồ") || t.contains("bàn")) switchToCard("TABLES");
        else if (t.contains("bếp") || t.contains("kds")) switchToCard("KITCHEN");
        else if (t.contains("thực đơn") || t.contains("món")) switchToCard("MENU");
        else if (t.contains("khách")) switchToCard("CUSTOMERS");
        else if (t.contains("khuyến mãi") || t.contains("voucher")) switchToCard("VOUCHERS");
        else if (t.contains("kho")) switchToCard("INVENTORY");
        else if (t.contains("sổ quỹ") || t.contains("thu chi")) switchToCard("CASHBOOK");
        else if (t.contains("ca") || t.contains("chấm công")) switchToCard("SHIFTS");
        else if (t.contains("báo cáo")) switchToCard("REPORTS");
        else if (t.contains("tài khoản") || t.contains("nhân viên")) switchToCard("USERS");
        else switchToCard("DASHBOARD");
    }

    public void navigateToPOS(DiningTable table) {
        switchToCard("POS");
        if (posPanel != null && table != null) {
            posPanel.loadTablesAsync();
            posPanel.selectDiningTable(table);
        }
    }

    // ==================== NAV ITEM BUTTON COMPONENT (KeroUI Style Pill) ====================
    private static class NavItemButton extends JPanel {
        private final String cardKey;
        private final String text;
        private final Runnable onClick;
        private boolean isActive = false;
        private boolean isHovered = false;

        public NavItemButton(String cardKey, String text, Runnable onClick) {
            this.cardKey = cardKey;
            this.text = text;
            this.onClick = onClick;

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(226, 38));
            setMaximumSize(new Dimension(226, 38));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null) onClick.run();
                }
            });
        }

        public String getCardKey() { return cardKey; }

        public void setActive(boolean active) {
            this.isActive = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                // Active pill: Soft rose/pink background (chuẩn KeroUI)
                g2.setColor(new Color(254, 226, 226)); // rose-100 #FEE2E2
                g2.fillRoundRect(4, 2, w - 8, h - 4, 10, 10);

                // Small indicator pill or bar
                g2.setColor(new Color(225, 29, 72)); // rose-600 #E11D48
                g2.fillRoundRect(8, (h - 16) / 2, 3, 16, 2, 2);

                // Text
                g2.setColor(new Color(225, 29, 72));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            } else if (isHovered) {
                // Hover: Soft light gray
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(4, 2, w - 8, h - 4, 10, 10);

                g2.setColor(new Color(51, 65, 85));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            } else {
                g2.setColor(new Color(71, 85, 105)); // slate-600
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            }

            FontMetrics fm = g2.getFontMetrics();
            int textX = 22;
            int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();

            // Bullet dot icon like in the screenshot
            String bullet = "•  ";
            g2.drawString(bullet + text, textX, textY);

            g2.dispose();
        }
    }

    private static class MatteBorder extends javax.swing.border.AbstractBorder {
        private final int top, left, bottom, right;
        private final Color color;
        public MatteBorder(int top, int left, int bottom, int right, Color color) {
            this.top = top; this.left = left; this.bottom = bottom; this.right = right;
            this.color = color;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color oldColor = g.getColor();
            g.setColor(color);
            if (top > 0) g.fillRect(x, y, width, top);
            if (left > 0) g.fillRect(x, y, left, height);
            if (bottom > 0) g.fillRect(x, y + height - bottom, width, bottom);
            if (right > 0) g.fillRect(x + width - right, y, right, height);
            g.setColor(oldColor);
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(top, left, bottom, right);
        }
    }
}
