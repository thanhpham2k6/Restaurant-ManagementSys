package service;

import java.math.BigDecimal;
import model.Invoice;
import service.strategy.DiscountStrategy;
import service.strategy.NoDiscountStrategy;


public final class OrderPricingService {

    public BigDecimal applyDiscount(Invoice invoice, DiscountStrategy strategy) {
        DiscountStrategy s = (strategy == null) ? new NoDiscountStrategy() : strategy;
        invoice.setDiscount(s.calculate(invoice.getGrandTotal()));
        return invoice.getDiscount();
    }
}
