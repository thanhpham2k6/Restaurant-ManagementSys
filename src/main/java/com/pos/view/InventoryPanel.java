package com.pos.view;

import com.pos.controller.StockController;
import com.pos.controller.StockObserver;
import com.pos.dao.IngredientDAO;
import com.pos.dao.MenuDAO;
import com.pos.dao.RecipeDAO;
import com.pos.model.*;
import com.pos.model.MenuItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class InventoryPanel extends JPanel implements StockObserver {
    private final User currentUser;
    private final StockController stockController;
    private final IngredientDAO ingredientDAO = new IngredientDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();

    private DefaultTableModel ingredientModel;
    private JTable ingredientTable;
    private JLabel lblLowStock;

    public InventoryPanel(User currentUser, StockController stockController) {
        this.currentUser = currentUser;
        this.stockController = stockController;
        setLayout(new BorderLayout());
        initUI();
        loadIngredients();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(10, 16, 10, 16));
        header.setBackground(new Color(28, 30, 42));

        JLabel title = new JLabel("QUẢN LÝ TỒN KHO & ĐỊNH LƯỢNG NGUYÊN LIỆU");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(255, 193, 7));
        header.add(title, BorderLayout.WEST);

        lblLowStock = new JLabel();
        lblLowStock.setForeground(new Color(239, 83, 80));
        lblLowStock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.add(lblLowStock, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table
        ingredientModel = new DefaultTableModel(
            new Object[]{"ID", "Tên nguyên liệu", "Đơn vị tính", "Số lượng tồn", "Ngưỡng cảnh báo", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ingredientTable = new JTable(ingredientModel);
        ingredientTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ingredientTable.setRowHeight(28);
        ingredientTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        ingredientTable.getColumnModel().getColumn(0).setMaxWidth(0);
        ingredientTable.getColumnModel().getColumn(0).setMinWidth(0);
        ingredientTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        add(new JScrollPane(ingredientTable), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton btnImport = new JButton("Nhập kho");
        btnImport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnImport.setBackground(new Color(46, 125, 50));
        btnImport.setForeground(Color.WHITE);
        btnImport.setFocusPainted(false);
        btnImport.addActionListener(e -> importStock());

        JButton btnAdjust = new JButton("Kiểm kê / Điều chỉnh");
        btnAdjust.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAdjust.addActionListener(e -> adjustStock());

        JButton btnAdd = new JButton("Thêm nguyên liệu mới");
        btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAdd.addActionListener(e -> addIngredient());

        JButton btnRecipe = new JButton("Định lượng món ăn (Recipe)");
        btnRecipe.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRecipe.setBackground(new Color(103, 58, 183));
        btnRecipe.setForeground(Color.WHITE);
        btnRecipe.setFocusPainted(false);
        btnRecipe.addActionListener(e -> openRecipeDialog());

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> loadIngredients());

        btnPanel.add(btnImport);
        btnPanel.add(btnAdjust);
        if (currentUser.getRole() == Role.MANAGER) {
            btnPanel.add(btnAdd);
            btnPanel.add(btnRecipe);
        }
        btnPanel.add(btnRefresh);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadIngredients() {
        ingredientModel.setRowCount(0);
        List<Ingredient> list = stockController.getAllIngredients();
        int lowCount = 0;
        for (Ingredient ing : list) {
            boolean isLow = ing.isLowStock();
            String status = isLow ? "SẮP HẾT HÀNG!" : "Đầy đủ";
            if (isLow) lowCount++;
            ingredientModel.addRow(new Object[]{
                ing.getId(),
                ing.getName(),
                ing.getUnit(),
                ing.getStockQty().stripTrailingZeros().toPlainString(),
                ing.getMinQty().stripTrailingZeros().toPlainString(),
                status
            });
        }
        lblLowStock.setText(lowCount > 0 ? "Cảnh báo: Có " + lowCount + " nguyên liệu sắp hết!" : "Kho hàng ổn định");
    }

    private void importStock() {
        int row = ingredientTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 nguyên liệu cần nhập kho!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) ingredientModel.getValueAt(row, 0);
        String name = (String) ingredientModel.getValueAt(row, 1);
        String unit = (String) ingredientModel.getValueAt(row, 2);

        String qtyStr = JOptionPane.showInputDialog(this, "Nhập số lượng bổ sung (" + name + " - " + unit + "):", "0");
        if (qtyStr == null) return;
        try {
            BigDecimal qty = new BigDecimal(qtyStr.trim());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            String note = JOptionPane.showInputDialog(this, "Ghi chú nhập kho:", "Nhập hàng từ nhà cung cấp");
            stockController.importStock(id, qty, currentUser, note);
            loadIngredients();
            JOptionPane.showMessageDialog(this, "Đã nhập thêm " + qty + " " + unit + " vào kho cho '" + name + "'!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adjustStock() {
        int row = ingredientTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nguyên liệu cần điều chỉnh!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) ingredientModel.getValueAt(row, 0);
        String name = (String) ingredientModel.getValueAt(row, 1);
        String currentQty = (String) ingredientModel.getValueAt(row, 3);

        String qtyStr = JOptionPane.showInputDialog(this,
            "Nhập số lượng thực tế trong kho (" + name + ") - Hiện tại: " + currentQty + ":", currentQty);
        if (qtyStr == null) return;
        try {
            BigDecimal qty = new BigDecimal(qtyStr.trim());
            if (qty.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            String note = JOptionPane.showInputDialog(this, "Lý do kiểm kê / điều chỉnh:", "Kiểm kê định kỳ");
            stockController.adjustStock(id, qty, currentUser, note);
            loadIngredients();
            JOptionPane.showMessageDialog(this, "Đã cập nhật tồn kho '" + name + "' thành " + qty);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addIngredient() {
        JTextField tfName = new JTextField(20);
        JTextField tfUnit = new JTextField(10);
        JTextField tfMin  = new JTextField("10", 10);

        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.add(new JLabel("Tên nguyên liệu:")); form.add(tfName);
        form.add(new JLabel("Đơn vị (g, ml, cái...):")); form.add(tfUnit);
        form.add(new JLabel("Ngưỡng cảnh báo:")); form.add(tfMin);

        int result = JOptionPane.showConfirmDialog(this, form, "Thêm nguyên liệu mới vào kho", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            String name = tfName.getText().trim();
            String unit = tfUnit.getText().trim();
            if (name.isEmpty() || unit.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên và đơn vị tính không được để trống!");
                return;
            }
            Ingredient ing = new Ingredient();
            ing.setName(name);
            ing.setUnit(unit);
            ing.setStockQty(BigDecimal.ZERO);
            ing.setMinQty(new BigDecimal(tfMin.getText().trim()));
            ing.setActive(true);
            ingredientDAO.insert(ing);
            loadIngredients();
            JOptionPane.showMessageDialog(this, "Đã thêm nguyên liệu: " + ing.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi thêm nguyên liệu: " + e.getMessage());
        }
    }

    private void openRecipeDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Quản lý công thức định lượng (Recipe)", true);
        dialog.setSize(680, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<MenuItem> cbMenuItems = new JComboBox<>();
        menuDAO.findAll().forEach(cbMenuItems::addItem);
        top.add(new JLabel("Chọn món ăn:"));
        top.add(cbMenuItems);
        dialog.add(top, BorderLayout.NORTH);

        DefaultTableModel recipeModel = new DefaultTableModel(new Object[]{"ID", "Nguyên liệu", "Đơn vị", "Định lượng / 1 suất"}, 0);
        JTable recipeTable = new JTable(recipeModel);
        recipeTable.getColumnModel().getColumn(0).setMaxWidth(0);
        recipeTable.getColumnModel().getColumn(0).setMinWidth(0);
        recipeTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        dialog.add(new JScrollPane(recipeTable), BorderLayout.CENTER);

        Runnable loadRecipe = () -> {
            recipeModel.setRowCount(0);
            MenuItem sel = (MenuItem) cbMenuItems.getSelectedItem();
            if (sel != null) {
                List<RecipeLine> lines = recipeDAO.findByMenuItem(sel.getId());
                for (RecipeLine line : lines) {
                    recipeModel.addRow(new Object[]{
                        line.getId(),
                        line.getIngredient().getName(),
                        line.getIngredient().getUnit(),
                        line.getQtyPerServing().stripTrailingZeros().toPlainString()
                    });
                }
            }
        };

        cbMenuItems.addActionListener(e -> loadRecipe.run());
        loadRecipe.run();

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAddIng = new JButton("Gán nguyên liệu cho món");
        btnAddIng.addActionListener(e -> {
            MenuItem selItem = (MenuItem) cbMenuItems.getSelectedItem();
            if (selItem == null) return;
            List<Ingredient> ingredients = stockController.getAllIngredients();
            if (ingredients.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Chưa có nguyên liệu nào trong kho!");
                return;
            }
            JComboBox<Ingredient> cbIng = new JComboBox<>(ingredients.toArray(new Ingredient[0]));
            JTextField tfQty = new JTextField("100", 10);
            JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
            p.add(new JLabel("Nguyên liệu:")); p.add(cbIng);
            p.add(new JLabel("Định lượng 1 suất:")); p.add(tfQty);
            int opt = JOptionPane.showConfirmDialog(dialog, p, "Gán định lượng cho " + selItem.getName(), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                try {
                    Ingredient selIng = (Ingredient) cbIng.getSelectedItem();
                    BigDecimal qty = new BigDecimal(tfQty.getText().trim());
                    recipeDAO.upsert(selItem.getId(), selIng.getId(), qty);
                    loadRecipe.run();
                    JOptionPane.showMessageDialog(dialog, "Đã lưu công thức!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage());
                }
            }
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dialog.dispose());

        bot.add(btnAddIng);
        bot.add(btnClose);
        dialog.add(bot, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    @Override
    public void onStockChanged(int menuItemId) {
        SwingUtilities.invokeLater(this::loadIngredients);
    }
}
