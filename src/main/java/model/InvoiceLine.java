package model;

import java.math.BigDecimal;

public class InvoiceLine {
    private final String name;
    private final int quantity;
    private final BigDecimal price; // Giá bán lẻ 1 món

    public InvoiceLine(String name, int quantity, BigDecimal price) {
        this.name = name;
        this.quantity = quantity;
        this.price = (price == null) ? BigDecimal.ZERO : price;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    // Thành tiền = Số lượng * Giá
    public BigDecimal getLineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}