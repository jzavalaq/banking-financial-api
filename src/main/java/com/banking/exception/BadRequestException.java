package com.banking.exception;

/**
 * Exception thrown for bad request scenarios.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
