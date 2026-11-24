package com.srinithi.taskapi.exception;

// Thrown when a resource doesn't exist - or, just as importantly, when it exists but
// doesn't belong to the current user. Both cases return 404, never revealing which.
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
