package com.pos.view;

import com.pos.dao.CashTransactionDAO;
import com.pos.model.CashTransaction;
import com.pos.model.User;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CashbookPanel extends JPanel {
    private final User currentUser;
    private final CashTransactionDAO cashDAO = new CashTransactionDAO();

    private JLabel lblTotalReceipt;
    private JLabel lblTotalPayment;
    private JLabel lblNetBalance;

    private JComboBox<String> cbDateFilter;
    private JComboBox<String> cbTypeFilter;
    private JTextField txtSearch;

    private JTable txTable;
    private DefaultTableModel tableModel;
    private List<CashTransaction> currentList = new ArrayList<>();

    public CashbookPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(16, 20, 20, 20));
        initComponents();
        loadDataAsync();
    }

    private void initComponents() {
        // TOP HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("SỔ QUỸ THU - CHI & DÒNG TIỀN NỘI BỘ");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSub = new JLabel("Quản lý phiếu thu, phiếu chi tiền mặt, hóa đơn chi phí và tồn quỹ thực tế");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        headerPanel.add(titleBox, BorderLayout.WEST);

        // Action buttons
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnReceipt = new JButton("+ Lập Phiếu Thu");
        btnReceipt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReceipt.setBackground(new Color(16, 185, 129));
        btnReceipt.setForeground(Color.WHITE);
        btnReceipt.setFocusPainted(false);
        btnReceipt.addActionListener(e -> showCreateTransactionDialog("RECEIPT"));

        JButton btnPayment = new JButton("- Lập Phiếu Chi");
        btnPayment.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPayment.setBackground(new Color(239, 68, 68));
        btnPayment.setForeground(Color.WHITE);
        btnPayment.setFocusPainted(false);
        btnPayment.addActionListener(e -> showCreateTransactionDialog("PAYMENT"));

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setBackground(new Color(238, 242, 246));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadDataAsync());

        actionBox.add(btnReceipt);
        actionBox.add(btnPayment);
        actionBox.add(btnRefresh);
        headerPanel.add(actionBox, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // CENTER CONTAINER
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // 3 KPI METRIC CARDS
        JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.setPreferredSize(new Dimension(0, 110));

        lblTotalReceipt = new JLabel("0 đ");
        lblTotalPayment = new JLabel("0 đ");
        lblNetBalance = new JLabel("0 đ");

        cardsGrid.add(createMetricCard("TỔNG TIỀN THU", lblTotalReceipt, "Khoản thu bán hàng & dịch vụ", new Color(16, 185, 129), "+ "));
        cardsGrid.add(createMetricCard("TỔNG TIỀN CHI", lblTotalPayment, "Chi phí vận hành & nhập nguyên liệu", new Color(239, 68, 68), "- "));
        cardsGrid.add(createMetricCard("TỒN QUỸ TIỀN MẶT THỰC TẾ", lblNetBalance, "Số dư quỹ khả dụng tại nhà hàng", new Color(59, 130, 246), "= "));

        centerPanel.add(cardsGrid, BorderLayout.NORTH);

        // TABLE SECTION
        JPanel tableCard = new JPanel(new BorderLayout(0, 12));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(14, 16, 16, 16)
        ));

        // Filter toolbar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.setOpaque(false);

        filterBar.add(new JLabel("Thời gian:"));
        cbDateFilter = new JComboBox<>(new String[]{"Hôm nay", "7 ngày qua", "Tháng này", "Toàn bộ thời gian"});
        cbDateFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbDateFilter.addActionListener(e -> loadDataAsync());
        filterBar.add(cbDateFilter);

        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(new JLabel("Loại phiếu:"));
        cbTypeFilter = new JComboBox<>(new String[]{"Tất cả", "Chỉ xem Phiếu Thu", "Chỉ xem Phiếu Chi"});
        cbTypeFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbTypeFilter.addActionListener(e -> filterTableData());
        filterBar.add(cbTypeFilter);

        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(new JLabel("Tìm kiếm:"));
        txtSearch = new JTextField(15);
        txtSearch.putClientProperty("JTextField.placeholderText", "Mã phiếu, người nộp/nhận, lý do...");
        txtSearch.addActionListener(e -> filterTableData());
        filterBar.add(txtSearch);

        JButton btnSearch = new JButton("Lọc");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSearch.addActionListener(e -> filterTableData());
        filterBar.add(btnSearch);

        JButton btnPrintSlip = new JButton("🖨️ In Phiếu / Xem Chi Tiết");
        btnPrintSlip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnPrintSlip.addActionListener(e -> printSelectedSlip());
        filterBar.add(btnPrintSlip);

        tableCard.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Mã phiếu", "Thời gian", "Loại", "Danh mục chi phí", "Số tiền", "Phương thức", "Người nộp / nhận", "Người lập", "Ghi chú"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0) return Integer.class;
                return String.class;
            }
        };

        txTable = new JTable(tableModel);
        txTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txTable.setRowHeight(32);
        txTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        txTable.getTableHeader().setBackground(new Color(248, 250, 252));
        TableUtils.applySmartSorting(txTable);

        // Custom renderer for Type & Amount
        DefaultTableCellRenderer typeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if (val.contains("THU")) {
                    l.setForeground(new Color(16, 185, 129));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if (val.contains("CHI")) {
                    l.setForeground(new Color(239, 68, 68));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else {
                    l.setForeground(new Color(30, 41, 59));
                }
                return l;
            }
        };
        txTable.getColumnModel().getColumn(3).setCellRenderer(typeRenderer);

        DefaultTableCellRenderer amountRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                String typeVal = String.valueOf(table.getValueAt(row, 3));
                if (typeVal.contains("THU")) {
                    l.setForeground(new Color(16, 185, 129));
                } else {
                    l.setForeground(new Color(239, 68, 68));
                }
                return l;
            }
        };
        txTable.getColumnModel().getColumn(5).setCellRenderer(amountRenderer);

        JScrollPane sp = new JScrollPane(txTable);
        sp.setBorder(new LineBorder(new Color(226, 232, 240), 1));
        tableCard.add(sp, BorderLayout.CENTER);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valLabel, String subText, Color accentColor, String prefix) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel topP = new JPanel(new BorderLayout());
        topP.setOpaque(false);
        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblT.setForeground(new Color(100, 116, 139));

        JPanel badge = new JPanel();
        badge.setPreferredSize(new Dimension(8, 8));
        badge.setBackground(accentColor);
        topP.add(lblT, BorderLayout.WEST);
        topP.add(badge, BorderLayout.EAST);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLabel.setForeground(accentColor);

        JLabel lblSub = new JLabel(subText);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(148, 163, 184));

        card.add(topP, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);
        return card;
    }

    public void loadDataAsync() {
        SwingWorker<List<CashTransaction>, Void> worker = new SwingWorker<>() {
            private BigDecimal totalReceipt = BigDecimal.ZERO;
            private BigDecimal totalPayment = BigDecimal.ZERO;
            private BigDecimal balance = BigDecimal.ZERO;

            @Override
            protected List<CashTransaction> doInBackground() {
                LocalDate to = LocalDate.now();
                LocalDate from = LocalDate.now();

                int idx = cbDateFilter != null ? cbDateFilter.getSelectedIndex() : 0;
                if (idx == 0) { // Hôm nay
                    from = to;
                } else if (idx == 1) { // 7 ngày qua
                    from = to.minusDays(7);
                } else if (idx == 2) { // Tháng này
                    from = to.withDayOfMonth(1);
                } else { // Toàn bộ
                    from = LocalDate.of(2020, 1, 1);
                }

                totalReceipt = cashDAO.getTotalReceipt(from, to);
                totalPayment = cashDAO.getTotalPayment(from, to);
                balance = cashDAO.getCurrentCashBalance();

                return cashDAO.findByDateRange(from, to, "ALL");
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    lblTotalReceipt.setText("+ " + CurrencyFormat.format(totalReceipt));
                    lblTotalPayment.setText("- " + CurrencyFormat.format(totalPayment));
                    lblNetBalance.setText(CurrencyFormat.format(balance));
                    filterTableData();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void filterTableData() {
        tableModel.setRowCount(0);
        String typeFilter = cbTypeFilter.getSelectedItem() != null ? (String) cbTypeFilter.getSelectedItem() : "Tất cả";
        String search = txtSearch.getText().trim().toLowerCase();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (CashTransaction tx : currentList) {
            if (typeFilter.contains("Thu") && !tx.isReceipt()) continue;
            if (typeFilter.contains("Chi") && tx.isReceipt()) continue;

            if (!search.isEmpty()) {
                String matchStr = (tx.getCode() + " " + tx.getCategory() + " " + tx.getPersonName() + " " + tx.getNote()).toLowerCase();
                if (!matchStr.contains(search)) continue;
            }

            String typeStr = tx.isReceipt() ? "THU TIỀN" : "CHI TIỀN";
            String sign = tx.isReceipt() ? "+ " : "- ";

            tableModel.addRow(new Object[]{
                tx.getId(),
                tx.getCode(),
                tx.getCreatedAt() != null ? tx.getCreatedAt().format(dtf) : "",
                typeStr,
                tx.getCategory(),
                sign + CurrencyFormat.format(tx.getAmount()),
                tx.getPaymentMethod(),
                tx.getPersonName() != null ? tx.getPersonName() : "",
                tx.getUserName() != null ? tx.getUserName() : "",
                tx.getNote() != null ? tx.getNote() : ""
            });
        }
    }

    private void showCreateTransactionDialog(String type) {
        boolean isReceipt = "RECEIPT".equalsIgnoreCase(type);
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isReceipt ? "LẬP PHIẾU THU TIỀN" : "LẬP PHIẾU CHI TIỀN", true);
        dlg.setSize(480, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 14));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        String codePrefix = isReceipt ? "PT" : "PC";
        String defaultCode = codePrefix + "-" + DateUtils.format(LocalDateTime.now()).replaceAll("[^0-9]", "").substring(0, 8) + "-" + String.format("%03d", (int)(Math.random() * 900) + 100);

        JTextField txtCode = new JTextField(defaultCode);
        JTextField txtAmount = new JTextField();
        JTextField txtPerson = new JTextField();
        JTextField txtNote = new JTextField();

        JComboBox<String> cbCategory;
        if (isReceipt) {
            cbCategory = new JComboBox<>(new String[]{
                "Doanh thu bán hàng", "Thu tiền cọc đặt bàn", "Thu hồi nợ / Khách trả sau", "Thu hoàn ứng", "Thu thanh lý tài sản", "Thu khác"
            });
        } else {
            cbCategory = new JComboBox<>(new String[]{
                "Nhập nguyên liệu tươi sống", "Điện, nước & gas bếp", "Tạm ứng lương nhân viên", "Mua sắm dụng cụ & vật tư", "Chi phí tiếp khách & marketing", "Chi sửa chữa & bảo trì", "Chi phí khác"
            });
        }

        JComboBox<String> cbMethod = new JComboBox<>(new String[]{"Tiền mặt", "Chuyển khoản"});

        form.add(new JLabel("Mã phiếu:"));
        form.add(txtCode);
        form.add(new JLabel("Danh mục:"));
        form.add(cbCategory);
        form.add(new JLabel(isReceipt ? "Số tiền thu (VND):" : "Số tiền chi (VND):"));
        form.add(txtAmount);
        form.add(new JLabel("Phương thức:"));
        form.add(cbMethod);
        form.add(new JLabel(isReceipt ? "Người nộp tiền:" : "Người nhận tiền:"));
        form.add(txtPerson);
        form.add(new JLabel("Lý do / Ghi chú:"));
        form.add(txtNote);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnSave = new JButton(isReceipt ? "Xác nhận Lập Phiếu Thu" : "Xác nhận Lập Phiếu Chi");
        btnSave.setBackground(isReceipt ? new Color(16, 185, 129) : new Color(239, 68, 68));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            String code = txtCode.getText().trim();
            String amountStr = txtAmount.getText().trim().replaceAll("[^0-9]", "");
            if (code.isEmpty() || amountStr.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập đầy đủ mã phiếu và số tiền!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BigDecimal amt = new BigDecimal(amountStr);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(dlg, "Số tiền phải lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            CashTransaction tx = new CashTransaction();
            tx.setCode(code);
            tx.setType(isReceipt ? "RECEIPT" : "PAYMENT");
            tx.setCategory((String) cbCategory.getSelectedItem());
            tx.setAmount(amt);
            tx.setPaymentMethod((String) cbMethod.getSelectedItem());
            tx.setPersonName(txtPerson.getText().trim());
            tx.setNote(txtNote.getText().trim());
            tx.setUserId(currentUser.getId());
            tx.setCreatedAt(LocalDateTime.now());

            int newId = cashDAO.insert(tx);
            if (newId > 0) {
                JOptionPane.showMessageDialog(dlg, "Lập phiếu thành công! Mã: " + code, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
                loadDataAsync();
            } else {
                JOptionPane.showMessageDialog(dlg, "Lỗi khi lưu phiếu (Mã phiếu có thể đã tồn tại)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void printSelectedSlip() {
        int row = txTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng phiếu trong bảng để xem chi tiết / in!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = txTable.convertRowIndexToModel(row);
        int txId = (int) tableModel.getValueAt(modelRow, 0);

        CashTransaction tx = null;
        for (CashTransaction t : currentList) {
            if (t.getId() == txId) {
                tx = t;
                break;
            }
        }
        if (tx == null) return;

        boolean isReceipt = tx.isReceipt();
        String title = isReceipt ? "PHIẾU THU TIỀN" : "PHIẾU CHI TIỀN";
        String personLabel = isReceipt ? "Người nộp tiền" : "Người nhận tiền";

        String html = "<html><body style='font-family: Segoe UI, sans-serif; padding: 15px; width: 360px;'>" +
            "<div style='text-align: center; border-bottom: 2px solid #334155; padding-bottom: 8px;'>" +
            "<h2 style='margin: 0; color: " + (isReceipt ? "#16a34a" : "#dc2626") + ";'>" + title + "</h2>" +
            "<div style='font-size: 11px; color: #64748b; margin-top: 4px;'>Mã phiếu: <b>" + tx.getCode() + "</b></div>" +
            "<div style='font-size: 11px; color: #64748b;'>Ngày lập: " + (tx.getCreatedAt() != null ? tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "") + "</div>" +
            "</div>" +
            "<div style='margin-top: 15px; font-size: 12px; line-height: 1.8;'>" +
            "<div><b>Hạng mục:</b> " + tx.getCategory() + "</div>" +
            "<div><b>" + personLabel + ":</b> " + (tx.getPersonName() != null ? tx.getPersonName() : "Khách lẻ") + "</div>" +
            "<div><b>Hình thức:</b> " + tx.getPaymentMethod() + "</div>" +
            "<div><b>Người lập phiếu:</b> " + (tx.getUserName() != null ? tx.getUserName() : currentUser.getFullName()) + "</div>" +
            "<div><b>Lý do / Nội dung:</b> " + (tx.getNote() != null ? tx.getNote() : "Không ghi chú") + "</div>" +
            "<div style='margin-top: 10px; padding: 10px; background: #f8fafc; border: 1px dashed #cbd5e1; text-align: center;'>" +
            "<div style='font-size: 11px; color: #64748b;'>TỔNG SỐ TIỀN THANH TOÁN</div>" +
            "<div style='font-size: 20px; font-weight: bold; color: " + (isReceipt ? "#16a34a" : "#dc2626") + ";'>" + CurrencyFormat.format(tx.getAmount()) + "</div>" +
            "</div>" +
            "</div>" +
            "<div style='margin-top: 25px; display: flex; justify-content: space-between; font-size: 11px; text-align: center;'>" +
            "<table style='width: 100%;'><tr>" +
            "<td style='width: 50%; text-align: center;'><b>Người lập phiếu</b><br><br><br><i>(Ký, ghi rõ họ tên)</i></td>" +
            "<td style='width: 50%; text-align: center;'><b>" + personLabel + "</b><br><br><br><i>(Ký, ghi rõ họ tên)</i></td>" +
            "</tr></table>" +
            "</div>" +
            "</body></html>";

        JDialog viewDlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chứng từ " + tx.getCode(), true);
        viewDlg.setSize(440, 520);
        viewDlg.setLocationRelativeTo(this);
        viewDlg.setLayout(new BorderLayout());

        JEditorPane ep = new JEditorPane("text/html", html);
        ep.setEditable(false);
        viewDlg.add(new JScrollPane(ep), BorderLayout.CENTER);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnPrint = new JButton("🖨️ In Chứng Từ");
        btnPrint.setBackground(new Color(37, 99, 235));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.addActionListener(e -> {
            try {
                ep.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(viewDlg, "Lỗi in: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> viewDlg.dispose());

        pBtn.add(btnPrint);
        pBtn.add(btnClose);
        viewDlg.add(pBtn, BorderLayout.SOUTH);

        viewDlg.setVisible(true);
    }
}
