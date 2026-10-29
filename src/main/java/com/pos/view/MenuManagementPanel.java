package com.pos.view;

import com.pos.dao.CategoryDAO;
import com.pos.dao.MenuDAO;
import com.pos.model.Category;
import com.pos.model.CategoryType;
import com.pos.model.MenuItem;
import com.pos.model.User;
import com.pos.utils.CurrencyFormat;
import com.pos.utils.DishImageManager;
import com.pos.utils.TableUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class MenuManagementPanel extends JPanel {
    private final User currentUser;
    private final MenuDAO menuDAO = new MenuDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // Dish tab
    private JTable dishTable;
    private DefaultTableModel dishModel;
    private JComboBox<String> cbCategoryFilter;
    private JComboBox<String> cbStatusFilter;
    private JTextField txtSearchDish;
    private List<MenuItem> menuItemsList = new ArrayList<>();
    private List<Category> categoriesList = new ArrayList<>();

    // Category tab
    private JTable catTable;
    private DefaultTableModel catModel;

    public MenuManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        initComponents();
        loadAllDataAsync();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("Danh sách Món ăn & Đồ uống", createDishManagementPanel());
        tabbedPane.addTab("Quản lý Danh mục Thực đơn", createCategoryManagementPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== TAB 1: DISH MANAGEMENT ====================
    private JPanel createDishManagementPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(new Color(245, 247, 250));
        main.setBorder(new EmptyBorder(16, 20, 20, 20));

        // Top Header
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("QUẢN LÝ THỰC ĐƠN NHÀ HÀNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSub = new JLabel("Thêm, chỉnh sửa món ăn, điều chỉnh giá bán và cập nhật trạng thái còn/hết món");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        topHeader.add(titleBox, BorderLayout.WEST);

        // Action Buttons
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnAddDish = new JButton("+ Thêm Món Mới");
        btnAddDish.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddDish.setBackground(new Color(16, 185, 129));
        btnAddDish.setForeground(Color.WHITE);
        btnAddDish.setFocusPainted(false);
        btnAddDish.addActionListener(e -> showAddEditDishDialog(null));

        JButton btnEditDish = new JButton("Sửa Món");
        btnEditDish.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnEditDish.setBackground(new Color(59, 130, 246));
        btnEditDish.setForeground(Color.WHITE);
        btnEditDish.setFocusPainted(false);
        btnEditDish.addActionListener(e -> editSelectedDish());

        JButton btnToggleStatus = new JButton("⚡ Bật/Tắt Hết Món");
        btnToggleStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnToggleStatus.setBackground(new Color(245, 158, 11));
        btnToggleStatus.setForeground(Color.WHITE);
        btnToggleStatus.setFocusPainted(false);
        btnToggleStatus.addActionListener(e -> toggleDishAvailability());

        JButton btnDeleteDish = new JButton("Xóa Món");
        btnDeleteDish.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteDish.setBackground(new Color(239, 68, 68));
        btnDeleteDish.setForeground(Color.WHITE);
        btnDeleteDish.setFocusPainted(false);
        btnDeleteDish.addActionListener(e -> deleteSelectedDish());

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setBackground(new Color(238, 242, 246));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadAllDataAsync());

        actionBox.add(btnAddDish);
        actionBox.add(btnEditDish);
        actionBox.add(btnToggleStatus);
        actionBox.add(btnDeleteDish);
        actionBox.add(btnRefresh);
        topHeader.add(actionBox, BorderLayout.EAST);

        main.add(topHeader, BorderLayout.NORTH);

        // Center: Card containing Toolbar + Table
        JPanel tableCard = new JPanel(new BorderLayout(0, 12));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(14, 16, 16, 16)
        ));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.setOpaque(false);

        filterBar.add(new JLabel("Danh mục:"));
        cbCategoryFilter = new JComboBox<>(new String[]{"Tất cả danh mục"});
        cbCategoryFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbCategoryFilter.addActionListener(e -> filterDishTable());
        filterBar.add(cbCategoryFilter);

        filterBar.add(new JLabel("Trạng thái:"));
        cbStatusFilter = new JComboBox<>(new String[]{"Tất cả", "Chỉ món Còn", "Chỉ món Hết"});
        cbStatusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbStatusFilter.addActionListener(e -> filterDishTable());
        filterBar.add(cbStatusFilter);

        filterBar.add(new JLabel("Tìm kiếm món:"));
        txtSearchDish = new JTextField(16);
        txtSearchDish.putClientProperty("JTextField.placeholderText", "Tên món hoặc mã...");
        txtSearchDish.addActionListener(e -> filterDishTable());
        filterBar.add(txtSearchDish);

        JButton btnSearch = new JButton("Tìm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSearch.addActionListener(e -> filterDishTable());
        filterBar.add(btnSearch);

        tableCard.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Hình ảnh", "Tên món ăn / Đồ uống", "Danh mục", "Đơn giá (VND)", "Trạng thái phục vụ"};
        dishModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0) return Integer.class;
                if (col == 1) return ImageIcon.class;
                return String.class;
            }
        };

        dishTable = new JTable(dishModel);
        dishTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dishTable.setRowHeight(52);
        dishTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        dishTable.getTableHeader().setBackground(new Color(248, 250, 252));
        dishTable.getColumnModel().getColumn(0).setMaxWidth(60);
        dishTable.getColumnModel().getColumn(1).setMaxWidth(64);
        TableUtils.applySmartSorting(dishTable);

        // Alignments & Styles
        DefaultTableCellRenderer priceRenderer = new DefaultTableCellRenderer();
        priceRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        dishTable.getColumnModel().getColumn(4).setCellRenderer(priceRenderer);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if (val.contains("Còn món")) {
                    l.setForeground(new Color(16, 185, 129));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else {
                    l.setForeground(new Color(239, 68, 68));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                }
                return l;
            }
        };
        dishTable.getColumnModel().getColumn(5).setCellRenderer(statusRenderer);

        JScrollPane sp = new JScrollPane(dishTable);
        sp.setBorder(new LineBorder(new Color(226, 232, 240), 1));
        tableCard.add(sp, BorderLayout.CENTER);

        main.add(tableCard, BorderLayout.CENTER);
        return main;
    }

    // ==================== TAB 2: CATEGORY MANAGEMENT ====================
    private JPanel createCategoryManagementPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(new Color(245, 247, 250));
        main.setBorder(new EmptyBorder(16, 20, 20, 20));

        // Header
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("DANH MỤC THỰC ĐƠN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));
        JLabel lblSub = new JLabel("Quản lý các nhóm món ăn, thức uống và tráng miệng của nhà hàng");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        topHeader.add(titleBox, BorderLayout.WEST);

        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnAddCat = new JButton("+ Thêm Danh Mục");
        btnAddCat.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddCat.setBackground(new Color(16, 185, 129));
        btnAddCat.setForeground(Color.WHITE);
        btnAddCat.setFocusPainted(false);
        btnAddCat.addActionListener(e -> showAddEditCategoryDialog(null));

        JButton btnEditCat = new JButton("Sửa Danh Mục");
        btnEditCat.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnEditCat.setBackground(new Color(59, 130, 246));
        btnEditCat.setForeground(Color.WHITE);
        btnEditCat.setFocusPainted(false);
        btnEditCat.addActionListener(e -> editSelectedCategory());

        JButton btnDeleteCat = new JButton("Xóa Danh Mục");
        btnDeleteCat.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteCat.setBackground(new Color(239, 68, 68));
        btnDeleteCat.setForeground(Color.WHITE);
        btnDeleteCat.setFocusPainted(false);
        btnDeleteCat.addActionListener(e -> deleteSelectedCategory());

        actionBox.add(btnAddCat);
        actionBox.add(btnEditCat);
        actionBox.add(btnDeleteCat);
        topHeader.add(actionBox, BorderLayout.EAST);

        main.add(topHeader, BorderLayout.NORTH);

        // Center Table
        JPanel tableCard = new JPanel(new BorderLayout(0, 12));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(14, 16, 16, 16)
        ));

        String[] cols = {"ID", "Tên Danh Mục", "Phân Loại Kỹ Thuật", "Số Lượng Món Đang Có"};
        catModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0 || col == 3) return Integer.class;
                return String.class;
            }
        };

        catTable = new JTable(catModel);
        catTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        catTable.setRowHeight(36);
        catTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        catTable.getTableHeader().setBackground(new Color(248, 250, 252));
        TableUtils.applySmartSorting(catTable);

        JScrollPane sp = new JScrollPane(catTable);
        sp.setBorder(new LineBorder(new Color(226, 232, 240), 1));
        tableCard.add(sp, BorderLayout.CENTER);

        main.add(tableCard, BorderLayout.CENTER);
        return main;
    }

    // ==================== DATA LOADING ====================
    public void loadAllDataAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                categoriesList = categoryDAO.findAll();
                menuItemsList = menuDAO.findAll();
                return null;
            }

            @Override
            protected void done() {
                // Update category filter combo
                if (cbCategoryFilter != null) {
                    cbCategoryFilter.removeAllItems();
                    cbCategoryFilter.addItem("Tất cả danh mục");
                    for (Category c : categoriesList) {
                        cbCategoryFilter.addItem(c.getName());
                    }
                }

                filterDishTable();
                refreshCategoryTable();
            }
        };
        worker.execute();
    }

    private void filterDishTable() {
        dishModel.setRowCount(0);
        String selCat = cbCategoryFilter.getSelectedItem() != null ? (String) cbCategoryFilter.getSelectedItem() : "Tất cả danh mục";
        String selStatus = cbStatusFilter.getSelectedItem() != null ? (String) cbStatusFilter.getSelectedItem() : "Tất cả";
        String search = txtSearchDish.getText().trim().toLowerCase();

        for (MenuItem item : menuItemsList) {
            if (!selCat.equals("Tất cả danh mục") && item.getCategory() != null && !item.getCategory().getName().equalsIgnoreCase(selCat)) {
                continue;
            }
            if (selStatus.contains("Còn") && !item.isAvailable()) continue;
            if (selStatus.contains("Hết") && item.isAvailable()) continue;

            if (!search.isEmpty()) {
                String match = (item.getId() + " " + item.getName() + " " + (item.getCategory() != null ? item.getCategory().getName() : "")).toLowerCase();
                if (!match.contains(search)) continue;
            }

            ImageIcon thumb = DishImageManager.getDishIcon(
                item.getId(),
                item.getImagePath(),
                item.getCategory() != null ? item.getCategory().getName() : "",
                item.getName(),
                44, 44, 8
            );

            dishModel.addRow(new Object[]{
                item.getId(),
                thumb,
                item.getName(),
                item.getCategory() != null ? item.getCategory().getName() : "Khác",
                CurrencyFormat.format(item.getPrice()),
                item.isAvailable() ? "● Còn món" : "○ Tạm hết món"
            });
        }
    }

    private void refreshCategoryTable() {
        catModel.setRowCount(0);
        for (Category c : categoriesList) {
            long dishCount = menuItemsList.stream().filter(m -> m.getCategory() != null && m.getCategory().getId() == c.getId()).count();
            catModel.addRow(new Object[]{
                c.getId(),
                c.getName(),
                c.getType().name(),
                (int) dishCount
            });
        }
    }

    // ==================== DISH ACTIONS ====================
    private void showAddEditDishDialog(MenuItem existing) {
        boolean isEdit = existing != null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "CHỈNH SỬA MÓN ĂN" : "THÊM MÓN ĂN MỚI", true);
        dlg.setSize(500, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 14));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField txtName = new JTextField(isEdit ? existing.getName() : "");
        JTextField txtPrice = new JTextField(isEdit && existing.getPrice() != null ? existing.getPrice().toPlainString() : "");

        JComboBox<Category> cbCat = new JComboBox<>(categoriesList.toArray(new Category[0]));
        cbCat.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category c) {
                    l.setText(c.getName() + " (" + c.getType() + ")");
                }
                return l;
            }
        });

        if (isEdit && existing.getCategory() != null) {
            for (int i = 0; i < categoriesList.size(); i++) {
                if (categoriesList.get(i).getId() == existing.getCategory().getId()) {
                    cbCat.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Image selector
        JTextField txtImagePath = new JTextField(isEdit && existing.getImagePath() != null ? existing.getImagePath() : "");
        JLabel lblPreview = new JLabel();
        lblPreview.setPreferredSize(new Dimension(64, 64));
        lblPreview.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);
        if (isEdit) {
            lblPreview.setIcon(DishImageManager.getDishIcon(existing.getId(), existing.getImagePath(),
                existing.getCategory() != null ? existing.getCategory().getName() : "", existing.getName(), 60, 60, 8));
        }

        JButton btnBrowseImg = new JButton("Chọn ảnh...");
        btnBrowseImg.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Ảnh món ăn (JPG, PNG)", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                File sel = fc.getSelectedFile();
                txtImagePath.setText(sel.getName());
                // Auto copy to images/dishes AND src/main/resources/images/dishes
                try {
                    File targetDir1 = new File("images/dishes");
                    if (!targetDir1.exists()) targetDir1.mkdirs();
                    File dest1 = new File(targetDir1, sel.getName());
                    Files.copy(sel.toPath(), dest1.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    File targetDir2 = new File("src/main/resources/images/dishes");
                    if (!targetDir2.exists()) targetDir2.mkdirs();
                    File dest2 = new File(targetDir2, sel.getName());
                    Files.copy(sel.toPath(), dest2.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Load preview immediately from selected file
                    BufferedImage bimg = ImageIO.read(sel);
                    if (bimg != null) {
                        Image scaled = bimg.getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                        lblPreview.setIcon(new ImageIcon(scaled));
                    }
                } catch (Exception ex) {
                    try {
                        BufferedImage bimg = ImageIO.read(sel);
                        if (bimg != null) {
                            Image scaled = bimg.getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                            lblPreview.setIcon(new ImageIcon(scaled));
                        }
                    } catch (Exception ignored) {}
                }
            }
        });

        JPanel pImg = new JPanel(new BorderLayout(8, 0));
        pImg.add(lblPreview, BorderLayout.WEST);
        pImg.add(txtImagePath, BorderLayout.CENTER);
        pImg.add(btnBrowseImg, BorderLayout.EAST);

        JCheckBox chkAvailable = new JCheckBox("Còn món / Phục vụ ngay hôm nay", isEdit ? existing.isAvailable() : true);

        form.add(new JLabel("Tên món ăn:"));
        form.add(txtName);
        form.add(new JLabel("Danh mục:"));
        form.add(cbCat);
        form.add(new JLabel("Đơn giá (VND):"));
        form.add(txtPrice);
        form.add(new JLabel("Hình ảnh món:"));
        form.add(pImg);
        form.add(new JLabel("Trạng thái:"));
        form.add(chkAvailable);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnSave = new JButton(isEdit ? "Cập Nhật Món" : "Thêm Vào Thực Đơn");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String priceStr = txtPrice.getText().trim().replaceAll("[^0-9]", "");
            Category cat = (Category) cbCat.getSelectedItem();

            if (name.isEmpty() || priceStr.isEmpty() || cat == null) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập tên món, danh mục và đơn giá!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BigDecimal price = new BigDecimal(priceStr);
            String img = txtImagePath.getText().trim();
            boolean isAvail = chkAvailable.isSelected();

            if (isEdit) {
                existing.setName(name);
                existing.setCategory(cat);
                existing.setPrice(price);
                existing.setImagePath(img);
                existing.setAvailable(isAvail);
                menuDAO.update(existing);
                JOptionPane.showMessageDialog(dlg, "Đã cập nhật món thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                MenuItem item = new MenuItem(0, cat, name, price, isAvail, 0);
                item.setImagePath(img);
                menuDAO.insert(item);
                JOptionPane.showMessageDialog(dlg, "Đã thêm món mới vào thực đơn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
            DishImageManager.clearCache();
            dlg.dispose();
            loadAllDataAsync();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void editSelectedDish() {
        int row = dishTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món ăn cần sửa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) dishTable.getValueAt(row, 0);
        MenuItem item = menuDAO.findById(id);
        if (item != null) {
            showAddEditDishDialog(item);
        }
    }

    private void toggleDishAvailability() {
        int row = dishTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món ăn để bật/tắt trạng thái Còn/Hết!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) dishTable.getValueAt(row, 0);
        MenuItem item = menuDAO.findById(id);
        if (item != null) {
            boolean nextState = !item.isAvailable();
            item.setAvailable(nextState);
            menuDAO.update(item);
            loadAllDataAsync();
            JOptionPane.showMessageDialog(this, "Đã đổi trạng thái món [" + item.getName() + "] thành: " + (nextState ? "CÒN MÓN" : "TẠM HẾT MÓN"), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteSelectedDish() {
        int row = dishTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món ăn cần xóa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) dishTable.getValueAt(row, 0);
        String name = (String) dishTable.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa món [" + name + "] khỏi thực đơn không?",
            "Xác nhận xóa món",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            menuDAO.delete(id);
            loadAllDataAsync();
            JOptionPane.showMessageDialog(this, "Đã xóa món thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================== CATEGORY ACTIONS ====================
    private void showAddEditCategoryDialog(Category existing) {
        boolean isEdit = existing != null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "SỬA DANH MỤC" : "THÊM DANH MỤC MỚI", true);
        dlg.setSize(400, 260);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 14));
        form.setBorder(new EmptyBorder(25, 20, 20, 20));

        JTextField txtName = new JTextField(isEdit ? existing.getName() : "");
        JComboBox<CategoryType> cbType = new JComboBox<>(CategoryType.values());
        if (isEdit) cbType.setSelectedItem(existing.getType());

        form.add(new JLabel("Tên danh mục:"));
        form.add(txtName);
        form.add(new JLabel("Phân loại:"));
        form.add(cbType);

        dlg.add(form, BorderLayout.CENTER);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = new JButton(isEdit ? "Cập Nhật" : "Lưu Danh Mục");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập tên danh mục!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            CategoryType type = (CategoryType) cbType.getSelectedItem();
            if (isEdit) {
                existing.setName(name);
                existing.setType(type);
                categoryDAO.update(existing);
            } else {
                Category c = new Category(0, name, type);
                categoryDAO.insert(c);
            }
            DishImageManager.clearCache();
            dlg.dispose();
            loadAllDataAsync();
        });

        pBtn.add(btnCancel);
        pBtn.add(btnSave);
        dlg.add(pBtn, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void editSelectedCategory() {
        int row = catTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn danh mục cần sửa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) catTable.getValueAt(row, 0);
        for (Category c : categoriesList) {
            if (c.getId() == id) {
                showAddEditCategoryDialog(c);
                break;
            }
        }
    }

    private void deleteSelectedCategory() {
        int row = catTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn danh mục cần xóa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) catTable.getValueAt(row, 0);
        String name = (String) catTable.getValueAt(row, 1);
        int count = (int) catTable.getValueAt(row, 3);

        if (count > 0) {
            JOptionPane.showMessageDialog(this, "Không thể xóa danh mục [" + name + "] vì đang có " + count + " món ăn thuộc danh mục này!\nHãy chuyển hoặc xóa các món ăn trước.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa danh mục [" + name + "] không?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            categoryDAO.delete(id);
            loadAllDataAsync();
        }
    }
}
