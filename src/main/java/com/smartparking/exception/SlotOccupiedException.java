package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class SlotOccupiedException extends ParkingException {
    public SlotOccupiedException(String message) {
        super("SLOT_OCCUPIED", HttpStatus.CONFLICT, message != null ? message : "That slot is occupied.");
    }
}
