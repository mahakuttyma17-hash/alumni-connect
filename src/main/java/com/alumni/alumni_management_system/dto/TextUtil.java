package com.alumni.alumni_management_system.dto;

import java.util.Locale;

public final class TextUtil {

    private TextUtil() {
    }

    /** Trims the text. Returns null when the result is empty. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Trims and lowercases an email address. Returns null when empty. */
    public static String email(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}