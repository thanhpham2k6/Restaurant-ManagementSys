package com.pos.view;

import com.pos.controller.UserController;
import com.pos.model.Role;
import com.pos.model.User;
import com.pos.utils.TableUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private final User currentUser;
    private final UserController userController = new UserController();

    private DefaultTableModel userTableModel;
    private JTable userTable;
    private List<User> loadedUsers;

    public UserManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(14, 18, 16, 18));
        initUI();
        loadUsers();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("QUẢN LÝ TÀI KHOẢN & PHÂN QUYỀN HỆ THỐNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSub = new JLabel("Thêm tài khoản nhân viên mới, phân quyền (Thu ngân, Bếp, Quản lý) hoặc xóa/khóa tài khoản.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        header.add(titleBox, BorderLayout.WEST);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        JButton btnAdd = new JButton("+ Đăng ký tài khoản mới");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> showRegisterDialog());
        toolbar.add(btnAdd);

        JButton btnToggle = new JButton("Khóa / Mở khóa");
        btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggle.setBackground(new Color(245, 158, 11));
        btnToggle.setForeground(Color.WHITE);
        btnToggle.setFocusPainted(false);
        btnToggle.addActionListener(e -> toggleSelectedUser());
        toolbar.add(btnToggle);

        JButton btnResetPass = new JButton("Đặt lại mật khẩu");
        btnResetPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnResetPass.setBackground(new Color(59, 130, 246));
        btnResetPass.setForeground(Color.WHITE);
        btnResetPass.setFocusPainted(false);
        btnResetPass.addActionListener(e -> showResetPasswordDialog());
        toolbar.add(btnResetPass);

        JButton btnDelete = new JButton("Xóa tài khoản");
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.setBackground(new Color(225, 29, 72));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        btnDelete.addActionListener(e -> deleteSelectedUser());
        toolbar.add(btnDelete);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(241, 245, 249));
        btnRefresh.setForeground(new Color(30, 41, 59));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadUsers());
        toolbar.add(btnRefresh);

        header.add(toolbar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table Panel
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));

        userTableModel = new DefaultTableModel(
            new Object[]{"ID", "Tên đăng nhập", "Họ và tên", "Vai trò (Phân quyền)", "Trạng thái", "Ghi chú quyền hạn"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userTableModel);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userTable.setRowHeight(36);
        userTable.setShowGrid(true);
        userTable.setGridColor(new Color(241, 245, 249));
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        userTable.getTableHeader().setBackground(new Color(241, 245, 249));
        userTable.getTableHeader().setForeground(new Color(51, 65, 85));
        userTable.setSelectionBackground(new Color(237, 242, 247));
        userTable.setSelectionForeground(new Color(15, 23, 42));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableUtils.applySmartSorting(userTable);

        userTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        userTable.getColumnModel().getColumn(5).setPreferredWidth(260);

        userTable.setDefaultRenderer(Object.class, new UserTableRenderer());

        tableContainer.add(new JScrollPane(userTable), BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void loadUsers() {
        userTableModel.setRowCount(0);
        try {
            loadedUsers = userController.getAllUsers();
            for (User u : loadedUsers) {
                String roleDisplay = switch (u.getRole()) {
                    case MANAGER -> "Quản lý (MANAGER)";
                    case CASHIER -> "Thu ngân (CASHIER)";
                    case KITCHEN -> "Nhà Bếp (KITCHEN)";
                };
                String statusDisplay = u.isActive() ? "Đang hoạt động" : "Đã khóa (Vô hiệu)";
                String desc = switch (u.getRole()) {
                    case MANAGER -> "Toàn quyền: Báo cáo, Quản lý tài khoản, POS, Kho, Bếp";
                    case CASHIER -> "Mở bàn, thêm món, gửi bếp, thanh toán hóa đơn POS";
                    case KITCHEN -> "Xem danh sách món gửi từ thu ngân, nhận món, nấu xong";
                };

                userTableModel.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    roleDisplay,
                    statusDisplay,
                    desc
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách tài khoản: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private User getSelectedUser() {
        int viewRow = userTable.getSelectedRow();
        if (viewRow < 0 || loadedUsers == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tài khoản trong danh sách!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int row = userTable.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= loadedUsers.size()) {
            return null;
        }
        return loadedUsers.get(row);
    }

    private void showRegisterDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Đăng ký tài khoản nhân viên mới", true);
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JTextField txtName = new JTextField();
        JComboBox<Role> cbRole = new JComboBox<>(Role.values());
        cbRole.setSelectedItem(Role.CASHIER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        form.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        form.add(new JLabel("Họ và tên:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        form.add(new JLabel("Vai trò phân quyền:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(cbRole, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSubmit = new JButton("Tạo tài khoản");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSubmit.setBackground(new Color(16, 185, 129));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.addActionListener(e -> {
            String username = txtUser.getText().trim();
            String password = new String(txtPass.getPassword()).trim();
            String fullName = txtName.getText().trim();
            Role role = (Role) cbRole.getSelectedItem();

            try {
                userController.registerUser(username, password, fullName, role);
                JOptionPane.showMessageDialog(dialog, "Đã tạo tài khoản '" + username + "' thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadUsers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void toggleSelectedUser() {
        User u = getSelectedUser();
        if (u == null) return;

        try {
            userController.toggleStatus(u.getId(), u.isActive(), currentUser);
            String actText = u.isActive() ? "khóa (vô hiệu hóa)" : "kích hoạt lại";
            JOptionPane.showMessageDialog(this, "Đã " + actText + " tài khoản '" + u.getUsername() + "' thành công!");
            loadUsers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showResetPasswordDialog() {
        User u = getSelectedUser();
        if (u == null) return;

        String newPass = JOptionPane.showInputDialog(this, "Nhập mật khẩu mới cho tài khoản '" + u.getUsername() + "':", "password123");
        if (newPass == null || newPass.isBlank()) return;

        try {
            userController.resetPassword(u.getId(), newPass);
            JOptionPane.showMessageDialog(this, "Đã đổi mật khẩu cho tài khoản '" + u.getUsername() + "' thành công!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedUser() {
        User u = getSelectedUser();
        if (u == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa tài khoản '" + u.getUsername() + "' (" + u.getFullName() + ")?",
            "Xác nhận xóa tài khoản", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            int result = userController.deleteUser(u.getId(), currentUser);
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "Đã xóa vĩnh viễn tài khoản '" + u.getUsername() + "' khỏi hệ thống!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else if (result == 2) {
                JOptionPane.showMessageDialog(this,
                    "Tài khoản '" + u.getUsername() + "' đã có lịch sử tạo đơn hàng hoặc kho.\n" +
                    "Hệ thống đã chuyển sang trạng thái VÔ HIỆU HÓA (Đã khóa) để bảo toàn số liệu sổ sách!",
                    "Đã vô hiệu hóa tài khoản", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
            loadUsers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class UserTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0 || column == 1 || column == 3 || column == 4) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (!isSelected && row < table.getRowCount()) {
                String status = table.getModel().getValueAt(row, 4).toString();
                if (column == 4) {
                    if (status.contains("Đang hoạt động")) {
                        c.setForeground(new Color(22, 101, 52));
                        c.setBackground(new Color(220, 252, 231));
                    } else {
                        c.setForeground(new Color(153, 27, 27));
                        c.setBackground(new Color(254, 226, 226));
                    }
                    return c;
                }
                c.setBackground(Color.WHITE);
                c.setForeground(new Color(30, 41, 59));
            }
            return c;
        }
    }
}
