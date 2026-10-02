package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class SlotMaintenanceException extends ParkingException {
    public SlotMaintenanceException(String message) {
        super("SLOT_MAINTENANCE", HttpStatus.CONFLICT, message != null ? message : "That slot is under maintenance.");
    }
}
