package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class DuplicateVehicleException extends ParkingException {
    public DuplicateVehicleException(String message) {
        super("DUPLICATE_VEHICLE", HttpStatus.CONFLICT, message != null ? message : "This vehicle number already exists.");
    }
}
