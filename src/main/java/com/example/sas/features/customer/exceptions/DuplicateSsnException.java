package com.example.sas.features.customer.exceptions;

/**
 * Exception thrown when attempting to create a customer with a duplicate SSN
 */
public class DuplicateSsnException extends RuntimeException {
    public DuplicateSsnException(String message) {
        super(message);
    }

    public DuplicateSsnException(String message, Throwable cause) {
        super(message, cause);
    }
}

