package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class AlreadyWaitingException extends ParkingException {
    public AlreadyWaitingException() {
        super("ALREADY_WAITING", HttpStatus.CONFLICT, "This vehicle is already in the waiting queue.");
    }
}
