package com.pos.view;

import com.pos.controller.ReservationController;
import com.pos.controller.TableController;
import com.pos.model.*;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DateUtils;
import com.pos.utils.TableUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module Chuyên biệt: Sơ đồ bàn (Floor Plan) & Quản lý Đặt bàn trước (Reservations).
 * Hỗ trợ tiếp nhận đặt bàn đa kênh: Hotline, Zalo, Facebook/MXH, Trực tiếp tại quầy, Website.
 */
public class TableReservationPanel extends JPanel implements com.pos.controller.OrderObserver {
    private final User currentUser;
    private final TableController tableController;
    private final ReservationController reservationController;
    private Consumer<DiningTable> onTableSelectedForPOS;
    private Runnable onTableStatusChanged;

    private JTabbedPane tabbedPane;

    // Tab 1: Sơ đồ bàn
    private JPanel cardsGridPanel;
    private List<DiningTable> tableList = new ArrayList<>();
    private String currentTableFilter = "ALL";
    private JLabel lblStatTotal, lblStatAvail, lblStatOccupied, lblStatReserved;

    // Tab 2: Đặt bàn
    private JTable resTable;
    private DefaultTableModel resTableModel;
    private List<Reservation> resList = new ArrayList<>();
    private JComboBox<String> cbFilterChannel;
    private JComboBox<String> cbFilterStatus;
    private JComboBox<String> cbFilterDate;

    public TableReservationPanel(User currentUser, TableController tableController, Consumer<DiningTable> onTableSelectedForPOS) {
        this.currentUser = currentUser;
        this.tableController = tableController;
        this.reservationController = new ReservationController();
        this.onTableSelectedForPOS = onTableSelectedForPOS;

        setLayout(new BorderLayout());
        initComponents();
        loadAllDataAsync();
    }

    public void setOnTableSelectedForPOS(Consumer<DiningTable> onTableSelectedForPOS) {
        this.onTableSelectedForPOS = onTableSelectedForPOS;
    }

    public void setOnTableStatusChanged(Runnable onTableStatusChanged) {
        this.onTableStatusChanged = onTableStatusChanged;
    }

