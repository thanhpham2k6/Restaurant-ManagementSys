package com.pos.utils;

import com.pos.model.Order;
import com.pos.model.OrderDetail;
import com.pos.model.Shift;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.print.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Service in hóa đơn nhiệt qua giao thức chuẩn ESC/POS.
 * Hỗ trợ in qua javax.print, tự động fallback lưu file log thermal_receipt.bin / text
 * nếu không kết nối máy in vật lý.
 */
public class PrinterService {
    private static final Logger log = LogManager.getLogger(PrinterService.class);
    private static final DateTimeFormatter DTM_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Lệnh ESC/POS cơ bản
    private static final byte[] CMD_INIT = new byte[]{0x1B, 0x40};               // ESC @
    private static final byte[] CMD_ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};   // ESC a 0
    private static final byte[] CMD_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01}; // ESC a 1
    private static final byte[] CMD_ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};  // ESC a 2
    private static final byte[] CMD_BOLD_ON = new byte[]{0x1B, 0x45, 0x01};      // ESC E 1
    private static final byte[] CMD_BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};     // ESC E 0
    private static final byte[] CMD_DOUBLE_SIZE = new byte[]{0x1D, 0x21, 0x11};  // GS ! 17
    private static final byte[] CMD_NORMAL_SIZE = new byte[]{0x1D, 0x21, 0x00};  // GS ! 0
    private static final byte[] CMD_CUT_PAPER = new byte[]{0x1D, 0x56, 0x00};    // GS V 0
    private static final byte[] CMD_OPEN_DRAWER = new byte[]{0x1B, 0x70, 0x00, 0x19, (byte) 0xFA}; // ESC p
    private static final byte[] LF = new byte[]{0x0A};

    /**
     * In hóa đơn bán lẻ ESC/POS 80mm
     */
    public static void printReceipt(Order order) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Charset charset = Charset.forName("CP850"); // Chuẩn máy in nhiệt phổ biến

                out.write(CMD_INIT);
                out.write(CMD_OPEN_DRAWER); // Mở két tiền

                // HEADER
                out.write(CMD_ALIGN_CENTER);
                out.write(CMD_BOLD_ON);
                out.write(CMD_DOUBLE_SIZE);
                out.write(toAscii("NHA HANG POS SYSTEM\n").getBytes(charset));
                out.write(CMD_NORMAL_SIZE);
                out.write(toAscii("Dia chi: 123 Nguyen Trai, Q.1, TP.HCM\n").getBytes(charset));
                out.write(toAscii("Hotline: 1900 6868\n").getBytes(charset));
                out.write(toAscii("------------------------------------------\n").getBytes(charset));
                out.write(CMD_BOLD_ON);
                out.write(toAscii("PHIEU THANH TOAN\n").getBytes(charset));
                out.write(CMD_BOLD_OFF);

                String orderTypeStr = order.getOrderType() != null ? order.getOrderType().getDisplayName() : "Tai ban";
                String tableStr = order.getTable() != null ? order.getTable().getName() : (order.getTableName() != null ? order.getTableName() : "Mang ve");
                out.write(toAscii(String.format("HD: #%d  |  Loai: %s\n", order.getId(), toAscii(orderTypeStr))).getBytes(charset));
                out.write(toAscii(String.format("Vi tri: %s\n", toAscii(tableStr))).getBytes(charset));
                out.write(toAscii(String.format("Gio: %s\n", order.getCreatedAt() != null ? order.getCreatedAt().format(DTM_FMT) : "")).getBytes(charset));
                if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
                    out.write(toAscii(String.format("Khach hang: %s\n", toAscii(order.getCustomerName()))).getBytes(charset));
                }
                out.write(toAscii("------------------------------------------\n").getBytes(charset));

                // CHI TIẾT MÓN
                out.write(CMD_ALIGN_LEFT);
                out.write(toAscii(String.format("%-22s %3s %14s\n", "Ten mon", "SL", "Thanh tien")).getBytes(charset));
                out.write(toAscii("------------------------------------------\n").getBytes(charset));

                for (OrderDetail d : order.getDetails()) {
                    String name = toAscii(d.getItem().getName());
                    if (name.length() > 22) name = name.substring(0, 21) + ".";
                    long amount = d.getUnitPrice().multiply(java.math.BigDecimal.valueOf(d.getQuantity())).longValue();
                    out.write(toAscii(String.format("%-22s %3d %,14d\n", name, d.getQuantity(), amount)).getBytes(charset));
                }

                out.write(toAscii("------------------------------------------\n").getBytes(charset));
                out.write(CMD_ALIGN_RIGHT);
                out.write(toAscii(String.format("Tam tinh:   %,16d d\n", order.getSubtotal().longValue())).getBytes(charset));
                if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() > 0) {
                    out.write(toAscii(String.format("Giam gia:  -%,16d d\n", order.getDiscountAmount().longValue())).getBytes(charset));
                }
                if (order.getDeliveryFee() != null && order.getDeliveryFee().signum() > 0) {
                    out.write(toAscii(String.format("Phi giao hang: %,14d d\n", order.getDeliveryFee().longValue())).getBytes(charset));
                }
                out.write(CMD_BOLD_ON);
                out.write(toAscii(String.format("TONG CONG:  %,16d d\n", order.getTotalAmount().longValue())).getBytes(charset));
                out.write(CMD_BOLD_OFF);

                String payMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "TIEN MAT";
                out.write(toAscii(String.format("Thanh toan qua: %s\n", payMethod)).getBytes(charset));

                // FOOTER
                out.write(LF);
                out.write(CMD_ALIGN_CENTER);
                out.write(toAscii("CAM ON QUY KHACH VA HEN GAP LAI!\n").getBytes(charset));
                out.write(toAscii("Wifi: POS_Guest | Pass: 88888888\n").getBytes(charset));
                out.write(LF);
                out.write(LF);
                out.write(CMD_CUT_PAPER);

                byte[] rawData = out.toByteArray();
                sendToHardwarePrinter(rawData, "Receipt_" + order.getId());

            } catch (Exception e) {
                log.error("Lỗi khi in ESC/POS cho đơn hàng #" + order.getId(), e);
            }
        }).start();
    }

    /**
     * In báo cáo đóng ca (Z-Report)
     */
    public static void printZReport(Shift shift) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Charset charset = Charset.forName("CP850");

                out.write(CMD_INIT);
                out.write(CMD_ALIGN_CENTER);
                out.write(CMD_BOLD_ON);
                out.write(CMD_DOUBLE_SIZE);
                out.write(toAscii("BAO CAO DONG CA (Z-REPORT)\n").getBytes(charset));
                out.write(CMD_NORMAL_SIZE);
                out.write(CMD_BOLD_OFF);
                out.write(toAscii("------------------------------------------\n").getBytes(charset));
                out.write(toAscii(String.format("Ma ca: #%d\n", shift.getId())).getBytes(charset));
                out.write(toAscii(String.format("Nhan vien: %s\n", toAscii(shift.getUserFullName() != null ? shift.getUserFullName() : "N/A"))).getBytes(charset));
                out.write(toAscii(String.format("Gio mo: %s\n", shift.getStartTime() != null ? shift.getStartTime().format(DTM_FMT) : "")).getBytes(charset));
                out.write(toAscii(String.format("Gio dong: %s\n", shift.getEndTime() != null ? shift.getEndTime().format(DTM_FMT) : "")).getBytes(charset));
                out.write(toAscii("------------------------------------------\n").getBytes(charset));

                out.write(CMD_ALIGN_LEFT);
                out.write(toAscii(String.format("Tien mat dau ca:     %,15d d\n", shift.getInitialCash().longValue())).getBytes(charset));
                out.write(toAscii(String.format("Doanh thu tien mat:  %,15d d\n", shift.getCashSales().longValue())).getBytes(charset));
                out.write(toAscii(String.format("Doanh thu CK/The:    %,15d d\n", shift.getTransferSales().longValue())).getBytes(charset));
                long totalRevenue = shift.getCashSales().add(shift.getTransferSales()).longValue();
                out.write(toAscii(String.format("Tong doanh thu:      %,15d d\n", totalRevenue)).getBytes(charset));
                out.write(toAscii("------------------------------------------\n").getBytes(charset));

                long expectedCash = shift.getInitialCash().add(shift.getCashSales()).longValue();
                long actualCash = shift.getActualCash() != null ? shift.getActualCash().longValue() : 0;
                long diff = shift.getDifferenceCash() != null ? shift.getDifferenceCash().longValue() : 0;

                out.write(toAscii(String.format("Tien mat ly thuyet:  %,15d d\n", expectedCash)).getBytes(charset));
                out.write(toAscii(String.format("Tien mat thuc te:    %,15d d\n", actualCash)).getBytes(charset));
                out.write(CMD_BOLD_ON);
                out.write(toAscii(String.format("Chenh lech:          %,15d d\n", diff)).getBytes(charset));
                out.write(CMD_BOLD_OFF);

                if (shift.getNote() != null && !shift.getNote().isBlank()) {
                    out.write(toAscii(String.format("Ghi chu: %s\n", toAscii(shift.getNote()))).getBytes(charset));
                }

                out.write(LF);
                out.write(CMD_ALIGN_CENTER);
                out.write(toAscii("Chu ky thu ngan          Chu ky quan ly\n\n\n\n").getBytes(charset));
                out.write(CMD_CUT_PAPER);

                byte[] rawData = out.toByteArray();
                sendToHardwarePrinter(rawData, "ZReport_Shift_" + shift.getId());

            } catch (Exception e) {
                log.error("Lỗi khi in Z-Report cho ca #" + shift.getId(), e);
            }
        }).start();
    }

    /**
     * Gửi byte array tới máy in POS qua Java Print Service, hoặc lưu ra file
     */
    private static void sendToHardwarePrinter(byte[] rawData, String jobName) {
        try {
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

            if (defaultService != null) {
                Doc doc = new SimpleDoc(rawData, flavor, null);
                DocPrintJob job = defaultService.createPrintJob();
                job.print(doc, null);
                log.info("Đã gửi dữ liệu in thành công tới máy in mặc định: " + defaultService.getName());
                return;
            }
        } catch (Exception ex) {
            log.warn("Không tìm thấy hoặc không in được tới máy in vật lý: " + ex.getMessage());
        }

        // Fallback lưu ra file spool trong thư mục invoices/ để mô phỏng và kiểm tra
        try {
            File dir = new File("invoices/escpos_spool");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, jobName + "_" + System.currentTimeMillis() + ".txt");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(rawData);
            }
            log.info("Máy in ESC/POS mô phỏng: Đã lưu bản in tại " + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Lỗi khi ghi file ESC/POS fallback", e);
        }
    }

    /**
     * Chuyển chuỗi tiếng Việt có dấu sang không dấu để máy in nhiệt không bị lỗi font
     */
    private static String toAscii(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String ascii = pattern.matcher(nfd).replaceAll("");
        return ascii.replace('đ', 'd').replace('Đ', 'D');
    }
}
