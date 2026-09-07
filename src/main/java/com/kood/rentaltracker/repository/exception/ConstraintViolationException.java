package com.kood.rentaltracker.repository.exception;

public class ConstraintViolationException extends RuntimeException {

    public ConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}