    @Override
    public void onOrderChanged(OrderEvent event) {
        SwingUtilities.invokeLater(this::loadAllDataAsync);
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        tabbedPane.addTab("Sơ đồ bàn (Floor Plan)", createFloorPlanTab());
        tabbedPane.addTab("Lịch Đặt Bàn (Reservations)", createReservationsTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // TAB 1: SƠ ĐỒ BÀN (FLOOR PLAN)
    // =========================================================================
    private JPanel createFloorPlanTab() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(245, 247, 250));

        // Top Banner & KPI Counters
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("SƠ ĐỒ BÀN ĂN THEO THỜI GIAN THỰC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));
        JLabel lblSub = new JLabel("Nhấp chọn bàn để Gọi món / Thu ngân hoặc chuyển đổi trạng thái phục vụ");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        // Stats Badges
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statsPanel.setOpaque(false);
        lblStatTotal = createBadge("Tổng: 0", new Color(71, 85, 105), Color.WHITE);
        lblStatAvail = createBadge("Trống: 0", new Color(34, 197, 94), Color.WHITE);
        lblStatOccupied = createBadge("Có khách: 0", new Color(239, 68, 68), Color.WHITE);
        lblStatReserved = createBadge("Đặt trước: 0", new Color(245, 158, 11), Color.WHITE);

        statsPanel.add(lblStatTotal);
        statsPanel.add(lblStatAvail);
        statsPanel.add(lblStatOccupied);
        statsPanel.add(lblStatReserved);

        topPanel.add(titlePanel, BorderLayout.WEST);
        topPanel.add(statsPanel, BorderLayout.EAST);

        // Filter Bar & Action Bar
        JPanel controlBar = new JPanel(new BorderLayout(10, 10));
        controlBar.setOpaque(false);

        JPanel filterBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBtns.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        JToggleButton tbAll = createFilterToggle("Tất cả bàn", true);
        JToggleButton tbAvail = createFilterToggle("Bàn trống", false);
        JToggleButton tbOccupied = createFilterToggle("Đang phục vụ", false);
        JToggleButton tbReserved = createFilterToggle("Đã đặt trước", false);

        bg.add(tbAll); bg.add(tbAvail); bg.add(tbOccupied); bg.add(tbReserved);
        filterBtns.add(tbAll); filterBtns.add(tbAvail); filterBtns.add(tbOccupied); filterBtns.add(tbReserved);

        tbAll.addActionListener(e -> { currentTableFilter = "ALL"; renderTableCards(); });
        tbAvail.addActionListener(e -> { currentTableFilter = "AVAILABLE"; renderTableCards(); });
        tbOccupied.addActionListener(e -> { currentTableFilter = "OCCUPIED"; renderTableCards(); });
        tbReserved.addActionListener(e -> { currentTableFilter = "RESERVED"; renderTableCards(); });

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        JButton btnAddTable = createStyledButton("+ Thêm bàn", new Color(16, 185, 129));
        JButton btnEditTable = createStyledButton("Sửa bàn", new Color(59, 130, 246));
        JButton btnDeleteTable = createStyledButton("Xóa bàn", new Color(239, 68, 68));
        JButton btnMoveTable = createStyledButton("Chuyển bàn", new Color(99, 102, 241));
        JButton btnMergeTable = createStyledButton("Gộp bàn", new Color(139, 92, 246));
        JButton btnRefresh = createStyledButton("Làm mới", new Color(100, 116, 139));

        btnAddTable.addActionListener(e -> showAddTableDialog());
        btnEditTable.addActionListener(e -> showEditTableDialog());
        btnDeleteTable.addActionListener(e -> showDeleteTableDialog());
        btnMoveTable.addActionListener(e -> showMoveTableDialog());
        btnMergeTable.addActionListener(e -> showMergeTableDialog());
        btnRefresh.addActionListener(e -> loadTablesAsync());

        if (currentUser.getRole() == Role.MANAGER) {
            actionBtns.add(btnAddTable);
            actionBtns.add(btnEditTable);
            actionBtns.add(btnDeleteTable);
        }
        actionBtns.add(btnMoveTable);
        actionBtns.add(btnMergeTable);
        actionBtns.add(btnRefresh);

        controlBar.add(filterBtns, BorderLayout.WEST);
        controlBar.add(actionBtns, BorderLayout.EAST);

        // Grid Container for Table Cards
        cardsGridPanel = new JPanel(new GridLayout(0, 4, 15, 15));
        cardsGridPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(cardsGridPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel headerWrapper = new JPanel(new BorderLayout(10, 12));
        headerWrapper.setOpaque(false);
        headerWrapper.add(topPanel, BorderLayout.NORTH);
        headerWrapper.add(controlBar, BorderLayout.SOUTH);

        root.add(headerWrapper, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);

        return root;
    }

    private void renderTableCards() {
        cardsGridPanel.removeAll();

        int total = tableList.size();
        int avail = 0, occ = 0, res = 0;

        for (DiningTable dt : tableList) {
            TableStatus st = dt.getStatus();
            if (st.isAvailable()) avail++;
            else if (st == TableStatus.OCCUPIED) occ++;
            else if (st == TableStatus.RESERVED) res++;

            // Filter check
            if (!currentTableFilter.equals("ALL")) {
                if (currentTableFilter.equals("AVAILABLE") && !st.isAvailable()) continue;
                if (currentTableFilter.equals("OCCUPIED") && st != TableStatus.OCCUPIED) continue;
                if (currentTableFilter.equals("RESERVED") && st != TableStatus.RESERVED) continue;
            }

            cardsGridPanel.add(buildTableCard(dt));
        }

        lblStatTotal.setText("Tổng: " + total);
        lblStatAvail.setText("Trống: " + avail);
        lblStatOccupied.setText("Có khách: " + occ);
        lblStatReserved.setText("Đặt trước: " + res);

        cardsGridPanel.revalidate();
        cardsGridPanel.repaint();
    }

    private JPanel buildTableCard(DiningTable dt) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setPreferredSize(new Dimension(240, 180));
        card.setBackground(Color.WHITE);

        Color borderColor;
        Color badgeBg;
        Color badgeFg = Color.WHITE;
        String statusText;

        TableStatus st = dt.getStatus();
        if (st == TableStatus.OCCUPIED) {
            borderColor = new Color(254, 202, 202);
            badgeBg = new Color(239, 68, 68);
            statusText = "● Đang phục vụ";
        } else if (st == TableStatus.RESERVED) {
            borderColor = new Color(254, 240, 138);
            badgeBg = new Color(245, 158, 11);
            statusText = "● Đã đặt trước";
        } else {
            borderColor = new Color(187, 247, 208);
            badgeBg = new Color(34, 197, 94);
            statusText = "● Trống - Sẵn sàng";
        }

        card.setBorder(new CompoundBorder(
            new LineBorder(borderColor, 2, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        // Header: Table Name & Capacity
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel lblName = new JLabel(dt.getName());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblName.setForeground(new Color(15, 23, 42));

        JLabel lblCap = new JLabel(dt.getCapacity() + " cho");
        lblCap.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCap.setForeground(new Color(100, 116, 139));

        cardHeader.add(lblName, BorderLayout.WEST);
        cardHeader.add(lblCap, BorderLayout.EAST);

        // Status Badge in Center
        JPanel cardCenter = new JPanel(new BorderLayout(4, 4));
        cardCenter.setOpaque(false);

        JLabel lblBadge = new JLabel(statusText, SwingConstants.CENTER);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        lblBadge.setForeground(badgeFg);
        lblBadge.setBorder(new EmptyBorder(6, 10, 6, 10));

        cardCenter.add(lblBadge, BorderLayout.CENTER);

        // Actions: [Gọi món / POS] & [Đổi trạng thái]
        JPanel cardActions = new JPanel(new GridLayout(1, 2, 6, 0));
        cardActions.setOpaque(false);

        JButton btnPOS = new JButton("Gọi món / POS");
        btnPOS.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPOS.setBackground(new Color(15, 118, 110));
        btnPOS.setForeground(Color.WHITE);
        btnPOS.setFocusPainted(false);
        btnPOS.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPOS.addActionListener(e -> {
            if (onTableSelectedForPOS != null) {
                onTableSelectedForPOS.accept(dt);
            }
        });

        JButton btnChangeStatus = new JButton("Đổi trạng thái");
        btnChangeStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnChangeStatus.setBackground(new Color(241, 245, 249));
        btnChangeStatus.setForeground(new Color(51, 65, 85));
        btnChangeStatus.setFocusPainted(false);
        btnChangeStatus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChangeStatus.addActionListener(e -> showChangeTableStatusDialog(dt));

        cardActions.add(btnPOS);
        cardActions.add(btnChangeStatus);

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(cardCenter, BorderLayout.CENTER);
        card.add(cardActions, BorderLayout.SOUTH);

        return card;
    }

    // =========================================================================
    // TAB 2: QUẢN LÝ ĐẶT BÀN (RESERVATIONS)
    // =========================================================================
    private JPanel createReservationsTab() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(245, 247, 250));

        // Top Banner
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("TIẾP NHẬN & QUẢN LÝ ĐẶT BÀN TRƯỚC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));
        JLabel lblSub = new JLabel("Tiếp nhận khách qua Hotline, Zalo, Facebook/MXH, Trực tiếp tại quầy và Website");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnNewRes = createStyledButton("+ Tiếp nhận Đặt bàn", new Color(16, 185, 129));
        JButton btnConfirm = createStyledButton("Xác nhận đặt", new Color(59, 130, 246));
        JButton btnSeat = createStyledButton("Nhận bàn (Check-in)", new Color(245, 158, 11));
        JButton btnCancel = createStyledButton("Hủy đặt bàn", new Color(239, 68, 68));
        JButton btnPrintRes = createStyledButton("In phiếu cọc/đặt", new Color(139, 92, 246));
        JButton btnRefresh = createStyledButton("Làm mới", new Color(100, 116, 139));

        btnNewRes.addActionListener(e -> showNewReservationDialog());
        btnConfirm.addActionListener(e -> confirmSelectedReservation());
        btnSeat.addActionListener(e -> seatSelectedReservation());
        btnCancel.addActionListener(e -> cancelSelectedReservation());
        btnPrintRes.addActionListener(e -> printSelectedReservation());
        btnRefresh.addActionListener(e -> loadReservationsAsync());

        btnPanel.add(btnNewRes);
        btnPanel.add(btnConfirm);
        btnPanel.add(btnSeat);
        btnPanel.add(btnCancel);
        btnPanel.add(btnPrintRes);
        btnPanel.add(btnRefresh);

        topPanel.add(titlePanel, BorderLayout.WEST);
        topPanel.add(btnPanel, BorderLayout.EAST);

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        filterBar.setOpaque(false);
        filterBar.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        filterBar.setBackground(Color.WHITE);

        filterBar.add(new JLabel("Lọc theo Kênh:"));
        cbFilterChannel = new JComboBox<>(new String[]{"Tất cả kênh", "Hotline", "Zalo", "Facebook / MXH", "Trực tiếp", "Website"});
        cbFilterChannel.addActionListener(e -> filterReservationsTable());
        filterBar.add(cbFilterChannel);

        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(new JLabel("Trạng thái:"));
        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả trạng thái", "Chờ xác nhận (PENDING)", "Đã xác nhận (CONFIRMED)", "Đã nhận bàn (SEATED)", "Đã hủy (CANCELLED)"});
        cbFilterStatus.addActionListener(e -> filterReservationsTable());
        filterBar.add(cbFilterStatus);

        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(new JLabel("Thời gian:"));
        cbFilterDate = new JComboBox<>(new String[]{"Tất cả", "Hôm nay", "Sắp tới"});
        cbFilterDate.addActionListener(e -> filterReservationsTable());
        filterBar.add(cbFilterDate);

        // Table of Reservations
        String[] columns = {"ID", "Kênh tiếp nhận", "Tên khách", "Số điện thoại", "Bàn hẹn", "Giờ đến", "Số khách", "Tiền cọc", "Trạng thái", "Ghi chú"};
        resTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resTable = new JTable(resTableModel);
        resTable.setRowHeight(34);
        resTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableUtils.applySmartSorting(resTable);

        // Alignment & Colors
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        resTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        resTable.getColumnModel().getColumn(0).setCellRenderer(center);
        resTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        resTable.getColumnModel().getColumn(1).setCellRenderer(center);
        resTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        resTable.getColumnModel().getColumn(4).setCellRenderer(center);
        resTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        resTable.getColumnModel().getColumn(6).setCellRenderer(center);
        resTable.getColumnModel().getColumn(8).setPreferredWidth(110);
        resTable.getColumnModel().getColumn(8).setCellRenderer(center);

        JScrollPane scrollTable = new JScrollPane(resTable);
        scrollTable.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));

        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.setOpaque(false);
        topContainer.add(topPanel, BorderLayout.NORTH);
        topContainer.add(filterBar, BorderLayout.SOUTH);

        root.add(topContainer, BorderLayout.NORTH);
        root.add(scrollTable, BorderLayout.CENTER);

        return root;
    }

    // =========================================================================
    // ASYNC DATA LOADERS
    // =========================================================================
    public void loadAllDataAsync() {
        loadTablesAsync();
        loadReservationsAsync();
    }

    public void loadTablesAsync() {
        new SwingWorker<List<DiningTable>, Void>() {
            @Override protected List<DiningTable> doInBackground() {
                return tableController.getAllTables();
            }
            @Override protected void done() {
                try {
                    tableList = get();
                    renderTableCards();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void loadReservationsAsync() {
        new SwingWorker<List<Reservation>, Void>() {
            @Override protected List<Reservation> doInBackground() {
                return reservationController.getAllReservations();
            }
            @Override protected void done() {
                try {
                    resList = get();
                    filterReservationsTable();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterReservationsTable() {
        resTableModel.setRowCount(0);
        String channelFilter = (String) cbFilterChannel.getSelectedItem();
        String statusFilter = (String) cbFilterStatus.getSelectedItem();
        String dateFilter = (String) cbFilterDate.getSelectedItem();

        LocalDate today = LocalDate.now();

        for (Reservation r : resList) {
            // Channel filter
            if (channelFilter != null && !channelFilter.equals("Tất cả kênh")) {
                if (!r.getChannelDisplayName().toLowerCase().contains(channelFilter.toLowerCase())) {
                    continue;
                }
            }

            // Status filter
            if (statusFilter != null && !statusFilter.equals("Tất cả trạng thái")) {
                if (!statusFilter.contains(r.getStatus().name())) continue;
            }

            // Date filter
            if (dateFilter != null && !dateFilter.equals("Tất cả") && r.getReservationTime() != null) {
                LocalDate rDate = r.getReservationTime().toLocalDate();
                if (dateFilter.equals("Hôm nay") && !rDate.isEqual(today)) continue;
                if (dateFilter.equals("Sắp tới") && rDate.isBefore(today)) continue;
            }

            resTableModel.addRow(new Object[]{
                r.getId(),
                r.getChannelDisplayName(),
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getTableName() != null ? r.getTableName() : "Bàn #" + r.getTableId(),
                r.getReservationTime() != null ? DateUtils.format(r.getReservationTime()) : "-",
                r.getGuestCount() + " người",
                CurrencyFormat.format(r.getDepositAmount()),
                r.getStatus().getDisplayName(),
                r.getNote() != null ? r.getNote() : ""
            });
        }
    }

    // =========================================================================
    // DIALOGS & ACTION HANDLERS
    // =========================================================================
    private void showNewReservationDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tiếp nhận Đặt bàn Mới", true);
        dlg.setSize(520, 560);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 25, 15, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField();
        JTextField txtPhone = new JTextField();

        JComboBox<DiningTable> cbTables = new JComboBox<>();
        for (DiningTable t : tableList) {
            cbTables.addItem(t);
        }

        // Date Spinner & Time Spinner
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE);
        JSpinner spinTime = new JSpinner(dateModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spinTime, "yyyy-MM-dd HH:mm");
        spinTime.setEditor(timeEditor);

        JSpinner spinGuests = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        JTextField txtDeposit = new JTextField("0");

        // Channel Combo
        JComboBox<String> cbChannel = new JComboBox<>(new String[]{
            "HOTLINE (Điện thoại Tổng đài)",
            "ZALO (Tiếp nhận qua Zalo)",
            "FACEBOOK (Facebook Fanpage / MXH)",
            "DIRECT (Khách đến đặt trực tiếp)",
            "WEBSITE (Đặt chỗ qua Website)"
        });

        JTextField txtNote = new JTextField();

        int row = 0;
        addFormRow(form, gbc, row++, "Tên khách hàng (*):", txtName);
        addFormRow(form, gbc, row++, "Số điện thoại (*):", txtPhone);
        addFormRow(form, gbc, row++, "Kênh đặt bàn:", cbChannel);
        addFormRow(form, gbc, row++, "Chọn bàn ăn:", cbTables);
        addFormRow(form, gbc, row++, "Thời gian đến (*):", spinTime);
        addFormRow(form, gbc, row++, "Số lượng khách:", spinGuests);
        addFormRow(form, gbc, row++, "Tiền đặt cọc (VNĐ):", txtDeposit);
        addFormRow(form, gbc, row++, "Ghi chú đặc biệt:", txtNote);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnSave = createStyledButton("Lưu Lịch Đặt Bàn", new Color(16, 185, 129));
        JButton btnClose = createStyledButton("Hủy", new Color(148, 163, 184));

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập Tên và Số điện thoại khách hàng!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }

            DiningTable selTable = (DiningTable) cbTables.getSelectedItem();
            if (selTable == null) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn bàn ăn!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date d = (Date) spinTime.getValue();
            LocalDateTime ldt = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            int guests = (int) spinGuests.getValue();

            BigDecimal deposit = BigDecimal.ZERO;
            try {
                deposit = new BigDecimal(txtDeposit.getText().trim().replace(",", "").replace(".", ""));
            } catch (Exception ignored) {}

            String selectedChannelRaw = (String) cbChannel.getSelectedItem();
            String channelCode = "DIRECT";
            if (selectedChannelRaw != null) {
                if (selectedChannelRaw.startsWith("HOTLINE")) channelCode = "HOTLINE";
                else if (selectedChannelRaw.startsWith("ZALO")) channelCode = "ZALO";
                else if (selectedChannelRaw.startsWith("FACEBOOK")) channelCode = "FACEBOOK";
                else if (selectedChannelRaw.startsWith("WEBSITE")) channelCode = "WEBSITE";
            }

            try {
                reservationController.createReservation(selTable.getId(), null, name, phone, ldt, guests, deposit, channelCode, txtNote.getText().trim());
                JOptionPane.showMessageDialog(dlg, "Đã tiếp nhận lịch đặt bàn thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
                loadAllDataAsync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi đặt bàn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnClose.addActionListener(e -> dlg.dispose());

        btnBar.add(btnSave);
        btnBar.add(btnClose);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void addFormRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        p.add(comp, gbc);
    }

    private void confirmSelectedReservation() {
        int viewRow = resTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một lịch đặt bàn trên bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = resTable.convertRowIndexToModel(viewRow);
        int id = (int) resTableModel.getValueAt(row, 0);
        reservationController.confirmReservation(id);
        JOptionPane.showMessageDialog(this, "Đã xác nhận đặt bàn thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        loadAllDataAsync();
    }

    private void seatSelectedReservation() {
        int viewRow = resTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch đặt bàn để nhận khách vào bàn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = resTable.convertRowIndexToModel(viewRow);
        int id = (int) resTableModel.getValueAt(row, 0);
        Reservation target = resList.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
        if (target == null) return;

        reservationController.seatGuest(id, target.getTableId());

        int opt = JOptionPane.showConfirmDialog(this,
            "Khách " + target.getCustomerName() + " đã nhận bàn " + target.getTableName() + ".\nBạn có muốn chuyển sang màn hình Thu ngân để gọi món ngay không?",
            "Nhận bàn thành công", JOptionPane.YES_NO_OPTION);

        loadAllDataAsync();
        if (onTableStatusChanged != null) onTableStatusChanged.run();

        if (opt == JOptionPane.YES_OPTION && onTableSelectedForPOS != null) {
            DiningTable dt = tableList.stream().filter(t -> t.getId() == target.getTableId()).findFirst().orElse(null);
            if (dt != null) onTableSelectedForPOS.accept(dt);
        }
    }

    private void cancelSelectedReservation() {
        int viewRow = resTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch đặt bàn cần hủy!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = resTable.convertRowIndexToModel(viewRow);
        int id = (int) resTableModel.getValueAt(row, 0);
        Reservation target = resList.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
        if (target == null) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận hủy đặt bàn của khách: " + target.getCustomerName() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            reservationController.cancelReservation(id, target.getTableId());
            JOptionPane.showMessageDialog(this, "Đã hủy lịch đặt bàn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            loadAllDataAsync();
        }
    }

    private void printSelectedReservation() {
        int viewRow = resTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch đặt bàn cần in phiếu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = resTable.convertRowIndexToModel(viewRow);
        int id = (int) resTableModel.getValueAt(row, 0);
        Reservation r = resList.stream().filter(res -> res.getId() == id).findFirst().orElse(null);
        if (r == null) return;

        String slip =
            "=========================================\n" +
            "       PHIẾU TIẾP NHẬN ĐẶT BÀN\n" +
            "=========================================\n" +
            "Mã phiếu      : RES-" + r.getId() + "\n" +
            "Kênh tiếp nhận: " + r.getChannelDisplayName() + "\n" +
            "Khách hàng    : " + r.getCustomerName() + "\n" +
            "Số điện thoại : " + r.getCustomerPhone() + "\n" +
            "Bàn ăn chỉ định: " + (r.getTableName() != null ? r.getTableName() : "Bàn #" + r.getTableId()) + "\n" +
            "Thời gian đến : " + DateUtils.format(r.getReservationTime()) + "\n" +
            "Số lượng khách: " + r.getGuestCount() + " người\n" +
            "Tiền đặt cọc  : " + CurrencyFormat.format(r.getDepositAmount()) + "\n" +
            "Trạng thái    : " + r.getStatus().getDisplayName() + "\n" +
            "Ghi chú       : " + (r.getNote() != null ? r.getNote() : "-") + "\n" +
            "-----------------------------------------\n" +
            "Nhân viên tiếp nhận: " + currentUser.getFullName() + "\n" +
            "Quý khách vui lòng đến trước 15 phút.\n" +
            "=========================================\n";

        JTextArea area = new JTextArea(slip);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);

        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Phiếu Đặt Bàn", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showChangeTableStatusDialog(DiningTable dt) {
        String[] options = {"● Bàn Trống (AVAILABLE)", "● Đang Phục Vụ (OCCUPIED)", "● Đã Đặt Trước (RESERVED)"};
        int sel = JOptionPane.showOptionDialog(this,
            "Chọn trạng thái mới cho bàn " + dt.getName() + ":",
            "Cập nhật trạng thái bàn",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (sel == 0) tableController.updateTableStatus(dt.getId(), TableStatus.AVAILABLE);
        else if (sel == 1) tableController.updateTableStatus(dt.getId(), TableStatus.OCCUPIED);
        else if (sel == 2) tableController.updateTableStatus(dt.getId(), TableStatus.RESERVED);

        loadTablesAsync();
        if (onTableStatusChanged != null) onTableStatusChanged.run();
    }

    private void showAddTableDialog() {
        JTextField txtName = new JTextField();
        JSpinner spinCap = new JSpinner(new SpinnerNumberModel(4, 1, 30, 1));

        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Tên bàn:"));
        p.add(txtName);
        p.add(new JLabel("Sức chứa (người):"));
        p.add(spinCap);

        int opt = JOptionPane.showConfirmDialog(this, p, "Thêm Bàn Ăn Mới", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên bàn!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                tableController.addTable(name, (int) spinCap.getValue());
                JOptionPane.showMessageDialog(this, "Đã thêm bàn '" + name + "' thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                loadTablesAsync();
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi thêm bàn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditTableDialog() {
        if (tableList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có bàn nào trong hệ thống!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<DiningTable> cbTables = new JComboBox<>(tableList.toArray(new DiningTable[0]));
        JTextField txtName = new JTextField();
        JSpinner spinCap = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));

        Runnable updateFields = () -> {
            DiningTable sel = (DiningTable) cbTables.getSelectedItem();
            if (sel != null) {
                txtName.setText(sel.getName());
                spinCap.setValue(sel.getCapacity());
            }
        };
        updateFields.run();
        cbTables.addActionListener(e -> updateFields.run());

        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Chọn bàn cần sửa:"));
        p.add(cbTables);
        p.add(new JLabel("Tên bàn mới:"));
        p.add(txtName);
        p.add(new JLabel("Sức chứa (người):"));
        p.add(spinCap);

        int opt = JOptionPane.showConfirmDialog(this, p, "Chỉnh Sửa Thông Tin Bàn", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            DiningTable sel = (DiningTable) cbTables.getSelectedItem();
            if (sel == null) return;
            String newName = txtName.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên bàn không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                tableController.updateTable(sel.getId(), newName, (int) spinCap.getValue());
                JOptionPane.showMessageDialog(this, "Đã cập nhật bàn thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                loadTablesAsync();
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi cập nhật bàn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDeleteTableDialog() {
        if (tableList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có bàn nào trong hệ thống!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<DiningTable> cbTables = new JComboBox<>(tableList.toArray(new DiningTable[0]));
        JPanel p = new JPanel(new GridLayout(2, 1, 8, 8));
        p.add(new JLabel("Chọn bàn cần xóa khỏi hệ thống:"));
        p.add(cbTables);

        int opt = JOptionPane.showConfirmDialog(this, p, "Xóa Bàn Ăn", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            DiningTable sel = (DiningTable) cbTables.getSelectedItem();
            if (sel == null) return;
            if (sel.getStatus() == TableStatus.OCCUPIED) {
                JOptionPane.showMessageDialog(this, "Bàn đang có khách phục vụ! Không thể xóa.", "Không thể xóa", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa bàn '" + sel.getName() + "' không?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    tableController.deleteTable(sel.getId());
                    JOptionPane.showMessageDialog(this, "Đã xóa bàn thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    loadTablesAsync();
                    if (onTableStatusChanged != null) onTableStatusChanged.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi xóa bàn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showMoveTableDialog() {
        List<DiningTable> occupied = tableList.stream().filter(t -> t.getStatus() == TableStatus.OCCUPIED).toList();
        List<DiningTable> available = tableList.stream().filter(DiningTable::isAvailable).toList();

        if (occupied.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hiện tại không có bàn nào đang phục vụ để chuyển!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có bàn trống nào để chuyển đến!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<DiningTable> cbFrom = new JComboBox<>(occupied.toArray(new DiningTable[0]));
        JComboBox<DiningTable> cbTo = new JComboBox<>(available.toArray(new DiningTable[0]));

        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Chuyển từ bàn (Đang phục vụ):"));
        p.add(cbFrom);
        p.add(new JLabel("Chuyển sang bàn (Trống):"));
        p.add(cbTo);

        int opt = JOptionPane.showConfirmDialog(this, p, "Chuyển Bàn Ăn", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            DiningTable fromTable = (DiningTable) cbFrom.getSelectedItem();
            DiningTable toTable = (DiningTable) cbTo.getSelectedItem();
            if (fromTable == null || toTable == null) return;

            try {
                Order openOrder = new com.pos.dao.OrderDAO().findOpenByTable(fromTable.getId());
                if (openOrder == null) {
                    tableController.updateTableStatus(toTable.getId(), TableStatus.OCCUPIED);
                    tableController.updateTableStatus(fromTable.getId(), TableStatus.AVAILABLE);
                } else {
                    tableController.moveTable(openOrder.getId(), toTable.getId(), currentUser);
                }
                JOptionPane.showMessageDialog(this, "Đã chuyển thành công từ '" + fromTable.getName() + "' sang '" + toTable.getName() + "'!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadTablesAsync();
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi chuyển bàn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showMergeTableDialog() {
        List<DiningTable> occupied = tableList.stream().filter(t -> t.getStatus() == TableStatus.OCCUPIED).toList();
        if (occupied.size() < 2) {
            JOptionPane.showMessageDialog(this, "Cần ít nhất 2 bàn đang phục vụ để thực hiện gộp bàn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<DiningTable> cbSource = new JComboBox<>(occupied.toArray(new DiningTable[0]));
        JComboBox<DiningTable> cbTarget = new JComboBox<>(occupied.toArray(new DiningTable[0]));
        if (cbTarget.getItemCount() > 1) cbTarget.setSelectedIndex(1);

        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Bàn chuyển gộp (Sẽ giải phóng bàn):"));
        p.add(cbSource);
        p.add(new JLabel("Bàn đích (Nhận tất cả món ăn):"));
        p.add(cbTarget);

        int opt = JOptionPane.showConfirmDialog(this, p, "Gộp Bàn Ăn", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            DiningTable src = (DiningTable) cbSource.getSelectedItem();
            DiningTable tgt = (DiningTable) cbTarget.getSelectedItem();
            if (src == null || tgt == null || src.getId() == tgt.getId()) {
                JOptionPane.showMessageDialog(this, "Bàn chuyển gộp và bàn đích phải khác nhau!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                com.pos.dao.OrderDAO oDAO = new com.pos.dao.OrderDAO();
                Order fromOrder = oDAO.findOpenByTable(src.getId());
                Order toOrder = oDAO.findOpenByTable(tgt.getId());

                if (fromOrder == null || toOrder == null) {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy hóa đơn đang mở trên một trong hai bàn!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                tableController.mergeTables(fromOrder.getId(), toOrder.getId());
                JOptionPane.showMessageDialog(this, "Đã gộp toàn bộ món từ '" + src.getName() + "' vào '" + tgt.getName() + "' thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadTablesAsync();
                if (onTableStatusChanged != null) onTableStatusChanged.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi gộp bàn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================
    private JLabel createBadge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(5, 10, 5, 10));
        return l;
    }

    private JToggleButton createFilterToggle(String text, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}
