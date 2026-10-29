package com.pos.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích tự động phát hiện và khôi phục lỗi font tiếng Việt (Mojibake UTF-8 bị đọc nhầm thành Windows-1252/ANSI)
 * trong cơ sở dữ liệu và dữ liệu hiển thị trên giao diện.
 */
public final class DatabaseEncodingFixer {
    private static final Logger log = LogManager.getLogger(DatabaseEncodingFixer.class);

    private static final Map<String, String> CANONICAL_USERS = new HashMap<>();
    private static final Map<Integer, String> CANONICAL_ITEMS = new HashMap<>();
    private static final Map<Integer, String> CANONICAL_ITEM_UNITS = new HashMap<>();
    private static final Map<Integer, String> CANONICAL_CATEGORIES = new HashMap<>();
    private static final Map<Integer, String> CANONICAL_MENU_ITEMS = new HashMap<>();
    private static final Map<Integer, String> CANONICAL_TABLES = new HashMap<>();
    private static final Map<Integer, String> CANONICAL_SUPPLIERS = new HashMap<>();

    static {
        // Users
        CANONICAL_USERS.put("admin", "Nguyễn Văn Quản Lý");
        CANONICAL_USERS.put("cashier1", "Trần Thị Thu Ngân");
        CANONICAL_USERS.put("kitchen1", "Lê Văn Đầu Bếp");

        // Categories (1-13)
        CANONICAL_CATEGORIES.put(1, "Món nước");
        CANONICAL_CATEGORIES.put(2, "Món bún");
        CANONICAL_CATEGORIES.put(3, "Món mì");
        CANONICAL_CATEGORIES.put(4, "Cơm");
        CANONICAL_CATEGORIES.put(5, "Bánh mì");
        CANONICAL_CATEGORIES.put(6, "Món bánh");
        CANONICAL_CATEGORIES.put(7, "Món ăn vặt");
        CANONICAL_CATEGORIES.put(8, "Món gỏi");
        CANONICAL_CATEGORIES.put(9, "Món mặn");
        CANONICAL_CATEGORIES.put(10, "Hải sản");
        CANONICAL_CATEGORIES.put(11, "Lẩu");
        CANONICAL_CATEGORIES.put(12, "Tráng miệng");
        CANONICAL_CATEGORIES.put(13, "Đồ uống");

        // Inventory Items 1-20
        CANONICAL_ITEMS.put(1, "Thịt bò thăn thái sẵn");
        CANONICAL_ITEM_UNITS.put(1, "khay (500g)");

        CANONICAL_ITEMS.put(2, "Thịt heo ba chỉ rút sườn");
        CANONICAL_ITEM_UNITS.put(2, "kg");

        CANONICAL_ITEMS.put(3, "Gà ta làm sạch");
        CANONICAL_ITEM_UNITS.put(3, "con");

        CANONICAL_ITEMS.put(4, "Tôm sú tươi sống");
        CANONICAL_ITEM_UNITS.put(4, "kg");

        CANONICAL_ITEMS.put(5, "Cua biển Cà Mau");
        CANONICAL_ITEM_UNITS.put(5, "kg");

        CANONICAL_ITEMS.put(6, "Rau thơm các loại");
        CANONICAL_ITEM_UNITS.put(6, "kg");

        CANONICAL_ITEMS.put(7, "Hành hoa & Ngò rí");
        CANONICAL_ITEM_UNITS.put(7, "kg");

        CANONICAL_ITEMS.put(8, "Cà chua Đà Lạt");
        CANONICAL_ITEM_UNITS.put(8, "kg");

        CANONICAL_ITEMS.put(9, "Bánh phở tươi Hà Nội");
        CANONICAL_ITEM_UNITS.put(9, "kg");

        CANONICAL_ITEMS.put(10, "Mì trứng tươi sợi vàng");
        CANONICAL_ITEM_UNITS.put(10, "kg");

        CANONICAL_ITEMS.put(11, "Gạo ST25 Ông Cua");
        CANONICAL_ITEM_UNITS.put(11, "bao (25kg)");

        CANONICAL_ITEMS.put(12, "Bia Tiger lon (thùng 24 lon)");
        CANONICAL_ITEM_UNITS.put(12, "thùng");

        CANONICAL_ITEMS.put(13, "Bia Heineken lon (thùng 24 lon)");
        CANONICAL_ITEM_UNITS.put(13, "thùng");

        CANONICAL_ITEMS.put(14, "Coca Cola lon (thùng 24 lon)");
        CANONICAL_ITEM_UNITS.put(14, "thùng");

        CANONICAL_ITEMS.put(15, "Nước khoáng Lavie 500ml (thùng 24 chai)");
        CANONICAL_ITEM_UNITS.put(15, "thùng");

        CANONICAL_ITEMS.put(16, "Dầu thực vật Neptune Gold (5L)");
        CANONICAL_ITEM_UNITS.put(16, "can");

        CANONICAL_ITEMS.put(17, "Nước mắm Phú Quốc truyền thống (1L)");
        CANONICAL_ITEM_UNITS.put(17, "chai");

        CANONICAL_ITEMS.put(18, "Đường cát Biên Hòa (1kg)");
        CANONICAL_ITEM_UNITS.put(18, "gói");

        CANONICAL_ITEMS.put(19, "Muối tinh sấy Bạc Liêu (1kg)");
        CANONICAL_ITEM_UNITS.put(19, "gói");

        CANONICAL_ITEMS.put(20, "Hộp bã mía Takeaway size L (50 cái)");
        CANONICAL_ITEM_UNITS.put(20, "lốc");

        // Suppliers
        CANONICAL_SUPPLIERS.put(1, "Công ty TNHH Thực phẩm Sạch ABC");
        CANONICAL_SUPPLIERS.put(2, "Nhà phân phối bia Tiger");
        CANONICAL_SUPPLIERS.put(3, "Công ty Nước Giải Khát Việt Nam");
        CANONICAL_SUPPLIERS.put(4, "Chợ đầu mối Nông sản Thủ Đức");

        // Tables
        for (int i = 1; i <= 12; i++) {
            CANONICAL_TABLES.put(i, String.format("Bàn %02d", i));
        }
        CANONICAL_TABLES.put(13, "Bàn VIP 01");
        CANONICAL_TABLES.put(14, "Bàn VIP 02");
        CANONICAL_TABLES.put(15, "Sân ngoài 01");

        // Menu items 1-53
        String[] menuNames = {
            "Phở bò đặc biệt Hà Nội", "Phở gà đồi lá chanh", "Hủ tiếu Nam Vang sườn tôm", "Bánh canh cua biển giò heo",
            "Bún bò Huế giò heo chả cua", "Bún chả Hà Nội nướng than hoa", "Bún đậu mắm tôm thập cẩm", "Bún riêu cua đồng bắp bò",
            "Mì Quảng tôm thịt trứng cút", "Cao lầu Hội An thịt xíu", "Mì xào giòn hải sản rau củ", "Mì vịt tiềm thuốc bắc hạt sen",
            "Cơm tấm sườn bì chả trứng ốp la", "Cơm gà Hải Nam luộc chấm gừng", "Cơm rang dưa bò giòn rụm", "Cơm niêu cá kho tộ đậm đà",
            "Bánh mì thịt nướng sả ớt", "Bánh mì chảo xíu mại trứng ốp", "Bánh mì pate chả lụa truyền thống", "Bánh mì bò kho tiêu xanh thơm lừng",
            "Bánh xèo miền Tây tôm nhảy thịt ba chỉ", "Bánh khọt Vũng Tàu tôm tươi", "Bánh bèo chén tôm chấy da heo chiên giòn", "Bánh cuốn nóng thịt băm mộc nhĩ", "Bánh bột lọc Huế nhân tôm thịt",
            "Nem rán Hà Nội giòn rụm nhân thịt", "Chả giò rế tôm cua sốt mayonnaise", "Gà rán sốt chua ngọt giòn tan", "Khoai tây chiên lắc phô mai",
            "Gỏi cuốn tôm thịt chấm tương đen", "Gỏi ngó sen tôm thịt thanh mát", "Gỏi bò bóp thấu chua cay hạt điều", "Gỏi xoài xanh khô cá lóc",
            "Bò lúc lắc khoai tây sốt tiêu đen", "Sườn xào chua ngọt kiểu truyền thống", "Cá chẽm chiên giòn sốt mắm xoài", "Mực ống xào sa tế cay giòn",
            "Tôm càng xanh hấp nước dừa xiêm", "Cua rang me sốt chua ngọt đậm đà", "Nghêu hấp sả ớt cay nồng nước ngọt", "Ốc hương xào bơ tỏi thơm lừng",
            "Lẩu mắm miền Tây cá linh bông điên điển", "Lẩu thái hải sản chua cay thập cẩm", "Lẩu gà lá é đặc sản Phú Yên", "Lẩu cá kèo lá giang thanh mát",
            "Chè khúc bạch hạnh nhân", "Sữa chua mít trân châu", "Kem dừa Tràng Tiền",
            "Trà sen vàng macchiato", "Trà đào cam sả", "Cà phê sữa đá pha phin", "Nước suối khoáng"
        };
        for (int i = 0; i < menuNames.length; i++) {
            CANONICAL_MENU_ITEMS.put(i + 1, menuNames[i]);
        }
    }

