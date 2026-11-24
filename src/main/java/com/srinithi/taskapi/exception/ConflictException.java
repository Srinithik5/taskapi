package com.srinithi.taskapi.exception;

// Thrown when a request conflicts with existing state - e.g. registering an email
// that's already taken.
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
