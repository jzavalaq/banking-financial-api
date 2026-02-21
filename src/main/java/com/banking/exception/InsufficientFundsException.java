package com.banking.exception;

/**
 * Exception thrown when account has insufficient funds for a transaction.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String accountNumber, String required, String available) {
        super(String.format("Insufficient funds in account %s. Required: %s, Available: %s",
                accountNumber, required, available));
    }
}
