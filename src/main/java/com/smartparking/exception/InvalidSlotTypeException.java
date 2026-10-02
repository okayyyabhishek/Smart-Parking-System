package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class InvalidSlotTypeException extends ParkingException {
    public InvalidSlotTypeException(String message) {
        super("INVALID_SLOT_TYPE", HttpStatus.BAD_REQUEST, message != null ? message : "Unsupported slot type.");
    }
}
