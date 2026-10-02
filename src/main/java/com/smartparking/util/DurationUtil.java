package com.smartparking.util;

/**
 * Utility for formatting duration in seconds to human-readable strings.
 */
public final class DurationUtil {

    private DurationUtil() {
    }

    /**
     * Formats duration in seconds to "Hh Mm" or "< 1m".
     */
    public static String formatDuration(Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return "0m";
        }
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
}
