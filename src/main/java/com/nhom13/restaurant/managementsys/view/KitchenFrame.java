package com.nhom13.restaurant.managementsys.view;

import com.nhom13.restaurant.managementsys.dao.*;
import com.nhom13.restaurant.managementsys.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Màn hình Nhà Bếp (Kitchen):
 *  Tab 1 – 🔥 Đơn cần nấu   : xem đơn Pending → Bắt đầu nấu → Hoàn thành
 *                              Khi "Hoàn thành": tự động trừ nguyên liệu kho theo công thức
 *  Tab 2 – 🍽️ Số suất hôm nay: đặt available_quantity cho từng món
 *  Tab 3 – 📦 Kho nguyên liệu: xem tồn kho, nhập/xuất hàng
 *
 *  Bảng MenuItemIngredients (item_id, ingredient_id, quantity_needed) định nghĩa
 *  công thức: khi 1 suất được phục vụ, nhà bếp trừ đúng lượng nguyên liệu.
 */
public class KitchenFrame extends JFrame {

    private final OrderDao       orderDao       = new OrderDao();
    private final OrderDetailDao detailDao      = new OrderDetailDao();
    private final MenuItemDao    menuItemDao    = new MenuItemDao();
    private final CategoryDao    categoryDao    = new CategoryDao();
    private final IngredientDao  ingredientDao  = new IngredientDao();
    private final DiningTableDao tableDao       = new DiningTableDao();

    // Tab 1 – orders
    private DefaultTableModel orderModel;
    private DefaultTableModel detailModel;
    private int selectedOrderId = -1;

    private javax.swing.Timer syncTimer;

    public KitchenFrame() {
        setTitle("Restaurant Management – Nhà Bếp");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("🔥 Đơn cần nấu",    buildOrderTab());
        tabs.addTab("🍽️ Số suất hôm nay", buildAvailabilityTab());
        tabs.addTab("📦 Kho nguyên liệu", buildIngredientTab());
        getContentPane().add(tabs);

        startSyncTimer();
        ensureIngredientsAndRecipes(); // Đảm bảo có nguyên liệu + công thức mẫu
    }

