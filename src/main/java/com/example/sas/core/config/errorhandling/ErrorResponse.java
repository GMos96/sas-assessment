package com.example.sas.core.config.errorhandling;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard error response structure for all API errors
 */
public class ErrorResponse {
    private String errorCode;
    private String message;
    private OffsetDateTime timestamp;
    private Map<String, String> fieldErrors;

    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(String errorCode, String message, Map<String, String> fieldErrors) {
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = OffsetDateTime.now();
    }

    // Getters and setters

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}

