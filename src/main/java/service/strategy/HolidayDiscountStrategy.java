package service.strategy;

import java.math.BigDecimal;

public final class HolidayDiscountStrategy implements DiscountStrategy {
    @Override
    public String getName() {
        return "Khuyến mãi lễ";
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.15"));
    }
}
