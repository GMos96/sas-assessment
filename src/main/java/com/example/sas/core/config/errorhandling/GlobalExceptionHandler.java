package com.example.sas.core.config.errorhandling;

import com.example.sas.common.security.exception.EncryptionException;
import com.example.sas.features.customer.exceptions.CustomerNotFoundException;
import com.example.sas.features.customer.exceptions.DuplicateSsnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers
 * Provides consistent error responses and proper HTTP status codes
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {} field(s) with errors", fieldErrors.size());

        return new ErrorResponse(
            "VALIDATION_ERROR",
            "Input validation failed. Please check the field errors.",
            fieldErrors
        );
    }

    /**
     * Handle customer not found errors
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCustomerNotFound(CustomerNotFoundException ex) {
        log.warn("Customer not found: {}", ex.getMessage());
        return new ErrorResponse("CUSTOMER_NOT_FOUND", ex.getMessage());
    }

    /**
     * Handle duplicate SSN errors
     */
    @ExceptionHandler(DuplicateSsnException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateSsn(DuplicateSsnException ex) {
        log.warn("Duplicate SSN attempt: {}", ex.getMessage());
        return new ErrorResponse("DUPLICATE_SSN", ex.getMessage());
    }

    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);

        String message = "Data integrity violation occurred";
        if (ex.getMessage() != null && ex.getMessage().contains("ssn_hash")) {
            message = "A customer with this SSN already exists";
        }

        return new ErrorResponse("DATA_INTEGRITY_VIOLATION", message);
    }

    /**
     * Handle encryption-related errors
     */
    @ExceptionHandler(EncryptionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleEncryptionError(EncryptionException ex) {
        log.error("Encryption error occurred", ex);
        return new ErrorResponse(
            "ENCRYPTION_ERROR",
            "An error occurred while processing sensitive data. Please contact support."
        );
    }

    /**
     * Handle optimistic locking failures
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return new ErrorResponse(
            "OPTIMISTIC_LOCKING_FAILURE",
            "The resource you are trying to update has been modified by another process. Please refresh and try again."
        );
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return new ErrorResponse("INVALID_ARGUMENT", ex.getMessage());
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericError(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact support if the problem persists."
        );
    }
}

