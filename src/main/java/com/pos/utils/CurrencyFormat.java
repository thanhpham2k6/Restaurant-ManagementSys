package com.pos.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormat {
    private static final NumberFormat FMT = NumberFormat.getInstance(new Locale("vi", "VN"));
    static { FMT.setGroupingUsed(true); }

    private CurrencyFormat() {}

    public static String format(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return FMT.format(amount) + " đ";
    }

    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }
}
