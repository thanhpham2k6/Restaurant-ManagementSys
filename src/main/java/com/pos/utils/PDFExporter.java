package com.pos.utils;

import com.pos.model.Order;
import com.pos.model.OrderDetail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;

public final class PDFExporter {
    private static final Logger log = LogManager.getLogger(PDFExporter.class);

    private PDFExporter() {}

    /**
     * Chuyển chuỗi tiếng Việt có dấu sang ASCII không dấu để tương thích với Helvetica (WinAnsiEncoding)
     */
    public static String toAscii(String input) {
        if (input == null) return "";
        // Thay thế ký tự đ/Đ
        String s = input.replace('đ', 'd').replace('Đ', 'D');
        // Tách các dấu thanh
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        // Xóa dấu thanh
        s = s.replaceAll("\\p{M}", "");
        // Thay ký hiệu tiền tệ sang VND
        s = s.replace("₫", "VND").replace("đ", "VND");
        // Giữ lại ký tự ASCII printable thông thường
        s = s.replaceAll("[^\\x20-\\x7E]", " ");
        return s.trim();
    }

    private static String formatMoneyAscii(BigDecimal amount) {
        if (amount == null) return "0 VND";
        return String.format("%,d VND", amount.longValue());
    }

    private static void drawCell(PDPageContentStream cs, PDType1Font font, float fontSize, float x, float y, String text) throws IOException {
        String clean = toAscii(text);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(clean);
        cs.endText();
    }

    private static void drawCellRight(PDPageContentStream cs, PDType1Font font, float fontSize, float rightX, float y, String text) throws IOException {
        String clean = toAscii(text);
        float width = 0;
        try {
            width = font.getStringWidth(clean) / 1000f * fontSize;
        } catch (Exception e) {
            width = clean.length() * fontSize * 0.5f;
        }
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(rightX - width, y);
        cs.showText(clean);
        cs.endText();
    }