    private void startSyncTimer() {
        syncTimer = new javax.swing.Timer(5000, e -> refreshOrders());
        syncTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  KHỞI TẠO DỮ LIỆU NGUYÊN LIỆU & CÔNG THỨC MẪU
    // ═══════════════════════════════════════════════════════════════
    /**
     * Tạo bảng MenuItemIngredients nếu chưa có, insert nguyên liệu mẫu
     * và công thức nấu ăn cơ bản theo logic nhà bếp chuyên nghiệp.
     */
    private void ensureIngredientsAndRecipes() {
        String createRecipeTable =
            "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='MenuItemIngredients' AND xtype='U') " +
            "CREATE TABLE MenuItemIngredients (" +
            "  id INT IDENTITY PRIMARY KEY," +
            "  item_id INT NOT NULL REFERENCES MenuItems(item_id) ON DELETE CASCADE," +
            "  ingredient_id INT NOT NULL REFERENCES Ingredients(ingredient_id) ON DELETE CASCADE," +
            "  quantity_needed DECIMAL(10,3) NOT NULL DEFAULT 1" +
            ")";

        String insertIngredients =
            // Bổ sung nguyên liệu nếu chưa có (dùng MERGE / IF NOT EXISTS)
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Gạo') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Gạo','kg',50,5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Thịt bò') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Thịt bò','kg',20,3); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Thịt heo') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Thịt heo','kg',20,3); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Thịt gà') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Thịt gà','kg',15,2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Tôm') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Tôm','kg',10,2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Mực') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Mực','kg',8,1); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Cá') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Cá','kg',10,2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Trứng') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Trứng','quả',100,20); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Rau cải') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Rau cải','kg',10,2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Cà chua') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Cà chua','kg',5,1); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Hành tây') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Hành tây','kg',5,1); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Tỏi') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Tỏi','kg',2,0.5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Gừng') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Gừng','kg',1,0.2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Nước mắm') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Nước mắm','lít',5,1); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Dầu ăn') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Dầu ăn','lít',10,2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Muối') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Muối','kg',3,0.5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Đường') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Đường','kg',3,0.5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Bún') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Bún','kg',15,3); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Bánh mì') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Bánh mì','ổ',50,10); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Mỳ') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Mỳ','kg',10,2); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Nước dùng') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Nước dùng','lít',30,5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Xúc xích') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Xúc xích','cây',30,5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Bơ') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Bơ','kg',2,0.5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Phô mai') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Phô mai','kg',2,0.5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Sốt cà chua') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Sốt cà chua','kg',3,0.5); " +
            "IF NOT EXISTS (SELECT 1 FROM Ingredients WHERE ingredient_name='Kem tươi') " +
            "  INSERT INTO Ingredients (ingredient_name,unit,quantity,min_quantity) VALUES ('Kem tươi','lít',3,0.5);";

        try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection();
             java.sql.Statement s = c.createStatement()) {
            s.execute(createRecipeTable);
            // Chạy từng câu lệnh riêng (SQL Server không cho multi-statement qua execute)
            for (String stmt : insertIngredients.split(";")) {
                stmt = stmt.trim();
                if (!stmt.isEmpty()) s.execute(stmt);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
     * Lấy ID nguyên liệu theo tên (dùng để map công thức).
     */
    private int getIngredientIdByName(java.sql.Connection c, String name) throws java.sql.SQLException {
        try (java.sql.PreparedStatement p = c.prepareStatement(
                "SELECT ingredient_id FROM Ingredients WHERE ingredient_name=?")) {
            p.setString(1, name);
            try (java.sql.ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Kiểm tra công thức đã có chưa theo item_id.
     */
    private boolean recipeExists(java.sql.Connection c, int itemId) throws java.sql.SQLException {
        try (java.sql.PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*) FROM MenuItemIngredients WHERE item_id=?")) {
            p.setInt(1, itemId);
            try (java.sql.ResultSet rs = p.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Thêm công thức nguyên liệu cho món ăn nếu chưa có.
     * Gọi sau khi đã insert đủ nguyên liệu.
     */
    private void ensureRecipesForItems() {
        try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection()) {
            List<MenuItem> items = menuItemDao.getAllMenuItems();
            for (MenuItem item : items) {
                if (recipeExists(c, item.getItemId())) continue;
                String n = item.getItemName().toLowerCase();
                // Xây dựng công thức dựa trên tên món
                insertRecipeByName(c, item.getItemId(), n);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void insertRecipe(java.sql.Connection c, int itemId, String ingName, double qty) throws java.sql.SQLException {
        int ingId = getIngredientIdByName(c, ingName);
        if (ingId < 0) return;
        // Tránh trùng
        try (java.sql.PreparedStatement chk = c.prepareStatement(
                "SELECT COUNT(*) FROM MenuItemIngredients WHERE item_id=? AND ingredient_id=?")) {
            chk.setInt(1, itemId); chk.setInt(2, ingId);
            try (java.sql.ResultSet rs = chk.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }
        try (java.sql.PreparedStatement p = c.prepareStatement(
                "INSERT INTO MenuItemIngredients(item_id,ingredient_id,quantity_needed) VALUES(?,?,?)")) {
            p.setInt(1, itemId); p.setInt(2, ingId); p.setDouble(3, qty);
            p.executeUpdate();
        }
    }

    private void insertRecipeByName(java.sql.Connection c, int itemId, String n) throws java.sql.SQLException {
        // Công thức theo logic nhà bếp: nguyên liệu chính + gia vị chung
        if (n.contains("phở") || n.contains("pho")) {
            insertRecipe(c, itemId, "Nước dùng", 0.5);
            insertRecipe(c, itemId, "Bún", 0.15);
            if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.15);
            else if (n.contains("gà")) insertRecipe(c, itemId, "Thịt gà", 0.15);
            else insertRecipe(c, itemId, "Thịt heo", 0.15);
            insertRecipe(c, itemId, "Hành tây", 0.05); insertRecipe(c, itemId, "Gừng", 0.02);
            insertRecipe(c, itemId, "Nước mắm", 0.02);

        } else if (n.contains("bún")) {
            insertRecipe(c, itemId, "Bún", 0.15); insertRecipe(c, itemId, "Nước dùng", 0.3);
            if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.15);
            else if (n.contains("tôm")) { insertRecipe(c, itemId, "Tôm", 0.1); insertRecipe(c, itemId, "Thịt heo", 0.05); }
            else insertRecipe(c, itemId, "Thịt heo", 0.12);
            insertRecipe(c, itemId, "Nước mắm", 0.02); insertRecipe(c, itemId, "Tỏi", 0.01);

        } else if (n.contains("bánh mì")) {
            insertRecipe(c, itemId, "Bánh mì", 1.0);
            if (n.contains("thịt") || n.contains("chả")) insertRecipe(c, itemId, "Thịt heo", 0.08);
            if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.08);
            if (n.contains("trứng")) insertRecipe(c, itemId, "Trứng", 1.0);
            insertRecipe(c, itemId, "Sốt cà chua", 0.03); insertRecipe(c, itemId, "Bơ", 0.01);

        } else if (n.contains("cơm")) {
            insertRecipe(c, itemId, "Gạo", 0.18);
            if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.15);
            else if (n.contains("gà")) insertRecipe(c, itemId, "Thịt gà", 0.15);
            else if (n.contains("tôm")) insertRecipe(c, itemId, "Tôm", 0.12);
            else if (n.contains("cá")) insertRecipe(c, itemId, "Cá", 0.15);
            else if (n.contains("heo") || n.contains("sườn")) insertRecipe(c, itemId, "Thịt heo", 0.15);
            else insertRecipe(c, itemId, "Thịt heo", 0.12);
            insertRecipe(c, itemId, "Dầu ăn", 0.02); insertRecipe(c, itemId, "Nước mắm", 0.02);

        } else if (n.contains("mỳ") || n.contains("mì") || n.contains("hủ tiếu")) {
            insertRecipe(c, itemId, "Mỳ", 0.12); insertRecipe(c, itemId, "Nước dùng", 0.35);
            if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.12);
            else if (n.contains("tôm")) insertRecipe(c, itemId, "Tôm", 0.1);
            else insertRecipe(c, itemId, "Thịt heo", 0.1);
            insertRecipe(c, itemId, "Hành tây", 0.03); insertRecipe(c, itemId, "Nước mắm", 0.02);

        } else if (n.contains("lẩu")) {
            insertRecipe(c, itemId, "Nước dùng", 1.5);
            if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.25);
            if (n.contains("hải sản") || n.contains("tôm")) insertRecipe(c, itemId, "Tôm", 0.15);
            if (n.contains("hải sản") || n.contains("mực")) insertRecipe(c, itemId, "Mực", 0.15);
            insertRecipe(c, itemId, "Rau cải", 0.2); insertRecipe(c, itemId, "Nước mắm", 0.05);
            insertRecipe(c, itemId, "Tỏi", 0.02); insertRecipe(c, itemId, "Gừng", 0.02);

        } else if (n.contains("gà") || n.contains("chiên") || n.contains("nướng")) {
            if (n.contains("gà")) insertRecipe(c, itemId, "Thịt gà", 0.25);
            else if (n.contains("bò")) insertRecipe(c, itemId, "Thịt bò", 0.2);
            else if (n.contains("tôm")) insertRecipe(c, itemId, "Tôm", 0.2);
            else insertRecipe(c, itemId, "Thịt heo", 0.2);
            insertRecipe(c, itemId, "Dầu ăn", 0.05); insertRecipe(c, itemId, "Tỏi", 0.01);
            insertRecipe(c, itemId, "Nước mắm", 0.02); insertRecipe(c, itemId, "Đường", 0.01);

        } else if (n.contains("súp") || n.contains("canh")) {
            insertRecipe(c, itemId, "Nước dùng", 0.4); insertRecipe(c, itemId, "Rau cải", 0.1);
            if (n.contains("gà")) insertRecipe(c, itemId, "Thịt gà", 0.1);
            else if (n.contains("tôm")) insertRecipe(c, itemId, "Tôm", 0.1);
            else insertRecipe(c, itemId, "Thịt heo", 0.08);
            insertRecipe(c, itemId, "Muối", 0.01); insertRecipe(c, itemId, "Hành tây", 0.03);

        } else if (n.contains("salad") || n.contains("rau")) {
            insertRecipe(c, itemId, "Rau cải", 0.15); insertRecipe(c, itemId, "Cà chua", 0.05);
            insertRecipe(c, itemId, "Dầu ăn", 0.02); insertRecipe(c, itemId, "Muối", 0.005);

        } else if (n.contains("trứng")) {
            insertRecipe(c, itemId, "Trứng", 2.0); insertRecipe(c, itemId, "Dầu ăn", 0.02);
            insertRecipe(c, itemId, "Muối", 0.005);

        } else {
            // Công thức mặc định cho món không xác định
            insertRecipe(c, itemId, "Dầu ăn", 0.03);
            insertRecipe(c, itemId, "Nước mắm", 0.02);
            insertRecipe(c, itemId, "Muối", 0.01);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 1: ĐƠN CẦN NẤU
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildOrderTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        orderModel = new DefaultTableModel(new String[]{"ID Đơn", "Bàn", "Nhân viên", "Trạng thái", "Thời gian"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable orderTable = new JTable(orderModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        detailModel = new DefaultTableModel(new String[]{"Tên món", "Số lượng", "Thành tiền"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable detailTable = new JTable(detailModel);

        refreshOrders();

        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && orderTable.getSelectedRow() >= 0) {
                int mr = orderTable.convertRowIndexToModel(orderTable.getSelectedRow());
                selectedOrderId = (int) orderModel.getValueAt(mr, 0);
                loadOrderDetails(selectedOrderId);
            }
        });

        JButton btnRefresh   = new JButton("🔄 Làm mới");
        JButton btnStartCook = new JButton("🔥 Bắt đầu nấu");
        JButton btnDone      = new JButton("✅ Hoàn thành / Đã phục vụ");
        btnDone.setBackground(new Color(70, 160, 70));
        btnDone.setForeground(Color.WHITE);

        JLabel lblStatus = new JLabel("  Chọn đơn hàng để xem chi tiết và cập nhật trạng thái.");

        btnRefresh.addActionListener(e -> refreshOrders());

        btnStartCook.addActionListener(e -> {
            if (selectedOrderId < 0) { JOptionPane.showMessageDialog(this, "Chọn đơn hàng!"); return; }
            orderDao.updateOrderStatus(selectedOrderId, "Cooking");
            refreshOrders();
            lblStatus.setText("  🔥 Đơn #" + selectedOrderId + " đang được nấu...");
        });

        btnDone.addActionListener(e -> {
            if (selectedOrderId < 0) { JOptionPane.showMessageDialog(this, "Chọn đơn hàng!"); return; }
            // Trừ nguyên liệu kho theo công thức
            deductIngredientsForOrder(selectedOrderId);
            orderDao.updateOrderStatus(selectedOrderId, "Ready");
            refreshOrders();
            detailModel.setRowCount(0);
            selectedOrderId = -1;
            lblStatus.setText("  ✅ Đơn đã hoàn thành – đã trừ nguyên liệu kho tự động!");
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(btnRefresh); btnPanel.add(btnStartCook); btnPanel.add(btnDone); btnPanel.add(lblStatus);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(480);

        JPanel leftPanel = new JPanel(new BorderLayout(3,3));
        leftPanel.add(new JLabel("  📋 Danh sách đơn hàng (Pending / Cooking)", SwingConstants.LEFT), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(3,3));
        rightPanel.add(new JLabel("  🧾 Chi tiết đơn hàng đã chọn", SwingConstants.LEFT), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);

        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void refreshOrders() {
        orderModel.setRowCount(0);
        String sql = "SELECT o.order_id, t.table_name, u.full_name, o.status, o.order_time " +
                     "FROM Orders o " +
                     "JOIN DiningTables t ON o.table_id = t.table_id " +
                     "JOIN Users u ON o.user_id = u.user_id " +
                     "WHERE o.status IN ('Pending','Cooking') " +
                     "ORDER BY o.order_time ASC";
        try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection();
             java.sql.Statement s = c.createStatement();
             java.sql.ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                String status = rs.getString("status");
                String statusLabel = "Pending".equals(status) ? "⏳ Chờ nấu" : "🔥 Đang nấu";
                orderModel.addRow(new Object[]{
                    rs.getInt("order_id"), rs.getString("table_name"),
                    rs.getString("full_name"), statusLabel, rs.getTimestamp("order_time")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadOrderDetails(int orderId) {
        detailModel.setRowCount(0);
        List<MenuItem> allItems = menuItemDao.getAllMenuItems();
        for (OrderDetail d : detailDao.getOrderDetailsByOrderId(orderId)) {
            String name = allItems.stream().filter(m -> m.getItemId() == d.getItemId())
                .map(MenuItem::getItemName).findFirst().orElse("ID:" + d.getItemId());
            detailModel.addRow(new Object[]{name, d.getQuantity(), String.format("%,.0f đ", d.getSubtotal())});
        }
    }

    /**
     * Trừ nguyên liệu kho khi hoàn thành 1 đơn hàng.
     * Với mỗi OrderDetail → lấy công thức trong MenuItemIngredients → trừ kho.
     */
    private void deductIngredientsForOrder(int orderId) {
        List<OrderDetail> details = detailDao.getOrderDetailsByOrderId(orderId);
        String sql = "SELECT ingredient_id, quantity_needed FROM MenuItemIngredients WHERE item_id=?";
        try (java.sql.Connection c = com.nhom13.restaurant.managementsys.database.Database.getConnection();
             java.sql.PreparedStatement p = c.prepareStatement(sql)) {
            for (OrderDetail d : details) {
                p.setInt(1, d.getItemId());
                try (java.sql.ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        int ingId  = rs.getInt("ingredient_id");
                        double qty = rs.getDouble("quantity_needed") * d.getQuantity();
                        ingredientDao.useStock(ingId, qty);
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 2: SỐ SUẤT CÓ SẴN HÔM NAY
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildAvailabilityTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        String[] cols = {"ID", "Danh mục", "Tên món", "Giá", "Số suất còn lại"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField txtQty    = new JTextField("10", 6);
        JTextField txtSearch = new JTextField(18);

        Runnable reload = () -> {
            model.setRowCount(0);
            List<Category> cats = categoryDao.getAllCategories();
            for (MenuItem item : menuItemDao.getAllMenuItems()) {
                if (!item.isAvailable()) continue;
                String catName = cats.stream().filter(c -> c.getCategoryId() == item.getCategoryId())
                    .map(Category::getCategoryName).findFirst().orElse("-");
                model.addRow(new Object[]{item.getItemId(), catName, item.getItemName(),
                    String.format("%,.0f", item.getPrice()), item.getAvailableQuantity()});
            }
        };
        reload.run();

        JButton btnSet     = new JButton("✏️ Đặt số suất");
        JButton btnRefresh = new JButton("🔄 Làm mới");
        JButton btnAutoRecipe = new JButton("🔗 Tạo công thức tự động");

        btnSet.addActionListener(e -> {
            int sr = table.getSelectedRow();
            if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn món cần cập nhật số suất!"); return; }
            int mr = table.convertRowIndexToModel(sr);
            int itemId = (int) model.getValueAt(mr, 0);
            try {
                int qty = Integer.parseInt(txtQty.getText().trim());
                menuItemDao.setAvailableQuantity(itemId, qty);
                reload.run();
                JOptionPane.showMessageDialog(this, "✅ Đã đặt " + qty + " suất cho món: " + model.getValueAt(mr, 2));
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Số suất phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnRefresh.addActionListener(e -> { txtSearch.setText(""); sorter.setRowFilter(null); reload.run(); });
        txtSearch.addActionListener(e -> sorter.setRowFilter(txtSearch.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtSearch.getText().trim())));

        btnAutoRecipe.addActionListener(e -> {
            ensureIngredientsAndRecipes();
            ensureRecipesForItems();
            JOptionPane.showMessageDialog(this, "✅ Đã tạo/cập nhật công thức nguyên liệu cho các món trong thực đơn!");
        });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("🔍 Tìm:"));
        topBar.add(txtSearch);
        topBar.add(new JButton("Tìm") {{ addActionListener(ev -> txtSearch.getActionListeners()[0].actionPerformed(null)); }});
        topBar.add(btnRefresh);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Số suất hôm nay:"));
        form.add(txtQty);
        form.add(btnSet);
        form.add(btnAutoRecipe);
        form.add(new JLabel("   ⚠️ Phục vụ gọi món sẽ trừ dần số này."));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 3: KHO NGUYÊN LIỆU
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

        JTextField txtAmount = new JTextField("1", 8);

        Runnable reload = () -> {
            model.setRowCount(0);
            for (Ingredient ing : ingredientDao.getAllIngredients())
                model.addRow(new Object[]{ing.getIngredientId(), ing.getIngredientName(), ing.getUnit(),
                    String.format("%.2f", ing.getQuantity()), String.format("%.2f", ing.getMinQuantity()),
                    ing.isLowStock() ? "⚠️ Sắp hết" : "✅ Đủ"});
        };
        reload.run();

        JButton btnAddStock  = new JButton("📥 Nhập hàng (+)");
        JButton btnUseStock  = new JButton("📤 Xuất kho (-)");
        JButton btnRefresh   = new JButton("🔄 Làm mới");

        btnAddStock.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nguyên liệu!"); return; }
            int mr = table.convertRowIndexToModel(sr); int id = (int) model.getValueAt(mr, 0);
            try {
                double delta = Double.parseDouble(txtAmount.getText().trim());
                if (delta <= 0) throw new NumberFormatException();
                ingredientDao.addStock(id, delta); reload.run();
                JOptionPane.showMessageDialog(this, "✅ Đã nhập thêm " + delta + " " + model.getValueAt(mr, 2));
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnUseStock.addActionListener(e -> {
            int sr = table.getSelectedRow(); if (sr < 0) { JOptionPane.showMessageDialog(this, "Chọn nguyên liệu!"); return; }
            int mr = table.convertRowIndexToModel(sr); int id = (int) model.getValueAt(mr, 0);
            try {
                double delta = Double.parseDouble(txtAmount.getText().trim());
                if (delta <= 0) throw new NumberFormatException();
                ingredientDao.useStock(id, delta); reload.run();
                JOptionPane.showMessageDialog(this, "✅ Đã xuất kho " + delta + " " + model.getValueAt(mr, 2));
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });

        btnRefresh.addActionListener(e -> reload.run());

        JTextField txtSearch = new JTextField(16);
        txtSearch.addActionListener(e -> sorter.setRowFilter(txtSearch.getText().trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + txtSearch.getText().trim())));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("🔍:")); topBar.add(txtSearch);
        topBar.add(new JButton("Tìm") {{ addActionListener(ev -> txtSearch.getActionListeners()[0].actionPerformed(null)); }});
        topBar.add(btnRefresh);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Số lượng:")); form.add(txtAmount);
        form.add(btnAddStock); form.add(btnUseStock);
        form.add(new JLabel("   ⚠️ Nguyên liệu sắp hết cần báo Admin nhập thêm hàng!"));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }
}
