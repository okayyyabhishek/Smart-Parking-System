package com.smartparking.exception;

import org.springframework.http.HttpStatus;

/**
 * Base domain exception carrying error codes and HTTP status mapping.
 */
public class ParkingException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ParkingException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
