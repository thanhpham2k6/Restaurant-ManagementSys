package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Invoice {
    private final String code;
    private final String customerName;
    private final List<InvoiceLine> items = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now(); // ngày giờ lập hóa đơn
    private String tableName = "";                          // số bàn / tên bàn
    private BigDecimal discount = BigDecimal.ZERO;          // tiền giảm giá

    public Invoice(String code, String customerName) {
        this.code = code;
        this.customerName = customerName;
    }

    // Thêm món ăn vào hóa đơn
    public void addItem(InvoiceLine item) {
        if (item != null) {
            items.add(item);
        }
    }

    public String getCode() {
        return code;
    }

    public String getCustomerName() {
        return customerName;
    }

    // Trả về danh sách chỉ đọc, muốn thêm món phải dùng addItem()
    public List<InvoiceLine> getItems() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Dùng khi nạp hóa đơn cũ từ DB (mặc định là thời điểm tạo đối tượng)
    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    // Giảm giá null hoặc âm sẽ được coi là 0
    public void setDiscount(BigDecimal discount) {
        this.discount = (discount == null || discount.signum() < 0) ? BigDecimal.ZERO : discount;
    }

    // Tổng tiền các món, chưa trừ giảm giá
    public BigDecimal getGrandTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    // Số tiền khách phải trả = tổng các món - giảm giá (không nhỏ hơn 0)
    public BigDecimal getFinalTotal() {
        BigDecimal result = getGrandTotal().subtract(discount);
        return (result.signum() < 0) ? BigDecimal.ZERO : result;
    }
}