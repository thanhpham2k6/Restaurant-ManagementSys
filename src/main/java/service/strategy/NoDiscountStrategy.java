package service.strategy;

import java.math.BigDecimal;

public final class NoDiscountStrategy implements DiscountStrategy {
    @Override public String getName() { return "Không giảm giá"; }
    @Override public BigDecimal calculate(BigDecimal subtotal) { return BigDecimal.ZERO; }
}
