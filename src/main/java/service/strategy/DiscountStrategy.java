package service.strategy;

import java.math.BigDecimal;

public interface DiscountStrategy {
    String getName();
    BigDecimal calculate(BigDecimal subtotal);
}