    /**
     * Xuất hóa đơn PDF cho đơn hàng đã thanh toán với các cột căn chỉnh tọa độ chính xác.
     * @return đường dẫn file PDF đã tạo, hoặc null nếu lỗi
     */
    public static String export(Order order) {
        String fileName = "invoice_" + order.getId() + "_" + System.currentTimeMillis() + ".pdf";
        File outputDir = new File("invoices");
        if (!outputDir.exists()) outputDir.mkdirs();
        File outputFile = new File(outputDir, fileName);

        // Bổ sung thông tin Bàn và Thu ngân nếu chưa nạp đầy đủ
        String tableName = "-";
        if (order.getTable() != null) {
            if (order.getTable().getName() != null && !order.getTable().getName().isBlank() && !"null".equalsIgnoreCase(order.getTable().getName())) {
                tableName = order.getTable().getName();
            } else if (order.getTable().getId() > 0) {
                try {
                    var tables = new com.pos.dao.TableDAO().findAll();
                    for (var t : tables) {
                        if (t.getId() == order.getTable().getId()) {
                            tableName = t.getName();
                            order.setTable(t);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
                if ("-".equals(tableName)) tableName = "Ban #" + order.getTable().getId();
            }
        }

        String cashierName = "-";
        if (order.getCashier() != null) {
            if (order.getCashier().getFullName() != null && !order.getCashier().getFullName().isBlank() && !"null".equalsIgnoreCase(order.getCashier().getFullName())) {
                cashierName = order.getCashier().getFullName();
            } else if (order.getCashier().getUsername() != null && !order.getCashier().getUsername().isBlank()) {
                cashierName = order.getCashier().getUsername();
            } else if (order.getCashier().getId() > 0) {
                try {
                    var users = new com.pos.dao.UserDAO().findAll();
                    for (var u : users) {
                        if (u.getId() == order.getCashier().getId()) {
                            cashierName = u.getFullName();
                            order.setCashier(u);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
                if ("-".equals(cashierName)) cashierName = "Thu ngan #" + order.getCashier().getId();
            }
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font font    = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float y = 780f;
                float leftMargin = 50f;
                float rightMargin = 545f;

                // Tọa độ X cho từng cột trong bảng (cố định, chuẩn xác không bị lệch)
                float colSTT = 50f;
                float colName = 85f;
                float colQtyRight = 325f;
                float colPriceRight = 430f;
                float colTotalRight = 545f;

                // Tiêu đề
                drawCell(cs, fontBold, 18, leftMargin, y, "HOA DON THANH TOAN");
                y -= 25;

                drawCell(cs, font, 10, leftMargin, y, "So don: #" + order.getId());
                y -= 15;

                drawCell(cs, font, 10, leftMargin, y, "Ban: " + tableName);
                y -= 15;

                drawCell(cs, font, 10, leftMargin, y, "Thu ngan: " + cashierName);
                y -= 15;

                String timeStr = order.getPaidAt() != null ? DateUtils.format(order.getPaidAt()) : DateUtils.format(order.getCreatedAt());
                drawCell(cs, font, 10, leftMargin, y, "Thoi gian: " + (timeStr != null ? timeStr : "-"));
                y -= 25;

                // Header table với tọa độ từng cột riêng biệt
                drawCell(cs, fontBold, 10, colSTT, y, "STT");
                drawCell(cs, fontBold, 10, colName, y, "Ten mon");
                drawCellRight(cs, fontBold, 10, colQtyRight, y, "SL");
                drawCellRight(cs, fontBold, 10, colPriceRight, y, "Don gia");
                drawCellRight(cs, fontBold, 10, colTotalRight, y, "Thanh tien");
                y -= 6;

                // Đường kẻ ngăn cách header
                cs.moveTo(leftMargin, y);
                cs.lineTo(rightMargin, y);
                cs.stroke();
                y -= 14;

                // Items list
                int stt = 1;
                BigDecimal subtotal = BigDecimal.ZERO;
                for (OrderDetail d : order.getDetails()) {
                    if (d.getStatus() == com.pos.model.ItemStatus.REJECTED) continue;
                    BigDecimal lineTotal = d.getSubtotal();
                    subtotal = subtotal.add(lineTotal);
                    String itemName = d.getItem() != null ? d.getItem().getName() : "?";

                    drawCell(cs, font, 9, colSTT, y, String.valueOf(stt++));
                    drawCell(cs, font, 9, colName, y, truncate(itemName, 32));
                    drawCellRight(cs, font, 9, colQtyRight, y, String.valueOf(d.getQuantity()));
                    drawCellRight(cs, font, 9, colPriceRight, y, formatMoneyAscii(d.getUnitPrice()));
                    drawCellRight(cs, font, 9, colTotalRight, y, formatMoneyAscii(lineTotal));

                    y -= 15;
                    if (y < 90) break; // Giới hạn an toàn lề dưới trang
                }

                y -= 2;
                // Đường kẻ chân bảng
                cs.moveTo(leftMargin, y);
                cs.lineTo(rightMargin, y);
                cs.stroke();
                y -= 18;

                BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal afterDiscount = subtotal.subtract(discount);
                BigDecimal vatRate = order.getVatRate() != null ? order.getVatRate() : new BigDecimal("0.10");
                BigDecimal vat = afterDiscount.multiply(vatRate);
                BigDecimal total = afterDiscount.add(vat);

                String[][] summaries = {
                    {"Tong cong:", formatMoneyAscii(subtotal), "false"},
                    {"Chiet khau (" + String.valueOf(order.getDiscountType()) + "):", "-" + formatMoneyAscii(discount), "false"},
                    {"VAT (" + vatRate.multiply(BigDecimal.valueOf(100)).intValue() + "%):", "+" + formatMoneyAscii(vat), "false"},
                    {"TONG THANH TOAN:", formatMoneyAscii(total), "true"},
                    {"Phuong thuc:", order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "-", "false"}
                };

                for (String[] row : summaries) {
                    boolean isBold = "true".equals(row[2]);
                    PDType1Font curFont = isBold ? fontBold : font;
                    float fSize = isBold ? 11f : 10f;

                    drawCell(cs, curFont, fSize, 310f, y, row[0]);
                    drawCellRight(cs, curFont, fSize, rightMargin, y, row[1]);
                    y -= 16;
                }

                // Lời cảm ơn chân trang
                drawCell(cs, font, 9, leftMargin, 45f, "Cam on quy khach! Hen gap lai.");
            }

            doc.save(outputFile);
            log.info("Invoice saved: {}", outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to export PDF", e);
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 2)) + "..";
    }
}