    private DatabaseEncodingFixer() {}

    /**
     * Kiểm tra xem chuỗi có dấu hiệu bị mã hóa Mojibake UTF-8 -> Windows-1252 / ISO-8859-1 hay không.
     */
    public static boolean isMojibake(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        // Các chuỗi đặc trưng khi UTF-8 tiếng Việt bị decode bằng Windows-1252 / ISO-8859-1 hoặc mất dấu thành ? / 
        return s.contains("?") || s.contains("\uFFFD") || s.contains("")
            || s.contains("á»") || s.contains("áº") || s.contains("Ã¡") || s.contains("Ã ")
            || s.contains("Ã£") || s.contains("Ã³") || s.contains("Ã²") || s.contains("Ã©")
            || s.contains("Ã¨") || s.contains("Ãª") || s.contains("Ã­") || s.contains("Ã¬")
            || s.contains("Ã½") || s.contains("Ã¹") || s.contains("Ãº") || s.contains("Äƒ")
            || s.contains("Ä‘") || s.contains("Æ°") || s.contains("Æ¡") || s.contains("Ã”")
            || s.contains("thÃ¹ng") || s.contains("gÃ³i") || s.contains("lÃ³c") || s.contains("ÄÆ°á»")
            || s.contains("Ä\u0083") || s.contains("Ä\u0091") || s.contains("Ã\u0082") || s.contains("â€™")
            || s.contains("Nguyá»") || s.contains("Tráº§n") || s.contains("LÃª");
    }

