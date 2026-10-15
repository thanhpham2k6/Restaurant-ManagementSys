package com.pos.controller;

import java.math.BigDecimal;

public class MomoPayment implements PaymentMethod {
    @Override
    public boolean pay(BigDecimal amount) {
        return true;
    }

    @Override
    public String getTypeName() {
        return "Momo";
    }
}