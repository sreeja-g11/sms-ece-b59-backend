package com.miniclub.service;

import java.util.Locale;

final class Text {

    private Text() {
    }

    /** Trims the value and turns blank strings into null. */
    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Lower-cased search text, or null when there is nothing to search for. */
    static String query(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    /** True when any of the values contains the (already lower-cased) query. */
    static boolean matches(String query, String... values) {
        if (query == null) {
            return true;
        }
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }
}
