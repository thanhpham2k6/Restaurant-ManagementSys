package com.nhom13.restaurant.managementsys.view;

public class LoginFrame extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LoginFrame.class.getName());
    public LoginFrame() {
    initComponents();
    btnDangnhap.addActionListener(this::btnDangnhapActionPerformed);
}
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnDangnhap = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        txtTaikhoan = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        txtMatkhau = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        btnDangnhap.setText("Đăng nhập");

        jButton2.setText("Đăng kí");

        txtTaikhoan.addActionListener(this::txtTaikhoanActionPerformed);

        jLabel1.setText(" Tài khoản");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel2.setText("Đăng nhập");

        jLabel3.setText(" Mật khẩu");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtTaikhoan, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                            .addComponent(txtMatkhau))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(132, 132, 132)
                .addComponent(btnDangnhap)
                .addGap(27, 27, 27)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTaikhoan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMatkhau, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(41, 41, 41)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDangnhap)
                    .addComponent(jButton2))
                .addContainerGap(33, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtTaikhoanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTaikhoanActionPerformed
        
    }//GEN-LAST:event_txtTaikhoanActionPerformed
    private void btnDangnhapActionPerformed(java.awt.event.ActionEvent evt) {
    String taiKhoan = txtTaikhoan.getText().trim();
    
    String matKhau = txtMatkhau.getText().trim(); 

    if (taiKhoan.isEmpty() || matKhau.isEmpty()) {
        javax.swing.JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Tài khoản và Mật khẩu!");
        return;
    }

    String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ?";
    try (java.sql.Connection conn = com.nhom13.restaurant.managementsys.database.Database.getConnection();
         java.sql.PreparedStatement pst = conn.prepareStatement(sql)) {

        pst.setString(1, taiKhoan);
        pst.setString(2, matKhau);

        try (java.sql.ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                String role = rs.getString("role");
                if ("Admin".equalsIgnoreCase(role)) {
                    com.nhom13.restaurant.managementsys.view.AdminMainFrame adminFrame = new com.nhom13.restaurant.managementsys.view.AdminMainFrame();
                    adminFrame.setVisible(true);
                } else {
                    com.nhom13.restaurant.managementsys.view.StaffMainFrame staffFrame = new com.nhom13.restaurant.managementsys.view.StaffMainFrame();
                    staffFrame.setVisible(true);
                }
                this.dispose(); 
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "Tài khoản hoặc mật khẩu không chính xác!", "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        javax.swing.JOptionPane.showMessageDialog(this, "Lỗi kết nối cơ sở dữ liệu!");
    }
}
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDangnhap;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField txtMatkhau;
    private javax.swing.JTextField txtTaikhoan;
    // End of variables declaration//GEN-END:variables
}
