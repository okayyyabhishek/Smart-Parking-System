package com.smartparking.dto.common;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error payload returned for all failure responses.
 * Stack traces and internal database exceptions are never leaked.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String code, String message) {
        return new ApiError(Instant.now(), status, code, message, null);
    }

    public static ApiError of(int status, String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, fieldErrors);
    }
}
