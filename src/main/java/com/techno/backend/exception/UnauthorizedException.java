package com.techno.backend.exception;

/**
 * Unauthorized Exception
 * Thrown when authentication fails or user lacks authorization
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

