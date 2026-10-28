package com.pos.view;

import com.pos.controller.CustomerController;
import com.pos.model.Customer;
import com.pos.model.MembershipTier;
import com.pos.model.User;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DateUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Phân hệ Quản lý Khách hàng (CRM) & Điểm thưởng tích lũy (Loyalty Points).
 */
public class CustomerPanel extends JPanel {
    private final CustomerController customerController;
    private final User currentUser;

    private JTable custTable;
    private DefaultTableModel tableModel;
    private List<Customer> currentList;

    private JTextField txtSearch;
    private JLabel lblTotalCust;
    private JLabel lblSilverCount;
    private JLabel lblGoldCount;
    private JLabel lblDiamondCount;

    public CustomerPanel(User currentUser) {
        this.currentUser = currentUser;
        this.customerController = new CustomerController();
        initComponents();
        loadDataAsync("");
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 247, 250));

        // ==================== TOP BAR & SEARCH ====================
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("QUẢN LÝ KHÁCH HÀNG & HỘI VIÊN TÍCH ĐIỂM");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(33, 33, 33));

        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchActions.setOpaque(false);

        txtSearch = new JTextField(15);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên, SĐT...");
        JButton btnSearch = createButton("Tìm kiếm", new Color(30, 136, 229));
        btnSearch.addActionListener(e -> loadDataAsync(txtSearch.getText().trim()));
        txtSearch.addActionListener(e -> loadDataAsync(txtSearch.getText().trim()));

        JButton btnAdd = createButton("+ Thêm khách", new Color(46, 125, 50));
        btnAdd.addActionListener(e -> showAddCustomerDialog());

        JButton btnEdit = createButton("Sửa", new Color(230, 81, 0));
        btnEdit.addActionListener(e -> showEditCustomerDialog());

        JButton btnDelete = createButton("Xóa", new Color(211, 47, 47));
        btnDelete.addActionListener(e -> deleteSelected());

        JButton btnRefresh = createButton("Làm mới", new Color(120, 144, 156));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            loadDataAsync("");
        });

        searchActions.add(new JLabel("Tìm kiếm:"));
        searchActions.add(txtSearch);
        searchActions.add(btnSearch);
        searchActions.add(btnAdd);
        searchActions.add(btnEdit);
        searchActions.add(btnDelete);
        searchActions.add(btnRefresh);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(searchActions, BorderLayout.EAST);

        // ==================== KPI CARDS ====================
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 12, 12));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        lblTotalCust = new JLabel("0", SwingConstants.CENTER);
        lblSilverCount = new JLabel("0", SwingConstants.CENTER);
        lblGoldCount = new JLabel("0", SwingConstants.CENTER);
        lblDiamondCount = new JLabel("0", SwingConstants.CENTER);

        cardsPanel.add(createKpiCard("Tổng khách hàng", lblTotalCust, new Color(30, 136, 229)));
        cardsPanel.add(createKpiCard("Hội viên Bạc (SILVER)", lblSilverCount, new Color(92, 107, 115)));
        cardsPanel.add(createKpiCard("Hội viên Vàng (GOLD)", lblGoldCount, new Color(245, 124, 0)));
        cardsPanel.add(createKpiCard("Kim Cương (DIAMOND)", lblDiamondCount, new Color(142, 36, 170)));

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.setOpaque(false);
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(cardsPanel, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        // ==================== TABLE ====================
        String[] cols = {"ID", "Họ tên", "Số điện thoại", "Email", "Địa chỉ", "Điểm tích lũy", "Quy đổi", "Tổng chi tiêu", "Hạng hội viên", "Ngày tạo"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        custTable = new JTable(tableModel);
        custTable.setRowHeight(32);
        custTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        custTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        custTable.getTableHeader().setBackground(new Color(238, 242, 246));
        custTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(custTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224)));
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(227, 232, 239), 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(new Color(117, 117, 117));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(accent);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
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

    public void loadDataAsync(String keyword) {
        new SwingWorker<List<Customer>, Void>() {
            @Override
            protected List<Customer> doInBackground() {
                return customerController.search(keyword);
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    int silver = 0, gold = 0, diamond = 0;
                    for (Customer c : currentList) {
                        if (c.getMembershipTier() == MembershipTier.SILVER) silver++;
                        else if (c.getMembershipTier() == MembershipTier.GOLD) gold++;
                        else if (c.getMembershipTier() == MembershipTier.DIAMOND) diamond++;

                        tableModel.addRow(new Object[]{
                            c.getId(),
                            c.getName(),
                            c.getPhone(),
                            c.getEmail() != null ? c.getEmail() : "-",
                            c.getAddress() != null ? c.getAddress() : "-",
                            c.getLoyaltyPoints() + " pts",
                            CurrencyFormat.format(c.getPointsValue()),
                            CurrencyFormat.format(c.getTotalSpent()),
                            c.getMembershipTier().getDisplayName(),
                            c.getCreatedAt() != null ? DateUtils.format(c.getCreatedAt()) : "-"
                        });
                    }
                    lblTotalCust.setText(String.valueOf(currentList.size()));
                    lblSilverCount.setText(String.valueOf(silver));
                    lblGoldCount.setText(String.valueOf(gold));
                    lblDiamondCount.setText(String.valueOf(diamond));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private Customer getSelected() {
        int r = custTable.getSelectedRow();
        if (r >= 0 && currentList != null && r < currentList.size()) {
            return currentList.get(r);
        }
        return null;
    }

    private void showAddCustomerDialog() {
        JTextField txtName = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtAddress = new JTextField();

        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.add(new JLabel("Họ tên:")); p.add(txtName);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(new JLabel("Địa chỉ:")); p.add(txtAddress);

        int res = JOptionPane.showConfirmDialog(this, p, "Thêm khách hàng mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                customerController.createCustomer(txtName.getText(), txtPhone.getText(), txtEmail.getText(), txtAddress.getText());
                JOptionPane.showMessageDialog(this, "Thêm khách hàng thành công!");
                loadDataAsync("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditCustomerDialog() {
        Customer c = getSelected();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần chỉnh sửa.");
            return;
        }

        JTextField txtName = new JTextField(c.getName());
        JTextField txtPhone = new JTextField(c.getPhone());
        JTextField txtEmail = new JTextField(c.getEmail() != null ? c.getEmail() : "");
        JTextField txtAddress = new JTextField(c.getAddress() != null ? c.getAddress() : "");
        JTextField txtPoints = new JTextField(String.valueOf(c.getLoyaltyPoints()));

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.add(new JLabel("Họ tên:")); p.add(txtName);
        p.add(new JLabel("Số điện thoại:")); p.add(txtPhone);
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(new JLabel("Địa chỉ:")); p.add(txtAddress);
        p.add(new JLabel("Điểm tích lũy:")); p.add(txtPoints);

        int res = JOptionPane.showConfirmDialog(this, p, "Chỉnh sửa khách hàng #" + c.getId(), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                c.setName(txtName.getText().trim());
                c.setPhone(txtPhone.getText().trim());
                c.setEmail(txtEmail.getText().trim());
                c.setAddress(txtAddress.getText().trim());
                int newPoints = Integer.parseInt(txtPoints.getText().trim());
                c.setLoyaltyPoints(newPoints);
                c.setMembershipTier(MembershipTier.fromPoints(newPoints));
                customerController.updateCustomer(c);
                JOptionPane.showMessageDialog(this, "Cập nhật thành công! Hạng thành viên hiện tại: " + c.getMembershipTier().getDisplayName());
                loadDataAsync("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelected() {
        Customer c = getSelected();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần xóa.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa khách hàng " + c.getName() + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            customerController.deleteCustomer(c.getId());
            loadDataAsync("");
        }
    }

    public CustomerController getCustomerController() {
        return customerController;
    }
}
