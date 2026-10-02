package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class VehicleNotFoundException extends ParkingException {
    public VehicleNotFoundException(String message) {
        super("VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND, message != null ? message : "No vehicle found with that number.");
    }
}
