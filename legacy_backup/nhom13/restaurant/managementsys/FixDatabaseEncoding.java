package com.nhom13.restaurant.managementsys;

import com.nhom13.restaurant.managementsys.database.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class FixDatabaseEncoding {
    public static void main(String[] args) {
        try (Connection conn = Database.getConnection()) {
            if (conn == null) {
                System.out.println("Cannot connect to DB");
                return;
            }

            // Xóa các danh mục cũ bị lỗi font (từ ID = 3 trở đi, hoặc xóa hết các món mình vừa thêm)
            // Lấy ID của các món bị lỗi font (nếu chứa kí tự đặc biệt hoặc ID > 2)
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM MenuItems WHERE item_id > 2 OR category_id > 2");
                st.executeUpdate("DELETE FROM Categories WHERE category_id > 2");
            }

            String[] categories = {
                "Món nước", "Món bún", "Món mì", "Cơm", "Bánh mì",
                "Món bánh", "Món ăn vặt", "Món gỏi", "Món mặn",
                "Hải sản", "Lẩu", "Tráng miệng"
            };

            Map<String, Integer> catMap = new HashMap<>();
            
            // Insert categories
            String insertCat = "INSERT INTO Categories (category_name) VALUES (?)";
            try (PreparedStatement pst = conn.prepareStatement(insertCat, Statement.RETURN_GENERATED_KEYS)) {
                for (String c : categories) {
                    pst.setString(1, c);
                    pst.executeUpdate();
                    try (ResultSet rs = pst.getGeneratedKeys()) {
                        if (rs.next()) catMap.put(c, rs.getInt(1));
                    }
                }
            }

            // Insert menu items
            String[][] items = {
                {"Món nước", "Phở bò", "45000"},
                {"Món nước", "Phở gà", "40000"},
                {"Món bún", "Bún chả", "45000"},
                {"Món bún", "Bún bò Huế", "50000"},
                {"Món bún", "Bún riêu", "40000"},
                {"Món bún", "Bún thịt nướng", "45000"},
                {"Món bún", "Bún cá", "40000"},
                {"Món mì", "Mì Quảng", "45000"},
                {"Món mì", "Cao lầu", "50000"},
                {"Món nước", "Hủ tiếu", "45000"},
                {"Cơm", "Cơm tấm sườn", "50000"},
                {"Cơm", "Cơm gà xối mỡ", "50000"},
                {"Cơm", "Cơm rang dưa bò", "45000"},
                {"Cơm", "Cơm chiên hải sản", "55000"},
                {"Cơm", "Cơm niêu", "60000"},
                {"Cơm", "Cơm sườn nướng", "55000"},
                {"Cơm", "Cơm gà Hội An", "50000"},
                {"Cơm", "Cơm cá kho", "45000"},
                {"Bánh mì", "Bánh mì thịt", "30000"},
                {"Bánh mì", "Bánh mì xíu mại", "35000"},
                {"Bánh mì", "Bánh mì chảo", "45000"},
                {"Món bánh", "Bánh xèo", "40000"},
                {"Món bánh", "Bánh cuốn", "35000"},
                {"Món bánh", "Bánh khọt", "40000"},
                {"Món ăn vặt", "Nem nướng", "45000"},
                {"Món ăn vặt", "Gỏi cuốn", "30000"},
                {"Món ăn vặt", "Chả giò", "35000"},
                {"Món gỏi", "Gỏi gà", "50000"},
                {"Món gỏi", "Gỏi ngó sen", "50000"},
                {"Món mặn", "Cá kho tộ", "60000"},
                {"Món mặn", "Thịt kho trứng", "55000"},
                {"Món mặn", "Sườn xào chua ngọt", "65000"},
                {"Món mặn", "Gà rang muối", "70000"},
                {"Món mặn", "Gà nướng mật ong", "75000"},
                {"Món mặn", "Vịt quay", "80000"},
                {"Món mặn", "Bò lúc lắc", "75000"},
                {"Món mặn", "Bò xào rau củ", "65000"},
                {"Hải sản", "Tôm rang me", "70000"},
                {"Hải sản", "Mực xào cần tỏi", "75000"},
                {"Hải sản", "Cá chiên xù", "65000"},
                {"Lẩu", "Lẩu thái", "180000"},
                {"Lẩu", "Lẩu gà lá é", "200000"},
                {"Lẩu", "Lẩu bò", "220000"},
                {"Lẩu", "Lẩu hải sản", "250000"},
                {"Tráng miệng", "Chè khúc bạch", "30000"},
                {"Tráng miệng", "Chè đậu đen", "20000"},
                {"Tráng miệng", "Sữa chua mít", "25000"},
                {"Tráng miệng", "Kem dừa", "30000"},
                {"Tráng miệng", "Trái cây thập cẩm", "35000"},
                {"Tráng miệng", "Chè thái", "30000"}
            };

            String insertItem = "INSERT INTO MenuItems (category_id, item_name, price, is_available, available_quantity) VALUES (?, ?, ?, 1, 100)";
            try (PreparedStatement pst = conn.prepareStatement(insertItem)) {
                for (String[] item : items) {
                    pst.setInt(1, catMap.get(item[0]));
                    pst.setString(2, item[1]);
                    pst.setDouble(3, Double.parseDouble(item[2]));
                    pst.executeUpdate();
                }
            }

            System.out.println("Inserted items successfully using JDBC (UTF-8)!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
