package com.pos.utils;

import javax.swing.JTable;
import javax.swing.table.TableRowSorter;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Tiện ích hỗ trợ sắp xếp thông minh cho JTable (Smart Table Sorter).
 * Tự động nhận diện và so sánh các cột số (ID, số lượng, đơn giá, tổng tiền, điểm tích lũy) theo giá trị số thực tế,
 * tránh lỗi sắp xếp chuỗi ký tự (như 1, 10, 2 thay vì 1, 2, 10), đồng thời hỗ trợ sắp xếp tiếng Việt chuẩn Unicode.
 */
public class TableUtils {
    private static final Collator VI_COLLATOR = Collator.getInstance(new Locale("vi", "VN"));

    public static void applySmartSorting(JTable table) {
        table.setAutoCreateRowSorter(true);
        if (table.getRowSorter() instanceof TableRowSorter<?> sorter) {
            int colCount = table.getModel().getColumnCount();
            for (int col = 0; col < colCount; col++) {
                sorter.setComparator(col, new SmartCellComparator());
            }
        }
    }

    public static class SmartCellComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;

            if (o1 instanceof Number n1 && o2 instanceof Number n2) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            }

            String s1 = o1.toString().trim();
            String s2 = o2.toString().trim();

            if (s1.equalsIgnoreCase(s2)) return 0;

            Double d1 = parseNumeric(s1);
            Double d2 = parseNumeric(s2);

            if (d1 != null && d2 != null) {
                return Double.compare(d1, d2);
            }

            return VI_COLLATOR.compare(s1, s2);
        }

        private Double parseNumeric(String s) {
            if (s == null || s.isEmpty() || s.equals("-")) return null;

            String clean = s;
            // Xử lý tiền tố mã: "#12", "RES-12", "RES_12"
            if (clean.startsWith("#")) {
                clean = clean.substring(1).trim();
            } else if (clean.toUpperCase().startsWith("RES-")) {
                clean = clean.substring(4).trim();
            } else if (clean.toUpperCase().startsWith("RES_")) {
                clean = clean.substring(4).trim();
            }

            // Xử lý dạng tỉ lệ "3 / 100" -> lấy số lượng đã dùng 3
            if (clean.contains("/")) {
                String first = clean.split("/")[0].trim();
                try {
                    return Double.parseDouble(first);
                } catch (Exception ignored) {}
            }

            // Loại bỏ ký hiệu tiền tệ và các đơn vị tính kèm theo
            clean = clean.replaceAll("(?i)[đđ₫]|VND|VNĐ|người|nguoi|can|kg|con|thùng|thung|bao|lốc|loc|gói|goi|chai|lon|dĩa|dia|tô|to|ly|%|KB|MB|GB", "").trim();

            if (clean.startsWith("+")) {
                clean = clean.substring(1).trim();
            }

            // Trường hợp tiền tệ định dạng Việt Nam dùng dấu chấm phân tách hàng nghìn (VD: "120.000", "1.500.000")
            if (clean.matches("^[+-]?\\d{1,3}(\\.\\d{3})+$")) {
                clean = clean.replace(".", "");
            } else if (clean.matches("^[+-]?\\d{1,3}(,\\d{3})+(\\.\\d+)?$")) {
                clean = clean.replace(",", "");
            }

            try {
                return Double.parseDouble(clean);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
