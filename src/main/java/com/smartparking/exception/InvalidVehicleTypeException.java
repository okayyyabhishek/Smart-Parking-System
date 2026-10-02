package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class InvalidVehicleTypeException extends ParkingException {
    public InvalidVehicleTypeException(String message) {
        super("INVALID_VEHICLE_TYPE", HttpStatus.BAD_REQUEST, message != null ? message : "Choose CAR, EV, SUV or ACCESSIBLE.");
    }
}
