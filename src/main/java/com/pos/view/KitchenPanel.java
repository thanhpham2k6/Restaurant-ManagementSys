package com.pos.view;

import com.pos.controller.KitchenController;
import com.pos.controller.OrderObserver;
import com.pos.dao.OrderDAO;
import com.pos.dao.TableDAO;
import com.pos.model.*;
import com.pos.utils.TableUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class KitchenPanel extends JPanel implements OrderObserver {
    private final KitchenController kitchenController;
    private final User currentUser;
    private final OrderDAO orderDAO = new OrderDAO();
    private final TableDAO tableDAO = new TableDAO();

    private DefaultTableModel tableModel;
    private JTable itemTable;
    private javax.swing.Timer autoRefreshTimer;
    private javax.swing.Timer clockTimer;

    private JLabel lblPendingBadge;
    private JLabel lblCookingBadge;
    private JLabel lblTotalBadge;
    private JLabel lblClock;
    private String currentFilter = "ALL";

    public KitchenPanel(KitchenController kitchenController, User currentUser) {
        this.kitchenController = kitchenController;
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        initUI();
        startAutoRefresh();
    }

    private void initUI() {
        // 1. Header (Dark Navy Theme with KPI Badges & Live Clock)
        JPanel header = new JPanel(new BorderLayout(10, 6));
        header.setBackground(new Color(24, 38, 59));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("MÀN HÌNH BẾP TRƯỞNG & ĐIỀU PHỐI MÓN (KDS)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(255, 214, 102));
        titleBox.add(title);

        lblPendingBadge = createKpiChip("Chờ nhận: 0", new Color(245, 158, 11));
        lblCookingBadge = createKpiChip("Đang nấu: 0", new Color(239, 68, 68));
        lblTotalBadge = createKpiChip("Tổng chờ: 0", new Color(59, 130, 246));
        titleBox.add(lblPendingBadge);
        titleBox.add(lblCookingBadge);
        titleBox.add(lblTotalBadge);

        header.add(titleBox, BorderLayout.WEST);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBox.setOpaque(false);

        lblClock = new JLabel();
        lblClock.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblClock.setForeground(new Color(226, 232, 240));
        updateClock();
        clockTimer = new javax.swing.Timer(1000, e -> updateClock());
        clockTimer.start();
        rightBox.add(lblClock);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(241, 245, 249));
        btnRefresh.setForeground(new Color(30, 41, 59));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadItems());
        rightBox.add(btnRefresh);

        header.add(rightBox, BorderLayout.EAST);

        // Filter Bar (Sub-header)
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(new Color(30, 46, 70));
        filterBar.setBorder(new EmptyBorder(4, 14, 4, 14));

        JLabel lblFilter = new JLabel("Bộ lọc trạng thái:");
        lblFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFilter.setForeground(new Color(203, 213, 225));
        filterBar.add(lblFilter);

        ButtonGroup bg = new ButtonGroup();
        JToggleButton tbAll = createFilterBtn("Tất cả", true);
        JToggleButton tbPending = createFilterBtn("Chờ nhận món", false);
        JToggleButton tbCooking = createFilterBtn("Đang nấu", false);

        bg.add(tbAll); bg.add(tbPending); bg.add(tbCooking);
        filterBar.add(tbAll); filterBar.add(tbPending); filterBar.add(tbCooking);

        tbAll.addActionListener(e -> { currentFilter = "ALL"; loadItems(); });
        tbPending.addActionListener(e -> { currentFilter = "PENDING"; loadItems(); });
        tbCooking.addActionListener(e -> { currentFilter = "COOKING"; loadItems(); });

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(header, BorderLayout.NORTH);
        topContainer.add(filterBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(
            new Object[]{"ID", "Đơn #", "Bàn", "Tên món ăn", "Số lượng", "Ghi chú", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        itemTable = new JTable(tableModel);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemTable.setRowHeight(38);
        itemTable.setShowGrid(true);
        itemTable.setGridColor(new Color(241, 245, 249));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemTable.getTableHeader().setBackground(new Color(241, 245, 249));
        itemTable.getTableHeader().setForeground(new Color(51, 65, 85));
        itemTable.getColumnModel().getColumn(0).setMaxWidth(0);
        itemTable.getColumnModel().getColumn(0).setMinWidth(0);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        itemTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        itemTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableUtils.applySmartSorting(itemTable);
        itemTable.setDefaultRenderer(Object.class, new StatusCellRenderer());

        add(new JScrollPane(itemTable), BorderLayout.CENTER);

        // Buttons
        if (currentUser != null && currentUser.getRole() != Role.CASHIER) {
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
            btnPanel.setBackground(new Color(248, 250, 252));
            btnPanel.setBorder(new EmptyBorder(6, 10, 6, 10));

            JButton btnAccept = createActionButton("Nhận món & Trừ kho", new Color(46, 125, 50));
            JButton btnCooking = createActionButton("Bắt đầu nấu", new Color(239, 108, 0));
            JButton btnDone = createActionButton("Báo nấu xong", new Color(25, 118, 210));
            JButton btnReject = createActionButton("Từ chối món", new Color(198, 40, 40));
            JButton btnAcceptAll = createActionButton("Nhận tất cả món chờ", new Color(13, 148, 136));

            btnAccept.addActionListener(e -> acceptSelected());
            btnCooking.addActionListener(e -> startCookingSelected());
            btnDone.addActionListener(e -> markDoneSelected());
            btnReject.addActionListener(e -> rejectSelected());
            btnAcceptAll.addActionListener(e -> acceptAllPending());

            btnPanel.add(btnAccept);
            btnPanel.add(btnCooking);
            btnPanel.add(btnDone);
            btnPanel.add(btnAcceptAll);
            btnPanel.add(btnReject);
            add(btnPanel, BorderLayout.SOUTH);
        }

        loadItems();
    }

    private JLabel createKpiChip(String text, Color bg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Color.WHITE);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(new EmptyBorder(4, 10, 4, 10));
        return l;
    }

    private JToggleButton createFilterBtn(String text, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton createActionButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void updateClock() {
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
        lblClock.setText(java.time.LocalDateTime.now().format(dtf));
    }

    public void loadItems() {
        tableModel.setRowCount(0);
        if (kitchenController == null) return;
        List<OrderDetail> items = kitchenController.getActiveItems();
        List<DiningTable> tables = tableDAO.findAll();

        int pendingCount = 0;
        int cookingCount = 0;

        for (OrderDetail d : items) {
            if (d.getStatus() == ItemStatus.PENDING || d.getStatus() == ItemStatus.ACCEPTED) pendingCount++;
            else if (d.getStatus() == ItemStatus.COOKING) cookingCount++;

            // Filter check
            if ("PENDING".equals(currentFilter) && (d.getStatus() != ItemStatus.PENDING && d.getStatus() != ItemStatus.ACCEPTED)) {
                continue;
            }
            if ("COOKING".equals(currentFilter) && d.getStatus() != ItemStatus.COOKING) {
                continue;
            }

            String status = switch (d.getStatus()) {
                case PENDING  -> "Chờ nhận món";
                case ACCEPTED -> "Đã nhận (chờ nấu)";
                case COOKING  -> "Đang nấu...";
                case DONE     -> "Đã nấu xong";
                case REJECTED -> "Đã từ chối";
            };

            String tableName = "?";
            try {
                Order order = orderDAO.findById(d.getOrderId());
                if (order != null && order.getTable() != null) {
                    int tid = order.getTable().getId();
                    tableName = tables.stream()
                        .filter(t -> t.getId() == tid)
                        .map(DiningTable::getName)
                        .findFirst().orElse("Bàn " + tid);
                }
            } catch (Exception ignored) {}

            String noteDisplay = (d.getNote() != null && !d.getNote().trim().isEmpty())
                ? "[!] " + d.getNote().trim() : "-";

            tableModel.addRow(new Object[]{
                d.getId(),
                "#" + d.getOrderId(),
                tableName,
                d.getItem() != null ? d.getItem().getName() : "?",
                d.getQuantity(),
                noteDisplay,
                status
            });
        }

        if (lblPendingBadge != null) lblPendingBadge.setText("Chờ nhận: " + pendingCount);
        if (lblCookingBadge != null) lblCookingBadge.setText("Đang nấu: " + cookingCount);
        if (lblTotalBadge != null) lblTotalBadge.setText("Tổng chờ: " + items.size());
    }

    private void acceptAllPending() {
        if (kitchenController == null) return;
        List<OrderDetail> items = kitchenController.getActiveItems();
        int count = 0;
        for (OrderDetail d : items) {
            if (d.getStatus() == ItemStatus.PENDING) {
                kitchenController.acceptItem(d.getId(), currentUser);
                count++;
            }
        }
        if (count > 0) {
            JOptionPane.showMessageDialog(this, "Đã nhận nhanh " + count + " món chờ chế biến!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Không có món nào đang ở trạng thái 'Chờ nhận'!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
        loadItems();
    }

    private int getSelectedDetailId() {
        int viewRow = itemTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 món ăn trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        int row = itemTable.convertRowIndexToModel(viewRow);
        return (int) tableModel.getValueAt(row, 0);
    }

    private void acceptSelected() {
        int detailId = getSelectedDetailId();
        if (detailId < 0 || kitchenController == null) return;
        kitchenController.acceptItem(detailId, currentUser);
        loadItems();
    }

    private void startCookingSelected() {
        int detailId = getSelectedDetailId();
        if (detailId < 0 || kitchenController == null) return;
        kitchenController.startCooking(detailId);
        loadItems();
    }

    private void markDoneSelected() {
        int detailId = getSelectedDetailId();
        if (detailId < 0 || kitchenController == null) return;
        // Kiểm tra trạng thái hiện tại - chỉ cho phép báo xong khi đang nấu
        int viewRow = itemTable.getSelectedRow();
        int modelRow = itemTable.convertRowIndexToModel(viewRow);
        String currentStatus = tableModel.getValueAt(modelRow, 6).toString();
        if (!currentStatus.contains("Đang nấu")) {
            JOptionPane.showMessageDialog(this,
                "Chỉ có thể báo xong khi món đang ở trạng thái 'Đang nấu'!\nTrạng thái hiện tại: " + currentStatus,
                "Không thể thực hiện", JOptionPane.WARNING_MESSAGE);
            return;
        }
        kitchenController.markDone(detailId);
        loadItems();
    }

    private void rejectSelected() {
        int detailId = getSelectedDetailId();
        if (detailId < 0 || kitchenController == null) return;
        String reason = JOptionPane.showInputDialog(this, "Nhập lý do từ chối (hết nguyên liệu...):", "Hết nguyên liệu");
        if (reason == null) return;
        kitchenController.rejectItem(detailId, reason);
        loadItems();
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new javax.swing.Timer(10_000, e -> SwingUtilities.invokeLater(this::loadItems));
        autoRefreshTimer.start();
    }

    @Override
    public void onOrderChanged(OrderEvent event) {
        SwingUtilities.invokeLater(this::loadItems);
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && row >= 0 && row < table.getRowCount()) {
                int modelRow = table.convertRowIndexToModel(row);
                String val = table.getModel().getValueAt(modelRow, 6).toString();
                if (val.contains("Đang nấu")) {
                    c.setBackground(new Color(254, 243, 199));
                    c.setForeground(new Color(146, 64, 14));
                } else if (val.contains("Đã nấu xong") || val.contains("xong")) {
                    c.setBackground(new Color(220, 252, 231));
                    c.setForeground(new Color(22, 101, 52));
                } else if (val.contains("Từ chối") || val.contains("từ chối")) {
                    c.setBackground(new Color(254, 226, 226));
                    c.setForeground(new Color(185, 28, 28));
                } else if (val.contains("Đã nhận")) {
                    c.setBackground(new Color(224, 242, 254));
                    c.setForeground(new Color(3, 105, 161));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(new Color(30, 41, 59));
                }

                // Highlight note column with special warning color if not empty
                if (column == 5 && value != null && value.toString().startsWith("[!]")) {
                    c.setForeground(new Color(194, 65, 12));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
            }
            return c;
        }
    }
}
