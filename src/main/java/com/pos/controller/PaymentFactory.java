package com.pos.controller;

public class PaymentFactory {
    public static PaymentMethod create(String type) {
        if ("TRANSFER".equalsIgnoreCase(type)) {
            return new TransferPayment();
        } else if ("MOMO".equalsIgnoreCase(type)) {
            return new MomoPayment(); // <-- THÊM MỚI Ở ĐÂY
        }
        return new CashPayment();     // Mặc định là thanh toán tiền mặt (Cash)
    }
}
