package com.pos.view;

import com.pos.controller.AuthController;
import com.pos.model.User;
import com.pos.utils.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;

public class LoginForm extends JFrame {
    private static final Logger log = LogManager.getLogger(LoginForm.class);
    private final AuthController authController = new AuthController();

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;

    public LoginForm() {
        setTitle("Restaurant POS - Đăng nhập hệ thống");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 410);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(30, 30, 42));

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        header.setBackground(new Color(30, 30, 42));
        header.setBorder(new EmptyBorder(22, 0, 10, 0));
        JLabel title = new JLabel("RESTAURANT POS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(255, 193, 7));
        header.add(title);

        // Form panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(45, 45, 62));
        form.setBorder(new EmptyBorder(16, 35, 16, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setForeground(new Color(230, 230, 240));
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(lblUser, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0;
        txtUsername = new JTextField(20);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setText(""); // Để trống mặc định để bảo mật
        form.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblPwd = new JLabel("Mật khẩu:");
        lblPwd.setForeground(new Color(230, 230, 240));
        lblPwd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(lblPwd, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setText(""); // Để trống mặc định để bảo mật
        form.add(txtPassword, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 4;
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(new Color(255, 82, 82));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(lblStatus, gbc);

        // Login Button
        gbc.gridx = 0; gbc.gridy = 5;
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(255, 160, 0));
        btnLogin.setForeground(Color.BLACK);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> doLogin());
        form.add(btnLogin, gbc);

        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        });
        txtUsername.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) txtPassword.requestFocusInWindow();
            }
        });

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);

        // Footer với nút Cấu hình CSDL
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(30, 30, 42));
        footer.setBorder(new EmptyBorder(6, 16, 10, 16));

        JLabel copy = new JLabel("Hệ thống Quản lý Nhà hàng POS © 2026");
        copy.setForeground(new Color(140, 140, 160));
        copy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(copy, BorderLayout.WEST);

        JButton btnConfig = new JButton("Cấu hình CSDL");
        btnConfig.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnConfig.setFocusPainted(false);
        btnConfig.addActionListener(e -> showDbConfigDialog());
        footer.add(btnConfig, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        btnLogin.setEnabled(false);
        lblStatus.setText("Đang kết nối CSDL và xác thực...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return authController.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        dispose();
                        SwingUtilities.invokeLater(() -> new MainDashboard(user).setVisible(true));
                    } else {
                        lblStatus.setText("Sai tên đăng nhập hoặc mật khẩu!");
                        txtPassword.setText("");
                        btnLogin.setEnabled(true);
                    }
                } catch (Exception ex) {
                    Throwable cause = (ex instanceof ExecutionException && ex.getCause() != null) ? ex.getCause() : ex;
                    btnLogin.setEnabled(true);

                    if (cause instanceof AuthController.AccountLockedException) {
                        lblStatus.setText(cause.getMessage());
                        txtPassword.setText("");
                        JOptionPane.showMessageDialog(LoginForm.this,
                            cause.getMessage(),
                            "Tài khoản bị khóa",
                            JOptionPane.WARNING_MESSAGE);
                    } else if (cause instanceof AuthController.InvalidCredentialsException) {
                        lblStatus.setText(cause.getMessage());
                        txtPassword.setText("");
                    } else {
                        lblStatus.setText("Không thể kết nối SQL Server!");

                        int opt = JOptionPane.showConfirmDialog(LoginForm.this,
                            "Lỗi kết nối CSDL tới máy chủ " + DatabaseConnection.getDbHost() + "!\n\n" +
                            "Nguyên nhân thường gặp:\n" +
                            "1. Nếu dùng SQL Server trên máy này: Hãy kiểm tra Host là 'localhost' hoặc 'localhost\\SQLEXPRESS'.\n" +
                            "2. Nếu dùng qua Radmin VPN: Vui lòng bật phần mềm Radmin VPN.\n\n" +
                            "Bạn có muốn mở bảng 'Cấu hình CSDL' để kiểm tra không?",
                            "Lỗi kết nối Cơ sở dữ liệu", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

                        if (opt == JOptionPane.YES_OPTION) {
                            showDbConfigDialog();
                        }
                    }
                }
            }
        };
        worker.execute();
    }

    private void showDbConfigDialog() {
        JDialog dialog = new JDialog(this, "Cấu hình kết nối SQL Server", true);
        dialog.setSize(440, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        JTextField tfHost = new JTextField(DatabaseConnection.getDbHost());
        JTextField tfPort = new JTextField(DatabaseConnection.getDbPort());
        JTextField tfName = new JTextField(DatabaseConnection.getDbName());
        JTextField tfUser = new JTextField(DatabaseConnection.getDbUser());
        JPasswordField tfPass = new JPasswordField(DatabaseConnection.getDbPass());

        form.add(new JLabel("Máy chủ (Host / IP / Instance):"));
        form.add(tfHost);
        form.add(new JLabel("Cổng (Port):"));
        form.add(tfPort);
        form.add(new JLabel("Tên Cơ sở dữ liệu:"));
        form.add(tfName);
        form.add(new JLabel("Tài khoản SQL (User):"));
        form.add(tfUser);
        form.add(new JLabel("Mật khẩu (Password):"));
        form.add(tfPass);

        dialog.add(form, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnTest = new JButton("Kiểm tra kết nối");
        btnTest.addActionListener(e -> {
            btnTest.setEnabled(false);
            btnTest.setText("Đang kiểm tra...");
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return DatabaseConnection.testConnection(
                        tfHost.getText().trim(),
                        tfPort.getText().trim(),
                        tfName.getText().trim(),
                        tfUser.getText().trim(),
                        new String(tfPass.getPassword())
                    );
                }
                @Override
                protected void done() {
                    try {
                        String err = get();
                        if (err == null) {
                            JOptionPane.showMessageDialog(dialog, "Kết nối tới SQL Server thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Lỗi kết nối:\n" + err, "Thất bại", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ignored) {}
                    btnTest.setEnabled(true);
                    btnTest.setText("Kiểm tra kết nối");
                }
            }.execute();
        });

        JButton btnSave = new JButton("Lưu & Kết nối lại");
        btnSave.setBackground(new Color(46, 125, 50));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            try {
                DatabaseConnection.saveConfig(
                    tfHost.getText().trim(),
                    tfPort.getText().trim(),
                    tfName.getText().trim(),
                    tfUser.getText().trim(),
                    new String(tfPass.getPassword())
                );
                DatabaseConnection.getInstance().reload();
                JOptionPane.showMessageDialog(dialog, "Đã lưu cấu hình vào db.properties!");
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi lưu cấu hình: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Đóng");
        btnCancel.addActionListener(e -> dialog.dispose());

        bot.add(btnTest);
        bot.add(btnSave);
        bot.add(btnCancel);

        dialog.add(bot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
