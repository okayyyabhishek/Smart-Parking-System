package com.smartparking.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ParkingException {
    public AccountLockedException() {
        super("ACCOUNT_LOCKED", HttpStatus.LOCKED, "Too many attempts. Try again in 5 minutes.");
    }
}
