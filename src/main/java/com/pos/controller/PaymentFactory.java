package com.pos.controller;

public class PaymentFactory {
    public static PaymentMethod create(String type) {
        if ("TRANSFER".equalsIgnoreCase(type)) {
            return new TransferPayment();
        }
        return new CashPayment();
    }
}
