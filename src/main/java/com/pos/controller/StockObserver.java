package com.pos.controller;

/**
 * Observer nhận thông báo khi tồn kho thay đổi.
 * Áp dụng Observer Pattern.
 */
public interface StockObserver {
    void onStockChanged(int menuItemId);
}
