package ThanhTran.lop_tien_ich;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormat {

    private static final Locale VIETNAM = new Locale("vi", "VN");

    private CurrencyFormat() {
        // Ngăn khởi tạo class tiện ích
    }

    public static String vnd(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(VIETNAM);
        formatter.setMaximumFractionDigits(0);

        return formatter.format(amount);
    }

    public static String vnd(double amount) {
        return vnd(BigDecimal.valueOf(amount));
    }

    public static String number(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(VIETNAM);
        formatter.setMaximumFractionDigits(0);

        return formatter.format(amount);
    }
}
