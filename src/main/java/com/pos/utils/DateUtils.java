package com.pos.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateUtils() {}

    public static String format(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DT);
    }

    public static String formatDate(LocalDateTime dt) {
        return dt == null ? "" : dt.format(D);
    }
}
