package com.pos.controller;

import java.math.BigDecimal;

public interface PaymentMethod {
    boolean pay(BigDecimal amount);
    String getTypeName();
}
