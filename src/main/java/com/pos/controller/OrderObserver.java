package com.pos.controller;

import com.pos.model.OrderEvent;

/**
 * Observer nhận thông báo khi có thay đổi trên Order.
 * Áp dụng Observer Pattern.
 */
public interface OrderObserver {
    void onOrderChanged(OrderEvent event);
}
