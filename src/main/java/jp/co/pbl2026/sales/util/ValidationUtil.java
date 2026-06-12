package jp.co.pbl2026.sales.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationUtil {
    private ValidationUtil() {}

    public static Map<String, String> errors() {
        // 表示順を入力項目順に近づけるため LinkedHashMap を使う。
        return new LinkedHashMap<>();
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static Integer parseInteger(String value, String fieldName, Map<String, String> errors) {
        String text = trim(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            errors.put(fieldName, "整数で入力してください。");
            return null;
        }
    }

    public static LocalDate parseDate(String value, String fieldName, Map<String, String> errors) {
        String text = trim(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            errors.put(fieldName, "日付形式で入力してください。");
            return null;
        }
    }
}
