package service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import utils.CurrencyFormat;
import model.Invoice;
import model.InvoiceLine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

public final class PDFExporter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportInvoice(Invoice invoice, Path outputFile, Path fontFile)
            throws IOException {

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType0Font font = PDType0Font.load(document, fontFile.toFile());

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 800;

                y = writeLine(content, font, 18, 50, y, "HÓA ĐƠN THANH TOÁN");
                y = writeLine(content, font, 11, 50, y - 15, "Mã hóa đơn: " + invoice.getCode());
                y = writeLine(content, font, 11, 50, y - 15,
                        "Ngày lập: " + invoice.getCreatedAt().format(DATE_TIME));
                if (!invoice.getTableName().isEmpty()) {
                    y = writeLine(content, font, 11, 50, y - 15, "Bàn: " + invoice.getTableName());
                }
                y = writeLine(content, font, 11, 50, y - 15, "Khách hàng: " + invoice.getCustomerName());

                y -= 25;
                y = writeLine(content, font, 11, 50, y, "Món ăn");
                y = writeLine(content, font, 11, 270, y, "SL");
                y = writeLine(content, font, 11, 330, y, "Thành tiền");

                y -= 18;
                for (InvoiceLine line : invoice.getItems()) {
                    y = writeLine(content, font, 10, 50, y, line.getName());
                    y = writeLine(content, font, 10, 270, y,
                            String.valueOf(line.getQuantity()));
                    y = writeLine(content, font, 10, 330, y,
                            CurrencyFormat.vnd(line.getLineTotal()));
                    y -= 16;
                }

                // Phần tổng tiền: nhãn ở cột trái, số tiền thẳng cột với "Thành tiền"
                y -= 15;
                y = writeLine(content, font, 11, 50, y, "Tạm tính:");
                y = writeLine(content, font, 11, 330, y, CurrencyFormat.vnd(invoice.getGrandTotal()));

                if (invoice.getDiscount().signum() > 0) {
                    y -= 16;
                    y = writeLine(content, font, 11, 50, y, "Giảm giá:");
                    y = writeLine(content, font, 11, 330, y,
                            "-" + CurrencyFormat.vnd(invoice.getDiscount()));
                }

                y -= 20;
                y = writeLine(content, font, 13, 50, y, "TỔNG THANH TOÁN:");
                writeLine(content, font, 13, 330, y, CurrencyFormat.vnd(invoice.getFinalTotal()));
            }

            document.save(outputFile.toFile());
        }
    }

    private float writeLine(PDPageContentStream content, PDType0Font font,
                             float size, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y;
    }
}