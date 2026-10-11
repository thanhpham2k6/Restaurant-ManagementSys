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

    private LocalDateTime createdAt = LocalDateTime.now(); 
    private String tableName = "";                          
    private BigDecimal discount = BigDecimal.ZERO;          

    public Invoice(String code, String customerName) {
        this.code = code;
        this.customerName = customerName;
    }

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

    public List<InvoiceLine> getItems() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

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

    public void setDiscount(BigDecimal discount) {
        this.discount = (discount == null || discount.signum() < 0) ? BigDecimal.ZERO : discount;
    }

    public BigDecimal getGrandTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    public BigDecimal getFinalTotal() {
        BigDecimal result = getGrandTotal().subtract(discount);
        return (result.signum() < 0) ? BigDecimal.ZERO : result;
    }
}