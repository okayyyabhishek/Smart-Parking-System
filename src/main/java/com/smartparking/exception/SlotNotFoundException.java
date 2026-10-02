package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class SlotNotFoundException extends ParkingException {
    public SlotNotFoundException(String message) {
        super("SLOT_NOT_FOUND", HttpStatus.NOT_FOUND, message != null ? message : "No slot found with that number.");
    }
}
