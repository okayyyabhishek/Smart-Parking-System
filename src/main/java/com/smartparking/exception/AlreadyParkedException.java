package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class AlreadyParkedException extends ParkingException {
    public AlreadyParkedException(String slotNumber) {
        super("ALREADY_PARKED", HttpStatus.CONFLICT, "This vehicle is already parked in slot " + slotNumber + ".");
    }
}
