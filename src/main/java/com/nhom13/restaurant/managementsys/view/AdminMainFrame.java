package com.nhom13.restaurant.managementsys.view;

import com.nhom13.restaurant.managementsys.dao.*;
import com.nhom13.restaurant.managementsys.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Màn hình quản trị Admin:
 *  Tab 1 – 👥 Nhân viên   : Thêm / Cập nhật / Xóa / Đổi mật khẩu
 *  Tab 2 – 📦 Nguyên liệu  : Thêm / Cập nhật / Xóa / Nhập hàng
 *  Tab 3 – 📁 Danh mục     : Thêm / Cập nhật / Xóa
 *  Tab 4 – 🍜 Thực đơn     : Thêm / Cập nhật / Xóa (lọc theo danh mục)
 *  Tab 5 – 🪑 Bàn ăn       : Thêm / Cập nhật / Xóa
 */
public class AdminMainFrame extends JFrame {

    private final UserDao        userDao        = new UserDao();
    private final IngredientDao  ingredientDao  = new IngredientDao();
    private final CategoryDao    categoryDao    = new CategoryDao();
    private final MenuItemDao    menuItemDao    = new MenuItemDao();
    private final DiningTableDao tableDao       = new DiningTableDao();

    public AdminMainFrame() {
        setTitle("Restaurant Management – Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("👥 Nhân viên",   buildUserTab());
        tabs.addTab("📦 Nguyên liệu", buildIngredientTab());
        tabs.addTab("📁 Danh mục",    buildCategoryTab());
        tabs.addTab("🍜 Thực đơn",    buildMenuItemTab());
        tabs.addTab("🪑 Bàn ăn",      buildTableTab());
        getContentPane().add(tabs);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Utility helpers
    // ═══════════════════════════════════════════════════════════════

    /** Thanh tìm kiếm nhanh – gắn vào TableRowSorter */
    private JPanel searchBar(JTextField txtKw, JButton btnSearch, JButton btnRefresh) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.add(new JLabel("🔍 Tìm kiếm:"));
        p.add(txtKw);
        p.add(btnSearch);
        p.add(btnRefresh);
        return p;
    }

    private JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 1: NHÂN VIÊN
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildUserTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"ID", "Tên đăng nhập", "Mật khẩu", "Họ và tên", "Vai trò", "Ngày tạo"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Form nhập
        JTextField txtUsername = new JTextField(14);
        JTextField txtPassword = new JTextField(14);
        JTextField txtFullname  = new JTextField(18);
        JComboBox<String> cmbRole = new JComboBox<>(new String[]{"Staff", "Kitchen", "Admin"});

        Runnable reload = () -> {
            model.setRowCount(0);
            for (User u : userDao.getAllUsers())
                model.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getPasswordHash(), u.getFullName(),
                    u.getRole(), u.getCreatedAt()});
        };
        reload.run();

        // Chọn dòng → điền form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int mr = table.convertRowIndexToModel(table.getSelectedRow());
                txtUsername.setText(model.getValueAt(mr, 1).toString());
                txtPassword.setText(model.getValueAt(mr, 2).toString());
                txtFullname.setText(model.getValueAt(mr, 3).toString());
                cmbRole.setSelectedItem(model.getValueAt(mr, 4).toString());
            }
        });

        // Buttons
        JButton btnAdd    = new JButton("➕ Thêm");
        JButton btnUpdate = new JButton("✏️ Cập nhật");
        JButton btnDelete = new JButton("🗑️ Xóa");
        JButton btnResetPw = new JButton("🔑 Đặt lại mật khẩu");

        btnAdd.addActionListener(e -> {
            String un = txtUsername.getText().trim();
            String pw = txtPassword.getText().trim();
            String fn = txtFullname.getText().trim();
            if (un.isEmpty() || pw.isEmpty() || fn.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin!", "Cảnh báo", JOptionPane.WARNING_MESSAGE); return;
            }
            User u = new User(); u.setUsername(un); u.setPasswordHash(pw);
            u.setFullName(fn); u.setRole(cmbRole.getSelectedItem().toString());
            userDao.addUser(u);
            reload.run();
            txtUsername.setText(""); txtPassword.setText(""); txtFullname.setText("");
        });

        btnUpdate.addActionListener(e -> {
            int sr = table.getSelectedRow();
            if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nhân viên cần cập nhật!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int uid = (int) model.getValueAt(mr, 0);
            String fn = txtFullname.getText().trim();
            if (fn.isEmpty()) { JOptionPane.showMessageDialog(this, "Họ tên không được trống!"); return; }
            User u = new User(); u.setUserId(uid); u.setFullName(fn);
            u.setRole(cmbRole.getSelectedItem().toString());
            userDao.updateUser(u); reload.run();
        });

        btnDelete.addActionListener(e -> {
            int sr = table.getSelectedRow();
            if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nhân viên cần xóa!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int uid = (int) model.getValueAt(mr, 0);
            if (JOptionPane.showConfirmDialog(this, "Xóa nhân viên này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                userDao.deleteUser(uid); reload.run();
            }
        });

        btnResetPw.addActionListener(e -> {
            int sr = table.getSelectedRow();
            if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nhân viên cần đặt lại mật khẩu!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int uid = (int) model.getValueAt(mr, 0);
            String pw = txtPassword.getText().trim();
            if (pw.isEmpty()) { JOptionPane.showMessageDialog(this, "Nhập mật khẩu mới vào ô Mật khẩu!"); return; }
            userDao.resetPassword(uid, pw);
            JOptionPane.showMessageDialog(this, "Đã đặt lại mật khẩu thành công.");
            reload.run();
        });

        // Search
        JTextField txtKw = new JTextField(20);
        JButton btnSearch  = new JButton("Tìm");
        JButton btnRefresh = new JButton("🔄");
        btnSearch.addActionListener(e -> sorter.setRowFilter(txtKw.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtKw.getText().trim())));
        txtKw.addActionListener(e -> btnSearch.doClick());
        btnRefresh.addActionListener(e -> { txtKw.setText(""); sorter.setRowFilter(null); reload.run(); });

        // Form panel
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(4,6,4,6); g.anchor = GridBagConstraints.WEST;
        int r = 0;
        g.gridx=0; g.gridy=r; form.add(bold("Tên đăng nhập:"), g); g.gridx=1; form.add(txtUsername, g);
        g.gridx=2; form.add(bold("Mật khẩu:"), g); g.gridx=3; form.add(txtPassword, g); r++;
        g.gridx=0; g.gridy=r; form.add(bold("Họ và tên:"), g); g.gridx=1; form.add(txtFullname, g);
        g.gridx=2; form.add(bold("Vai trò:"), g); g.gridx=3; form.add(cmbRole, g); r++;
        JPanel btnRow = new JPanel(); btnRow.add(btnAdd); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnResetPw);
        g.gridx=0; g.gridy=r; g.gridwidth=4; form.add(btnRow, g);

        panel.add(searchBar(txtKw, btnSearch, btnRefresh), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 2: NGUYÊN LIỆU
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildIngredientTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"ID", "Tên nguyên liệu", "Đơn vị", "Tồn kho", "Tối thiểu", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtName    = new JTextField(16);
        JTextField txtUnit    = new JTextField(8);
        JTextField txtQty     = new JTextField(8);
        JTextField txtMinQty  = new JTextField(8);
        JTextField txtAddQty  = new JTextField(8); // nhập thêm hàng

        Runnable reload = () -> {
            model.setRowCount(0);
            for (Ingredient ing : ingredientDao.getAllIngredients())
                model.addRow(new Object[]{
                    ing.getIngredientId(), ing.getIngredientName(), ing.getUnit(),
                    ing.getQuantity(), ing.getMinQuantity(),
                    ing.isLowStock() ? "⚠️ Sắp hết" : "✅ Đủ"
                });
        };
        reload.run();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int mr = table.convertRowIndexToModel(table.getSelectedRow());
                txtName.setText(model.getValueAt(mr, 1).toString());
                txtUnit.setText(model.getValueAt(mr, 2).toString());
                txtQty.setText(model.getValueAt(mr, 3).toString());
                txtMinQty.setText(model.getValueAt(mr, 4).toString());
                txtAddQty.setText("");
            }
        });

        JButton btnAdd     = new JButton("➕ Thêm");
        JButton btnUpdate  = new JButton("✏️ Cập nhật");
        JButton btnDelete  = new JButton("🗑️ Xóa");
        JButton btnAddStock= new JButton("📥 Nhập hàng (+)");

        btnAdd.addActionListener(e -> {
            try {
                if (txtName.getText().trim().isEmpty() || txtUnit.getText().trim().isEmpty()) throw new Exception("Thiếu thông tin");
                Ingredient ing = new Ingredient(0, txtName.getText().trim(), txtUnit.getText().trim(),
                    Double.parseDouble(txtQty.getText().trim()), Double.parseDouble(txtMinQty.getText().trim()));
                ingredientDao.addIngredient(ing); reload.run();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ và đúng định dạng!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnUpdate.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nguyên liệu!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            try {
                Ingredient ing = new Ingredient(id, txtName.getText().trim(), txtUnit.getText().trim(),
                    Double.parseDouble(txtQty.getText().trim()), Double.parseDouble(txtMinQty.getText().trim()));
                ingredientDao.updateIngredient(ing); reload.run();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Sai định dạng số!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnDelete.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) return;
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            if (JOptionPane.showConfirmDialog(this, "Xóa nguyên liệu này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                ingredientDao.deleteIngredient(id); reload.run();
            }
        });

        btnAddStock.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nguyên liệu cần nhập!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            try {
                double delta = Double.parseDouble(txtAddQty.getText().trim());
                if (delta <= 0) throw new NumberFormatException();
                ingredientDao.addStock(id, delta);
                reload.run();
                JOptionPane.showMessageDialog(this, "Đã nhập thêm " + delta + " vào kho!");
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Nhập số lượng hợp lệ vào ô 'Nhập thêm'!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        // Search
        JTextField txtKw = new JTextField(20);
        JButton btnSearch = new JButton("Tìm");
        JButton btnRefresh = new JButton("🔄");
        btnSearch.addActionListener(e -> sorter.setRowFilter(txtKw.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtKw.getText().trim())));
        txtKw.addActionListener(e -> btnSearch.doClick());
        btnRefresh.addActionListener(e -> { txtKw.setText(""); sorter.setRowFilter(null); reload.run(); });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(4,6,4,6); g.anchor = GridBagConstraints.WEST;
        int rv = 0;
        g.gridx=0; g.gridy=rv; form.add(bold("Tên nguyên liệu:"), g); g.gridx=1; form.add(txtName, g);
        g.gridx=2; form.add(bold("Đơn vị:"), g); g.gridx=3; form.add(txtUnit, g); rv++;
        g.gridx=0; g.gridy=rv; form.add(bold("Số lượng:"), g); g.gridx=1; form.add(txtQty, g);
        g.gridx=2; form.add(bold("Tối thiểu:"), g); g.gridx=3; form.add(txtMinQty, g); rv++;
        g.gridx=0; g.gridy=rv; form.add(bold("Nhập thêm:"), g); g.gridx=1; form.add(txtAddQty, g); rv++;
        JPanel btnRow = new JPanel(); btnRow.add(btnAdd); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnAddStock);
        g.gridx=0; g.gridy=rv; g.gridwidth=4; form.add(btnRow, g);

        panel.add(searchBar(txtKw, btnSearch, btnRefresh), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 3: DANH MỤC
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildCategoryTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Tên danh mục"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtName = new JTextField(24);
        Runnable reload = () -> {
            model.setRowCount(0);
            for (Category c : categoryDao.getAllCategories())
                model.addRow(new Object[]{c.getCategoryId(), c.getCategoryName()});
        };
        reload.run();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int mr = table.convertRowIndexToModel(table.getSelectedRow());
                txtName.setText(model.getValueAt(mr, 1).toString());
            }
        });

        JButton btnAdd    = new JButton("➕ Thêm");
        JButton btnUpdate = new JButton("✏️ Cập nhật");
        JButton btnDelete = new JButton("🗑️ Xóa");

        btnAdd.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Nhập tên danh mục!"); return; }
            Category c = new Category(); c.setCategoryName(txtName.getText().trim());
            categoryDao.addCategory(c); reload.run(); txtName.setText("");
        });
        btnUpdate.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn danh mục!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            Category c = new Category((int) model.getValueAt(mr, 0), txtName.getText().trim());
            categoryDao.updateCategory(c); reload.run();
        });
        btnDelete.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) return;
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            if (JOptionPane.showConfirmDialog(this, "Xóa danh mục? (Sẽ lỗi nếu còn món ăn trong danh mục)", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try { categoryDao.deleteCategory(id); reload.run(); }
                catch (Exception ex) { JOptionPane.showMessageDialog(this, "Không thể xóa: danh mục đang có món ăn!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
            }
        });

        JTextField txtKw = new JTextField(20); JButton btnSearch = new JButton("Tìm"); JButton btnRefresh = new JButton("🔄");
        btnSearch.addActionListener(e -> sorter.setRowFilter(txtKw.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtKw.getText().trim())));
        txtKw.addActionListener(e -> btnSearch.doClick());
        btnRefresh.addActionListener(e -> { txtKw.setText(""); sorter.setRowFilter(null); reload.run(); });

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(bold("Tên danh mục:")); form.add(txtName); form.add(btnAdd); form.add(btnUpdate); form.add(btnDelete);

        panel.add(searchBar(txtKw, btnSearch, btnRefresh), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 4: THỰC ĐƠN
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildMenuItemTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"ID", "Danh mục", "Tên món", "Giá (VNĐ)", "Trạng thái", "Số suất"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Form
        JComboBox<String> cmbCat  = new JComboBox<>();
        JTextField txtName        = new JTextField(16);
        JTextField txtPrice       = new JTextField(10);
        JCheckBox  chkAvail       = new JCheckBox("Có sẵn", true);
        JTextField txtAvailQty    = new JTextField("0", 6);

        // Filter combobox
        JComboBox<String> cmbFilter = new JComboBox<>();

        Runnable reloadCats = () -> {
            String sel = (String) cmbFilter.getSelectedItem();
            String selAdd = (String) cmbCat.getSelectedItem();
            cmbFilter.removeAllItems(); cmbCat.removeAllItems();
            cmbFilter.addItem("-- Tất cả --"); cmbCat.addItem("-- Chọn danh mục --");
            for (Category c : categoryDao.getAllCategories()) {
                cmbFilter.addItem(c.getCategoryId() + " - " + c.getCategoryName());
                cmbCat.addItem(c.getCategoryId() + " - " + c.getCategoryName());
            }
            if (sel != null) cmbFilter.setSelectedItem(sel);
            if (selAdd != null) cmbCat.setSelectedItem(selAdd);
        };

        Runnable reload = () -> {
            model.setRowCount(0);
            List<Category> cats = categoryDao.getAllCategories();
            String filterSel = (String) cmbFilter.getSelectedItem();
            int filterCatId = (filterSel == null || filterSel.startsWith("--")) ? -1 :
                Integer.parseInt(filterSel.split(" - ")[0].trim());
            List<MenuItem> list = (filterCatId <= 0) ? menuItemDao.getAllMenuItems()
                : menuItemDao.getMenuItemsByCategory(filterCatId);
            for (MenuItem item : list) {
                String catName = cats.stream().filter(c -> c.getCategoryId() == item.getCategoryId())
                    .map(Category::getCategoryName).findFirst().orElse("-");
                model.addRow(new Object[]{item.getItemId(), catName, item.getItemName(),
                    String.format("%,.0f", item.getPrice()),
                    item.isAvailable() ? "✅ Có sẵn" : "❌ Hết",
                    item.getAvailableQuantity()
                });
            }
        };

        reloadCats.run(); reload.run();
        cmbFilter.addActionListener(e -> reload.run());

        // Chọn dòng → điền form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int mr = table.convertRowIndexToModel(table.getSelectedRow());
                txtName.setText(model.getValueAt(mr, 2).toString());
                txtPrice.setText(model.getValueAt(mr, 3).toString().replace(",", "").trim());
                chkAvail.setSelected(model.getValueAt(mr, 4).toString().startsWith("✅"));
                txtAvailQty.setText(model.getValueAt(mr, 5).toString());
                // Tìm đúng danh mục trong combobox
                String catName = model.getValueAt(mr, 1).toString();
                for (int i = 0; i < cmbCat.getItemCount(); i++) {
                    if (cmbCat.getItemAt(i).contains(catName)) { cmbCat.setSelectedIndex(i); break; }
                }
            }
        });

        JButton btnAdd    = new JButton("➕ Thêm");
        JButton btnUpdate = new JButton("✏️ Cập nhật");
        JButton btnDelete = new JButton("🗑️ Xóa");

        btnAdd.addActionListener(e -> {
            try {
                if (cmbCat.getSelectedIndex() == 0) { JOptionPane.showMessageDialog(this, "Chọn danh mục!"); return; }
                String catSel = (String) cmbCat.getSelectedItem();
                int catId = Integer.parseInt(catSel.split(" - ")[0].trim());
                MenuItem item = new MenuItem(0, catId, txtName.getText().trim(),
                    Double.parseDouble(txtPrice.getText().trim().replace(",", "")),
                    chkAvail.isSelected(), Integer.parseInt(txtAvailQty.getText().trim()));
                menuItemDao.addMenuItem(item); reload.run();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Sai định dạng! Vui lòng kiểm tra lại.", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnUpdate.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn món cần cập nhật!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            try {
                if (cmbCat.getSelectedIndex() == 0) { JOptionPane.showMessageDialog(this, "Chọn danh mục!"); return; }
                String catSel = (String) cmbCat.getSelectedItem();
                int catId = Integer.parseInt(catSel.split(" - ")[0].trim());
                MenuItem item = new MenuItem(id, catId, txtName.getText().trim(),
                    Double.parseDouble(txtPrice.getText().trim().replace(",", "")),
                    chkAvail.isSelected(), Integer.parseInt(txtAvailQty.getText().trim()));
                menuItemDao.updateMenuItem(item); reload.run();
                JOptionPane.showMessageDialog(this, "✅ Đã cập nhật món ăn!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Sai định dạng!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnDelete.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) return;
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            if (JOptionPane.showConfirmDialog(this, "Xóa món này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                menuItemDao.deleteMenuItem(id); reload.run();
            }
        });

        // Search + filter
        JTextField txtKw = new JTextField(16); JButton btnSearch = new JButton("Tìm"); JButton btnRefresh = new JButton("🔄");
        btnSearch.addActionListener(ev -> sorter.setRowFilter(txtKw.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtKw.getText().trim())));
        txtKw.addActionListener(ev -> btnSearch.doClick());
        btnRefresh.addActionListener(ev -> { txtKw.setText(""); sorter.setRowFilter(null); reloadCats.run(); reload.run(); });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("📁 Lọc danh mục:")); topBar.add(cmbFilter);
        topBar.add(new JLabel("🔍:")); topBar.add(txtKw); topBar.add(btnSearch); topBar.add(btnRefresh);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(4,6,4,6); g.anchor = GridBagConstraints.WEST;
        int rv = 0;
        g.gridx=0; g.gridy=rv; form.add(bold("Danh mục:"), g); g.gridx=1; form.add(cmbCat, g);
        g.gridx=2; form.add(bold("Tên món:"), g); g.gridx=3; form.add(txtName, g); rv++;
        g.gridx=0; g.gridy=rv; form.add(bold("Giá (VNĐ):"), g); g.gridx=1; form.add(txtPrice, g);
        g.gridx=2; form.add(chkAvail, g); g.gridx=3; form.add(new JLabel("Số suất:"), g); rv++;
        g.gridx=3; g.gridy=rv-1; form.add(txtAvailQty, g); rv++;
        JPanel btnRow = new JPanel(); btnRow.add(btnAdd); btnRow.add(btnUpdate); btnRow.add(btnDelete);
        g.gridx=0; g.gridy=rv; g.gridwidth=4; form.add(btnRow, g);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 5: BÀN ĂN
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildTableTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Tên bàn", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtName = new JTextField(16);
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Empty", "Occupied"});

        Runnable reload = () -> {
            model.setRowCount(0);
            for (DiningTable t : tableDao.getAllTables())
                model.addRow(new Object[]{t.getTableId(), t.getTableName(), t.getStatus()});
        };
        reload.run();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int mr = table.convertRowIndexToModel(table.getSelectedRow());
                txtName.setText(model.getValueAt(mr, 1).toString());
                cmbStatus.setSelectedItem(model.getValueAt(mr, 2).toString());
            }
        });

        JButton btnAdd    = new JButton("➕ Thêm");
        JButton btnUpdate = new JButton("✏️ Cập nhật");
        JButton btnDelete = new JButton("🗑️ Xóa");

        btnAdd.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Nhập tên bàn!"); return; }
            DiningTable t = new DiningTable(0, txtName.getText().trim(), "Empty");
            tableDao.addTable(t); reload.run(); txtName.setText("");
        });
        btnUpdate.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn bàn cần cập nhật!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            tableDao.updateTableStatus(id, cmbStatus.getSelectedItem().toString());
            // Cập nhật tên bàn
            String sql2 = "UPDATE DiningTables SET table_name=? WHERE table_id=?";
            try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection();
                 java.sql.PreparedStatement p = c.prepareStatement(sql2)) {
                p.setString(1, txtName.getText().trim()); p.setInt(2, id); p.executeUpdate();
            } catch (Exception ex) { ex.printStackTrace(); }
            reload.run();
        });
        btnDelete.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) return;
            int mr = table.convertRowIndexToModel(sr);
            int id = (int) model.getValueAt(mr, 0);
            if (JOptionPane.showConfirmDialog(this, "Xóa bàn này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String sql2 = "DELETE FROM DiningTables WHERE table_id=?";
                try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection();
                     java.sql.PreparedStatement p = c.prepareStatement(sql2)) {
                    p.setInt(1, id); p.executeUpdate();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Không thể xóa bàn đang có đơn hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
                reload.run();
            }
        });

        JTextField txtKw = new JTextField(16); JButton btnSearch = new JButton("Tìm"); JButton btnRefresh = new JButton("🔄");
        JComboBox<String> cmbStatusFilter = new JComboBox<>(new String[]{"-- Tất cả --", "Empty", "Occupied"});
        btnSearch.addActionListener(ev -> {
            String kw = txtKw.getText().trim(); String st = (String) cmbStatusFilter.getSelectedItem();
            RowFilter<DefaultTableModel,Object> rf1 = kw.isEmpty() ? null : RowFilter.regexFilter("(?i)" + kw, 1);
            RowFilter<DefaultTableModel,Object> rf2 = st.startsWith("--") ? null : RowFilter.regexFilter("(?i)" + st, 2);
            if (rf1==null && rf2==null) sorter.setRowFilter(null);
            else if (rf1==null) sorter.setRowFilter(rf2);
            else if (rf2==null) sorter.setRowFilter(rf1);
            else sorter.setRowFilter(RowFilter.andFilter(java.util.Arrays.asList(rf1, rf2)));
        });
        txtKw.addActionListener(ev -> btnSearch.doClick()); cmbStatusFilter.addActionListener(ev -> btnSearch.doClick());
        btnRefresh.addActionListener(ev -> { txtKw.setText(""); cmbStatusFilter.setSelectedIndex(0); sorter.setRowFilter(null); reload.run(); });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("🔍:")); topBar.add(txtKw); topBar.add(new JLabel("Trạng thái:")); topBar.add(cmbStatusFilter);
        topBar.add(btnSearch); topBar.add(btnRefresh);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(bold("Tên bàn:")); form.add(txtName);
        form.add(bold("Trạng thái:")); form.add(cmbStatus);
        form.add(btnAdd); form.add(btnUpdate); form.add(btnDelete);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }
}
