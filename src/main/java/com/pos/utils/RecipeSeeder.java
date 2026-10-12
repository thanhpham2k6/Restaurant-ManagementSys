package com.pos.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Tự động đồng bộ nguyên liệu và công thức định lượng (recipes) cho toàn bộ thực đơn
 * Giúp cột "Suất còn" hiển thị số lượng suất thực tế thay vì "Sẵn sàng".
 */
public final class RecipeSeeder {
    private static final Logger log = LogManager.getLogger(RecipeSeeder.class);

    public static void main(String[] args) {
        seedMissingRecipesIfNecessary();
        backupDatabase();
    }

    public static void backupDatabase() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            java.io.File publicBak = new java.io.File("C:/Users/Public/RestaurantManagementSys.bak");
            java.io.File targetBak = new java.io.File("database/RestaurantManagementSys.bak").getAbsoluteFile();
            if (publicBak.exists()) publicBak.delete();

            String sql = "BACKUP DATABASE RestaurantManagementSys TO DISK = '" + publicBak.getAbsolutePath().replace("'", "''") + "' WITH FORMAT, INIT, NAME = 'Full RestaurantManagementSys Backup'";
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                log.info("SQL Server đã backup ra {}", publicBak.getAbsolutePath());
            }

            if (publicBak.exists()) {
                java.nio.file.Files.copy(publicBak.toPath(), targetBak.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Đã đồng bộ file backup mới nhất về {}", targetBak.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Không thể backup CSDL tự động: {}", e.getMessage());
        }
    }

    public static void seedMissingRecipesIfNecessary() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Kiểm tra số lượng công thức hiện tại
            int recipeCount = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM recipes")) {
                if (rs.next()) recipeCount = rs.getInt(1);
            }

            // Nếu đã có từ 25 công thức trở lên, không cần nạp lại
            if (recipeCount >= 25) {
                log.info("CSDL đã có {} công thức định lượng món ăn, bỏ qua seeding.", recipeCount);
                return;
            }

            log.info("Số công thức hiện tại ({}) ít hơn mức chuẩn. Đang tự động nạp định lượng cho thực đơn...", recipeCount);

            // 1. Đảm bảo danh sách nguyên liệu đa dạng có trong kho
            ensureIngredients(conn);

            // 2. Nạp công thức cho từng món ăn trong menu_items
            ensureRecipes(conn);

            log.info("Tự động đồng bộ công thức định lượng nguyên liệu kho hoàn tất!");
        } catch (Exception e) {
            log.warn("Không thể tự động nạp công thức món ăn (có thể CSDL chưa sẵn sàng): {}", e.getMessage());
        }
    }

    private static void ensureIngredients(Connection conn) throws SQLException {
        // Danh sách nguyên liệu cần có: Tên -> [Đơn vị, Tồn kho, Mức tối thiểu]
        Object[][] ings = {
            {"Thịt bò tươi", "kg", 45.0, 5.0},
            {"Thịt gà ta", "kg", 35.0, 5.0},
            {"Thịt heo sườn", "kg", 40.0, 5.0},
            {"Thịt ba chỉ heo", "kg", 30.0, 5.0},
            {"Bún tươi", "kg", 60.0, 10.0},
            {"Bánh phở tươi", "kg", 50.0, 10.0},
            {"Gạo thơm ST25", "kg", 80.0, 15.0},
            {"Tôm sú tươi", "kg", 25.0, 4.0},
            {"Mực ống tươi", "kg", 20.0, 3.0},
            {"Cá tươi", "kg", 25.0, 4.0},
            {"Rau sống tổng hợp", "kg", 30.0, 5.0},
            {"Nước dùng phở/bún", "lít", 100.0, 20.0},
            {"Trứng gà", "quả", 150.0, 20.0},
            {"Bánh mì ổ", "ổ", 60.0, 10.0},
            {"Patê gan", "kg", 15.0, 2.0},
            {"Bột gạo pha sẵn", "kg", 30.0, 5.0},
            {"Đậu phụ tươi", "kg", 25.0, 5.0},
            {"Mì sợi tươi", "kg", 35.0, 5.0},
            {"Khoai tây tươi", "kg", 30.0, 5.0},
            {"Trà xanh & sả", "kg", 15.0, 2.0},
            {"Cà phê phin", "kg", 20.0, 3.0},
            {"Sữa đặc & sữa tươi", "lít", 40.0, 5.0},
            {"Nước khoáng đóng chai", "chai", 120.0, 20.0}
        };

        String checkSql = "SELECT id FROM ingredients WHERE name = ?";
        String insertSql = "INSERT INTO ingredients (name, unit, stock_qty, min_qty, is_active) VALUES (?, ?, ?, ?, 1)";
        String updateSql = "UPDATE ingredients SET stock_qty = ? WHERE name = ? AND stock_qty < ?";

        for (Object[] row : ings) {
            String name = (String) row[0];
            String unit = (String) row[1];
            double stock = (Double) row[2];
            double min = (Double) row[3];

            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setString(1, name);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                            psIns.setString(1, name);
                            psIns.setString(2, unit);
                            psIns.setBigDecimal(3, BigDecimal.valueOf(stock));
                            psIns.setBigDecimal(4, BigDecimal.valueOf(min));
                            psIns.executeUpdate();
                        }
                    } else {
                        // Nếu đã có nhưng hết sạch tồn kho thì nạp thêm để demo
                        try (PreparedStatement psUpd = conn.prepareStatement(updateSql)) {
                            psUpd.setBigDecimal(1, BigDecimal.valueOf(stock));
                            psUpd.setString(2, name);
                            psUpd.setBigDecimal(3, BigDecimal.valueOf(1.0));
                            psUpd.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private static void ensureRecipes(Connection conn) throws SQLException {
        // Lấy danh sách nguyên liệu dạng Map: Tên -> ID
        Map<String, Integer> ingMap = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM ingredients")) {
            while (rs.next()) {
                ingMap.put(rs.getString("name"), rs.getInt("id"));
            }
        }

        // Lấy danh sách món ăn: Tên -> ID
        Map<String, Integer> itemMap = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM menu_items")) {
            while (rs.next()) {
                itemMap.put(rs.getString("name"), rs.getInt("id"));
            }
        }

        // Bảng quy định công thức: Món -> danh sách [Tên nguyên liệu, Số lượng định mức / 1 suất]
        Map<String, List<Object[]>> recipeDefs = new LinkedHashMap<>();

        // Món nước
        recipeDefs.put("Phở bò tái nạm", Arrays.asList(
            new Object[]{"Thịt bò tươi", 0.150},
            new Object[]{"Bánh phở tươi", 0.200},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));
        recipeDefs.put("Phở gà ta lá chanh", Arrays.asList(
            new Object[]{"Thịt gà ta", 0.150},
            new Object[]{"Bánh phở tươi", 0.200},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));
        recipeDefs.put("Hủ tiếu Nam Vang", Arrays.asList(
            new Object[]{"Thịt heo sườn", 0.120},
            new Object[]{"Tôm sú tươi", 0.080},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));
        recipeDefs.put("Hủ tiếu mực", Arrays.asList(
            new Object[]{"Mực ống tươi", 0.150},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));
        recipeDefs.put("Bánh canh cua", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.100},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));

        // Món bún
        recipeDefs.put("Bún chả Hà Nội", Arrays.asList(
            new Object[]{"Thịt ba chỉ heo", 0.150},
            new Object[]{"Bún tươi", 0.200},
            new Object[]{"Rau sống tổng hợp", 0.080}
        ));
        recipeDefs.put("Bún bò Huế đặc biệt", Arrays.asList(
            new Object[]{"Thịt bò tươi", 0.120},
            new Object[]{"Bún tươi", 0.200},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));
        recipeDefs.put("Bún riêu cua đồng", Arrays.asList(
            new Object[]{"Đậu phụ tươi", 0.120},
            new Object[]{"Bún tươi", 0.200},
            new Object[]{"Rau sống tổng hợp", 0.080}
        ));
        recipeDefs.put("Bún thịt nướng chả giò", Arrays.asList(
            new Object[]{"Thịt ba chỉ heo", 0.140},
            new Object[]{"Bún tươi", 0.200},
            new Object[]{"Rau sống tổng hợp", 0.080}
        ));
        recipeDefs.put("Bún cá Nha Trang", Arrays.asList(
            new Object[]{"Cá tươi", 0.150},
            new Object[]{"Bún tươi", 0.200},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));
        recipeDefs.put("Bún đậu mắm tôm thập cẩm", Arrays.asList(
            new Object[]{"Bún tươi", 0.250},
            new Object[]{"Đậu phụ tươi", 0.150},
            new Object[]{"Thịt ba chỉ heo", 0.100}
        ));

        // Món mì
        recipeDefs.put("Mì Quảng gà trứng", Arrays.asList(
            new Object[]{"Thịt gà ta", 0.150},
            new Object[]{"Mì sợi tươi", 0.200},
            new Object[]{"Trứng gà", 1.0}
        ));
        recipeDefs.put("Cao lầu Hội An", Arrays.asList(
            new Object[]{"Thịt heo sườn", 0.120},
            new Object[]{"Mì sợi tươi", 0.200},
            new Object[]{"Rau sống tổng hợp", 0.050}
        ));
        recipeDefs.put("Mì xào giòn hải sản", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.100},
            new Object[]{"Mực ống tươi", 0.100},
            new Object[]{"Mì sợi tươi", 0.180}
        ));
        recipeDefs.put("Mì vịt tiềm", Arrays.asList(
            new Object[]{"Thịt gà ta", 0.200},
            new Object[]{"Mì sợi tươi", 0.200},
            new Object[]{"Nước dùng phở/bún", 0.400}
        ));

        // Cơm
        recipeDefs.put("Cơm tấm sườn bì chả", Arrays.asList(
            new Object[]{"Thịt heo sườn", 0.180},
            new Object[]{"Gạo thơm ST25", 0.150},
            new Object[]{"Trứng gà", 1.0}
        ));
        recipeDefs.put("Cơm gà xối mỡ giòn rụm", Arrays.asList(
            new Object[]{"Thịt gà ta", 0.200},
            new Object[]{"Gạo thơm ST25", 0.150}
        ));
        recipeDefs.put("Cơm rang dưa bò", Arrays.asList(
            new Object[]{"Thịt bò tươi", 0.120},
            new Object[]{"Gạo thơm ST25", 0.150},
            new Object[]{"Trứng gà", 1.0}
        ));
        recipeDefs.put("Cơm chiên hải sản Dương Châu", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.080},
            new Object[]{"Mực ống tươi", 0.080},
            new Object[]{"Gạo thơm ST25", 0.150},
            new Object[]{"Trứng gà", 1.0}
        ));
        recipeDefs.put("Cơm niêu cá kho tộ", Arrays.asList(
            new Object[]{"Cá tươi", 0.200},
            new Object[]{"Gạo thơm ST25", 0.150}
        ));
        recipeDefs.put("Cơm sườn nướng mật ong", Arrays.asList(
            new Object[]{"Thịt heo sườn", 0.180},
            new Object[]{"Gạo thơm ST25", 0.150}
        ));

        // Bánh mì
        recipeDefs.put("Bánh mì thịt nướng pate", Arrays.asList(
            new Object[]{"Bánh mì ổ", 1.0},
            new Object[]{"Patê gan", 0.050},
            new Object[]{"Thịt ba chỉ heo", 0.080}
        ));
        recipeDefs.put("Bánh mì xíu mại trứng muối", Arrays.asList(
            new Object[]{"Bánh mì ổ", 1.0},
            new Object[]{"Thịt ba chỉ heo", 0.100},
            new Object[]{"Trứng gà", 1.0}
        ));
        recipeDefs.put("Bánh mì chảo đặc biệt", Arrays.asList(
            new Object[]{"Bánh mì ổ", 1.0},
            new Object[]{"Patê gan", 0.050},
            new Object[]{"Thịt bò tươi", 0.080},
            new Object[]{"Trứng gà", 2.0}
        ));

        // Món bánh
        recipeDefs.put("Bánh xèo tôm thịt miền Tây", Arrays.asList(
            new Object[]{"Bột gạo pha sẵn", 0.150},
            new Object[]{"Tôm sú tươi", 0.080},
            new Object[]{"Thịt ba chỉ heo", 0.080},
            new Object[]{"Rau sống tổng hợp", 0.100}
        ));
        recipeDefs.put("Bánh cuốn nóng chả lụa", Arrays.asList(
            new Object[]{"Bột gạo pha sẵn", 0.150},
            new Object[]{"Thịt ba chỉ heo", 0.080}
        ));
        recipeDefs.put("Bánh khọt Vũng Tàu", Arrays.asList(
            new Object[]{"Bột gạo pha sẵn", 0.150},
            new Object[]{"Tôm sú tươi", 0.100}
        ));
        recipeDefs.put("Bánh bèo tôm cháy chén", Arrays.asList(
            new Object[]{"Bột gạo pha sẵn", 0.150},
            new Object[]{"Tôm sú tươi", 0.080}
        ));

        // Ăn vặt & Gỏi
        recipeDefs.put("Nem nướng Nha Trang cuốn", Arrays.asList(
            new Object[]{"Thịt ba chỉ heo", 0.180},
            new Object[]{"Rau sống tổng hợp", 0.100}
        ));
        recipeDefs.put("Gỏi cuốn tôm thịt (4 cuốn)", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.100},
            new Object[]{"Thịt ba chỉ heo", 0.080},
            new Object[]{"Bún tươi", 0.100},
            new Object[]{"Rau sống tổng hợp", 0.080}
        ));
        recipeDefs.put("Chả giò hải sản giòn rụm", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.100},
            new Object[]{"Mực ống tươi", 0.080}
        ));
        recipeDefs.put("Khoai tây chiên lắc phô mai", Collections.singletonList(
            new Object[]{"Khoai tây tươi", 0.250}
        ));
        recipeDefs.put("Gỏi gà xé phay bắp cải", Arrays.asList(
            new Object[]{"Thịt gà ta", 0.200},
            new Object[]{"Rau sống tổng hợp", 0.150}
        ));
        recipeDefs.put("Gỏi ngó sen tôm thịt", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.100},
            new Object[]{"Thịt ba chỉ heo", 0.080},
            new Object[]{"Rau sống tổng hợp", 0.150}
        ));
        recipeDefs.put("Gỏi bò bóp thấu", Arrays.asList(
            new Object[]{"Thịt bò tươi", 0.180},
            new Object[]{"Rau sống tổng hợp", 0.120}
        ));

        // Món mặn & Hải sản
        recipeDefs.put("Cá kho tộ đậm đà", Collections.singletonList(
            new Object[]{"Cá tươi", 0.250}
        ));
        recipeDefs.put("Thịt kho tàu trứng cút", Arrays.asList(
            new Object[]{"Thịt ba chỉ heo", 0.200},
            new Object[]{"Trứng gà", 2.0}
        ));
        recipeDefs.put("Sườn xào chua ngọt", Collections.singletonList(
            new Object[]{"Thịt heo sườn", 0.250}
        ));
        recipeDefs.put("Gà ta rang muối giòn", Collections.singletonList(
            new Object[]{"Thịt gà ta", 0.250}
        ));
        recipeDefs.put("Bò lúc lắc khoai tây", Arrays.asList(
            new Object[]{"Thịt bò tươi", 0.200},
            new Object[]{"Khoai tây tươi", 0.150}
        ));
        recipeDefs.put("Tôm sú rang me", Collections.singletonList(
            new Object[]{"Tôm sú tươi", 0.250}
        ));
        recipeDefs.put("Mực tươi xào cần tỏi", Collections.singletonList(
            new Object[]{"Mực ống tươi", 0.250}
        ));
        recipeDefs.put("Cá chẽm chiên sốt chua ngọt", Collections.singletonList(
            new Object[]{"Cá tươi", 0.300}
        ));

        // Lẩu
        recipeDefs.put("Lẩu Thái tomyum hải sản", Arrays.asList(
            new Object[]{"Tôm sú tươi", 0.250},
            new Object[]{"Mực ống tươi", 0.200},
            new Object[]{"Rau sống tổng hợp", 0.300},
            new Object[]{"Nước dùng phở/bún", 0.800}
        ));
        recipeDefs.put("Lẩu gà lá é đặc sản", Arrays.asList(
            new Object[]{"Thịt gà ta", 0.500},
            new Object[]{"Rau sống tổng hợp", 0.250},
            new Object[]{"Nước dùng phở/bún", 0.800}
        ));
        recipeDefs.put("Lẩu riêu cua bắp bò sườn sụn", Arrays.asList(
            new Object[]{"Thịt bò tươi", 0.250},
            new Object[]{"Thịt heo sườn", 0.200},
            new Object[]{"Bún tươi", 0.300},
            new Object[]{"Nước dùng phở/bún", 0.800}
        ));

        // Tráng miệng & Đồ uống
        recipeDefs.put("Chè khúc bạch hạnh nhân", Collections.singletonList(
            new Object[]{"Sữa đặc & sữa tươi", 0.150}
        ));
        recipeDefs.put("Sữa chua mít trân châu", Collections.singletonList(
            new Object[]{"Sữa đặc & sữa tươi", 0.150}
        ));
        recipeDefs.put("Kem dừa Tràng Tiền", Collections.singletonList(
            new Object[]{"Sữa đặc & sữa tươi", 0.100}
        ));
        recipeDefs.put("Trà sen vàng macchiato", Arrays.asList(
            new Object[]{"Trà xanh & sả", 0.050},
            new Object[]{"Sữa đặc & sữa tươi", 0.100}
        ));
        recipeDefs.put("Trà đào cam sả", Collections.singletonList(
            new Object[]{"Trà xanh & sả", 0.060}
        ));
        recipeDefs.put("Cà phê sữa đá pha phin", Arrays.asList(
            new Object[]{"Cà phê phin", 0.030},
            new Object[]{"Sữa đặc & sữa tươi", 0.060}
        ));
        recipeDefs.put("Nước suối khoáng", Collections.singletonList(
            new Object[]{"Nước khoáng đóng chai", 1.0}
        ));

        // Chèn các công thức vào CSDL
        String upsertSql = "MERGE recipes AS target " +
            "USING (SELECT ? as menu_item_id, ? as ingredient_id) AS source " +
            "ON target.menu_item_id = source.menu_item_id AND target.ingredient_id = source.ingredient_id " +
            "WHEN MATCHED THEN UPDATE SET qty_per_serving = ? " +
            "WHEN NOT MATCHED THEN INSERT (menu_item_id, ingredient_id, qty_per_serving) VALUES (?, ?, ?);";

        try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
            for (Map.Entry<String, List<Object[]>> entry : recipeDefs.entrySet()) {
                String dishName = entry.getKey();
                Integer dishId = itemMap.get(dishName);
                if (dishId == null) {
                    // Thử tìm theo tên tương tự
                    for (Map.Entry<String, Integer> itemEntry : itemMap.entrySet()) {
                        if (itemEntry.getKey().contains(dishName) || dishName.contains(itemEntry.getKey())) {
                            dishId = itemEntry.getValue();
                            break;
                        }
                    }
                }
                if (dishId == null) continue;

                for (Object[] recipeLine : entry.getValue()) {
                    String ingName = (String) recipeLine[0];
                    Double qty = (Double) recipeLine[1];
                    Integer ingId = ingMap.get(ingName);
                    if (ingId == null) continue;

                    ps.setInt(1, dishId);
                    ps.setInt(2, ingId);
                    ps.setBigDecimal(3, BigDecimal.valueOf(qty));
                    ps.setInt(4, dishId);
                    ps.setInt(5, ingId);
                    ps.setBigDecimal(6, BigDecimal.valueOf(qty));
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}
