package service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class HolidayDiscountStrategy implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.15");

    @Override
    public String getName() {
        return "Khuyến mãi lễ";
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal.multiply(RATE).setScale(0, RoundingMode.HALF_UP);
    }
}
