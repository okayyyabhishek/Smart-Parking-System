package com.smartparking.util;

import java.util.regex.Pattern;

/**
 * Utility for normalizing and validating vehicle registration numbers.
 * Normalized form: upper-case alphanumeric string without whitespace or hyphens.
 */
public final class VehicleNumberUtil {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9]{4,15}$");

    private VehicleNumberUtil() {
        // Utility class
    }

    /**
     * Normalizes a raw vehicle number:
     * - Trims whitespace
     * - Converts to uppercase
     * - Removes all whitespace, hyphens, and dots
     *
     * @param raw the raw input (e.g. "pb-10 ab 1234")
     * @return normalized number (e.g. "PB10AB1234"), or empty string if input is null
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toUpperCase()
                .replaceAll("[\\s\\-\\.]", "");
    }

    /**
     * Checks if the normalized vehicle number conforms to format requirements (4 to 15 alphanumeric characters).
     *
     * @param normalized the normalized string
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String normalized) {
        if (normalized == null) {
            return false;
        }
        return VALID_PATTERN.matcher(normalized).matches();
    }
}
