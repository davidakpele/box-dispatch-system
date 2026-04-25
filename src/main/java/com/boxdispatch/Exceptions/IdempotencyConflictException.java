package com.boxdispatch.Exceptions;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Request with idempotency key '" + key + "' is already being processed.");
    }
}