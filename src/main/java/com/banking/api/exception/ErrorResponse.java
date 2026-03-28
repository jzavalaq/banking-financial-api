package com.banking.api.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard error response structure for API errors.
 * Format: {"error": "error-type", "status": 400, "message": "details", "path": "/api/v1/...", "timestamp": "..."}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        int status,
        String message,
        String path,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp
) {
    /**
     * Convenience constructor for standard error format.
     * Uses the HTTP status reason phrase as the error type.
     */
    public ErrorResponse(int status, String message, String path, Instant timestamp) {
        this(getReasonPhrase(status), status, message, path, timestamp);
    }

    /**
     * Get the HTTP reason phrase for a status code.
     */
    private static String getReasonPhrase(int statusCode) {
        try {
            HttpStatus status = HttpStatus.valueOf(statusCode);
            return status.getReasonPhrase();
        } catch (IllegalArgumentException e) {
            return "Error";
        }
    }
}
