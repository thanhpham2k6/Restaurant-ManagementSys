package service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import static javax.swing.border.TitledBorder.BOTTOM;
import model.Invoice;
import model.InvoiceLine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import utils.CurrencyFormat;

public final class PDFExporter {

    private static final float LEFT = 50;
    private static final float RIGHT = 545;          
    private static final float TOP = 800;            
    private static final float QTY_RIGHT = 360;      
    private static final float NAME_MAX_WIDTH = 270; 
    private static final float ROW_HEIGHT = 16;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportInvoice(Invoice invoice, Path outputFile, Path fontFile)
            throws IOException {

        try (PDDocument document = new PDDocument()) {
            PDType0Font font = PDType0Font.load(document, fontFile.toFile());

            try (PageWriter w = new PageWriter(document, font)) {
                writeHeader(w, invoice);
                writeItems(w, invoice);
                writeTotals(w, invoice);
            }

            document.save(outputFile.toFile());
        }
    }

    private void writeHeader(PageWriter w, Invoice inv) throws IOException {
        w.text("HÓA ĐƠN THANH TOÁN", 18, LEFT);
        w.down(22);
        w.text("Mã hóa đơn: " + Objects.toString(inv.getCode(), ""), 11, LEFT);
        w.down(15);
        w.text("Ngày lập: " + inv.getCreatedAt().format(DATE_TIME), 11, LEFT);
        if (!inv.getTableName().isEmpty()) {
            w.down(15);
            w.text("Bàn: " + inv.getTableName(), 11, LEFT);
        }
        w.down(15);
        w.text("Khách hàng: " + Objects.toString(inv.getCustomerName(), ""), 11, LEFT);
        w.down(28);
        writeColumnTitles(w);
    }

    private void writeColumnTitles(PageWriter w) throws IOException {
        w.text("Món ăn", 11, LEFT);
        w.textRight("SL", 11, QTY_RIGHT);
        w.textRight("Thành tiền", 11, RIGHT);
        w.down(6);
        w.rule();
        w.down(16);
    }

    private void writeItems(PageWriter w, Invoice inv) throws IOException {
        List<InvoiceLine> items = inv.getItems();
        for (int i = 0; i < items.size(); i++) {
            InvoiceLine line = items.get(i);
            boolean last = (i == items.size() - 1);

            float needed = ROW_HEIGHT + (last ? totalsHeight(inv) : 0);
            if (!w.hasRoom(needed)) {           
                w.newPage();
                writeColumnTitles(w);
            }
            w.text(w.fit(line.getName(), 10, NAME_MAX_WIDTH), 10, LEFT);
            w.textRight(String.valueOf(line.getQuantity()), 10, QTY_RIGHT);
            w.textRight(CurrencyFormat.vnd(line.getLineTotal()), 10, RIGHT);
            w.down(ROW_HEIGHT);
        }
    }

    private float totalsHeight(Invoice inv) {
        return inv.getDiscount().signum() > 0 ? 90 : 70;
    }

    private void writeTotals(PageWriter w, Invoice inv) throws IOException {
        boolean hasDiscount = inv.getDiscount().signum() > 0;

        if (w.hasRoom(totalsHeight(inv))) {
            w.down(4);
        } else {
            w.newPage();
        }
        w.rule();
        w.down(18);

        w.text("Tạm tính:", 11, LEFT);
        w.textRight(CurrencyFormat.vnd(inv.getGrandTotal()), 11, RIGHT);

        if (hasDiscount) {
            w.down(16);
            w.text("Giảm giá:", 11, LEFT);
            w.textRight("-" + CurrencyFormat.vnd(inv.getDiscount()), 11, RIGHT);
        }

        w.down(22);
        w.text("TỔNG THANH TOÁN:", 13, LEFT);
        w.textRight(CurrencyFormat.vnd(inv.getFinalTotal()), 13, RIGHT);
    }

    private static final class PageWriter implements AutoCloseable {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream content;
        private float y;

        PageWriter(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = TOP;
        }

        void down(float d) { y -= d; }

        boolean hasRoom(float needed) { return y - needed >= BOTTOM; }

        void text(String s, float size, float x) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(clean(s));
            content.endText();
        }

        void textRight(String s, float size, float rightX) throws IOException {
            String c = clean(s);
            text(c, size, rightX - width(c, size));
        }

        void rule() throws IOException {
            content.setLineWidth(0.5f);
            content.moveTo(LEFT, y);
            content.lineTo(RIGHT, y);
            content.stroke();
        }

        float width(String s, float size) throws IOException {
            return font.getStringWidth(s) / 1000f * size;
        }

        String fit(String s, float size, float maxWidth) throws IOException {
            String c = clean(s);
            if (width(c, size) <= maxWidth) {
                return c;
            }
            int end = c.length();
            while (end > 0 && width(c.substring(0, end) + "...", size) > maxWidth) {
                end--;
            }
            return c.substring(0, end) + "...";
        }

        private static String clean(String s) {
            return (s == null) ? "" : s.replaceAll("\\p{Cntrl}", " ");
        }

        @Override
        public void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }
    }
}
