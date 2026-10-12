package com.pos.view;

import com.pos.controller.KitchenController;
import com.pos.controller.OrderObserver;
import com.pos.dao.OrderDAO;
import com.pos.dao.TableDAO;
import com.pos.model.*;

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

    public KitchenPanel(KitchenController kitchenController, User currentUser) {
        this.kitchenController = kitchenController;
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        initUI();
        startAutoRefresh();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 38, 59));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("MÀN HÌNH BẾP TRƯỞNG & ĐIỀU PHỐI MÓN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(255, 214, 102));
        header.add(title, BorderLayout.WEST);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(241, 245, 249));
        btnRefresh.setForeground(new Color(30, 41, 59));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadItems());
        header.add(btnRefresh, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(
            new Object[]{"ID", "Đơn #", "Bàn", "Tên món ăn", "Số lượng", "Ghi chú", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        itemTable = new JTable(tableModel);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemTable.setRowHeight(34);
        itemTable.setShowGrid(true);
        itemTable.setGridColor(new Color(241, 245, 249));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemTable.getTableHeader().setBackground(new Color(241, 245, 249));
        itemTable.getTableHeader().setForeground(new Color(51, 65, 85));
        itemTable.getColumnModel().getColumn(0).setMaxWidth(0);
        itemTable.getColumnModel().getColumn(0).setMinWidth(0);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        itemTable.getColumnModel().getColumn(5).setPreferredWidth(140);
        itemTable.getColumnModel().getColumn(6).setPreferredWidth(140);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setDefaultRenderer(Object.class, new StatusCellRenderer());

        add(new JScrollPane(itemTable), BorderLayout.CENTER);

        // Buttons
        if (currentUser != null && currentUser.getRole() != Role.CASHIER) {
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));

            JButton btnAccept = new JButton("Nhận món & Trừ kho");
            btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnAccept.setBackground(new Color(46, 125, 50));
            btnAccept.setForeground(Color.WHITE);
            btnAccept.setFocusPainted(false);
            btnAccept.addActionListener(e -> acceptSelected());

            JButton btnCooking = new JButton("Bắt đầu nấu");
            btnCooking.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnCooking.setBackground(new Color(239, 108, 0));
            btnCooking.setForeground(Color.WHITE);
            btnCooking.setFocusPainted(false);
            btnCooking.addActionListener(e -> startCookingSelected());

            JButton btnDone = new JButton("Báo nấu xong");
            btnDone.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnDone.setBackground(new Color(25, 118, 210));
            btnDone.setForeground(Color.WHITE);
            btnDone.setFocusPainted(false);
            btnDone.addActionListener(e -> markDoneSelected());

            JButton btnReject = new JButton("Từ chối món");
            btnReject.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnReject.setBackground(new Color(198, 40, 40));
            btnReject.setForeground(Color.WHITE);
            btnReject.setFocusPainted(false);
            btnReject.addActionListener(e -> rejectSelected());

            btnPanel.add(btnAccept);
            btnPanel.add(btnCooking);
            btnPanel.add(btnDone);
            btnPanel.add(btnReject);
            add(btnPanel, BorderLayout.SOUTH);
        }

        loadItems();
    }

    private void loadItems() {
        tableModel.setRowCount(0);
        if (kitchenController == null) return;
        List<OrderDetail> items = kitchenController.getActiveItems();
        List<DiningTable> tables = tableDAO.findAll();

        for (OrderDetail d : items) {
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

            tableModel.addRow(new Object[]{
                d.getId(),
                "#" + d.getOrderId(),
                tableName,
                d.getItem() != null ? d.getItem().getName() : "?",
                d.getQuantity(),
                d.getNote() != null ? d.getNote() : "",
                status
            });
        }
    }

    private int getSelectedDetailId() {
        int row = itemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 món ăn trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
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
            if (!isSelected && row < table.getRowCount()) {
                String val = table.getModel().getValueAt(row, 6).toString();
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
            }
            return c;
        }
    }
}
