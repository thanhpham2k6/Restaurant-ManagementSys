package model;

import java.math.BigDecimal;

public class InvoiceLine {
    private final String name;
    private final int quantity;
    private final BigDecimal price; 

    public InvoiceLine(String name, int quantity, BigDecimal price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        this.name = (name == null) ? "" : name;
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

    public BigDecimal getLineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
