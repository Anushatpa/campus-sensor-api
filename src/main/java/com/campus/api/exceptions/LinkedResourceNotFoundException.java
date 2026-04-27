package com.campus.api.exceptions;

/**
 * Mapped to HTTP 422 Entity by LinkedResourceNotFoundExceptionMapper
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
