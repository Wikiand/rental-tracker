package com.kood.rentaltracker.repository.exception;

public class DatabaseOpenException extends RuntimeException {

    public DatabaseOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}