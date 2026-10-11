package service.strategy;

import java.math.BigDecimal;

public final class VipDiscountStrategy implements DiscountStrategy {
    @Override
    public String getName() {
        return "Khách hàng VIP";
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.10"));
    }
}