    /**
     * Sửa chuỗi bị Mojibake về lại tiếng Việt có dấu chuẩn Unicode UTF-8.
     */
    public static String fixMojibake(String s) {
        if (s == null || !isMojibake(s)) return s;

        // Thử giải mã qua Windows-1252 bytes -> UTF-8
        try {
            byte[] bytes = s.getBytes(Charset.forName("windows-1252"));
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            if (!decoded.contains("") && !decoded.equals(s) && containsVietnamese(decoded)) {
                return decoded;
            }
        } catch (Exception ignored) {}

        // Thử qua ISO-8859-1 bytes -> UTF-8
        try {
            byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            if (!decoded.contains("") && !decoded.equals(s) && containsVietnamese(decoded)) {
                return decoded;
            }
        } catch (Exception ignored) {}

        return s;
    }

    private static boolean containsVietnamese(String s) {
        return s.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ].*");
    }

    /**
     * Quét và tự động sửa chữa toàn bộ bảng trong cơ sở dữ liệu nếu phát hiện dữ liệu tiếng Việt bị lỗi font (Mojibake).
     */
    public static void autoFixDatabaseSync() {
        log.info("Checking database for font encoding (mojibake) issues...");
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            if (conn == null || conn.isClosed()) return;

            int fixedCount = 0;

            // 1. Kiểm tra users
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, username, full_name FROM users")) {
                Map<Integer, String> updates = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String fullName = rs.getString("full_name");
                    if (CANONICAL_USERS.containsKey(username)) {
                        String expected = CANONICAL_USERS.get(username);
                        if (!expected.equals(fullName)) {
                            updates.put(id, expected);
                        }
                    } else if (isMojibake(fullName)) {
                        updates.put(id, fixMojibake(fullName));
                    }
                }
                for (Map.Entry<Integer, String> e : updates.entrySet()) {
                    try (PreparedStatement pst = conn.prepareStatement("UPDATE users SET full_name = ? WHERE id = ?")) {
                        pst.setString(1, e.getValue());
                        pst.setInt(2, e.getKey());
                        pst.executeUpdate();
                        fixedCount++;
                        log.info("Fixed user #{} -> {}", e.getKey(), e.getValue());
                    }
                }
            }

            // 2. Kiểm tra inventory_items
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name, unit, category FROM inventory_items")) {
                Map<Integer, String[]> updates = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String unit = rs.getString("unit");
                    String cat = rs.getString("category");

                    if (CANONICAL_ITEMS.containsKey(id)) {
                        String expName = CANONICAL_ITEMS.get(id);
                        String expUnit = CANONICAL_ITEM_UNITS.getOrDefault(id, unit);
                        if (!expName.equals(name) || !expUnit.equals(unit) || isMojibake(cat)) {
                            updates.put(id, new String[]{expName, expUnit, fixMojibake(cat)});
                        }
                    } else if (isMojibake(name) || isMojibake(unit) || isMojibake(cat)) {
                        updates.put(id, new String[]{fixMojibake(name), fixMojibake(unit), fixMojibake(cat)});
                    }
                }
                for (Map.Entry<Integer, String[]> e : updates.entrySet()) {
                    try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE inventory_items SET name = ?, unit = ?, category = ? WHERE id = ?")) {
                        pst.setString(1, e.getValue()[0]);
                        pst.setString(2, e.getValue()[1]);
                        pst.setString(3, e.getValue()[2]);
                        pst.setInt(4, e.getKey());
                        pst.executeUpdate();
                        fixedCount++;
                    }
                }
                if (!updates.isEmpty()) {
                    log.info("Fixed {} inventory items with font issues", updates.size());
                }
            }

            // 3. Kiểm tra categories
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name FROM categories")) {
                Map<Integer, String> updates = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    if (CANONICAL_CATEGORIES.containsKey(id)) {
                        String expected = CANONICAL_CATEGORIES.get(id);
                        if (!expected.equals(name)) {
                            updates.put(id, expected);
                        }
                    } else if (isMojibake(name)) {
                        updates.put(id, fixMojibake(name));
                    }
                }
                for (Map.Entry<Integer, String> e : updates.entrySet()) {
                    try (PreparedStatement pst = conn.prepareStatement("UPDATE categories SET name = ? WHERE id = ?")) {
                        pst.setString(1, e.getValue());
                        pst.setInt(2, e.getKey());
                        pst.executeUpdate();
                        fixedCount++;
                    }
                }
            }

            // 4. Kiểm tra menu_items
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name FROM menu_items")) {
                Map<Integer, String> updates = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    if (CANONICAL_MENU_ITEMS.containsKey(id)) {
                        String expected = CANONICAL_MENU_ITEMS.get(id);
                        if (!expected.equals(name)) {
                            updates.put(id, expected);
                        }
                    } else if (isMojibake(name)) {
                        updates.put(id, fixMojibake(name));
                    }
                }
                for (Map.Entry<Integer, String> e : updates.entrySet()) {
                    try (PreparedStatement pst = conn.prepareStatement("UPDATE menu_items SET name = ? WHERE id = ?")) {
                        pst.setString(1, e.getValue());
                        pst.setInt(2, e.getKey());
                        pst.executeUpdate();
                        fixedCount++;
                    }
                }
            }

            // 5. Kiểm tra dining_tables
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name FROM dining_tables")) {
                Map<Integer, String> updates = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    if (CANONICAL_TABLES.containsKey(id)) {
                        String expected = CANONICAL_TABLES.get(id);
                        if (!expected.equals(name)) {
                            updates.put(id, expected);
                        }
                    } else if (isMojibake(name)) {
                        updates.put(id, fixMojibake(name));
                    }
                }
                for (Map.Entry<Integer, String> e : updates.entrySet()) {
                    try (PreparedStatement pst = conn.prepareStatement("UPDATE dining_tables SET name = ? WHERE id = ?")) {
                        pst.setString(1, e.getValue());
                        pst.setInt(2, e.getKey());
                        pst.executeUpdate();
                        fixedCount++;
                    }
                }
            }

            // 6. Kiểm tra suppliers
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name, address FROM suppliers")) {
                Map<Integer, String[]> updates = new HashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String addr = rs.getString("address");
                    if (CANONICAL_SUPPLIERS.containsKey(id)) {
                        String expected = CANONICAL_SUPPLIERS.get(id);
                        if (!expected.equals(name) || isMojibake(addr)) {
                            updates.put(id, new String[]{expected, fixMojibake(addr)});
                        }
                    } else if (isMojibake(name) || isMojibake(addr)) {
                        updates.put(id, new String[]{fixMojibake(name), fixMojibake(addr)});
                    }
                }
                for (Map.Entry<Integer, String[]> e : updates.entrySet()) {
                    try (PreparedStatement pst = conn.prepareStatement("UPDATE suppliers SET name = ?, address = ? WHERE id = ?")) {
                        pst.setString(1, e.getValue()[0]);
                        pst.setString(2, e.getValue()[1]);
                        pst.setInt(3, e.getKey());
                        pst.executeUpdate();
                        fixedCount++;
                    }
                }
            }

            if (fixedCount > 0) {
                log.info("Database font encoding successfully repaired: {} records updated to clean Vietnamese.", fixedCount);
            } else {
                log.info("Database font check completed: all records are clean Vietnamese.");
            }
        } catch (Exception ex) {
            log.warn("Notice: DatabaseEncodingFixer encountered an exception: {}", ex.getMessage());
        }
    }

    /**
     * Chạy tự động sửa lỗi font ở chế độ bất đồng bộ nền (không làm nghẽn khởi động giao diện).
     */
    public static void autoFixDatabaseAsync() {
        Thread t = new Thread(DatabaseEncodingFixer::autoFixDatabaseSync, "DatabaseEncodingFixerThread");
        t.setDaemon(true);
        t.start();
    }
}
