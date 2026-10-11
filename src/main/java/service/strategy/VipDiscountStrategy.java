package service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VipDiscountStrategy implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.10"); 

    @Override
    public String getName() {
        return "Khách hàng VIP";
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal.multiply(RATE).setScale(0, RoundingMode.HALF_UP);
    }
}
