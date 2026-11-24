package com.srinithi.taskapi.exception;

// Thrown for bad login credentials. Deliberately the same message whether the email
// doesn't exist or the password is wrong - never reveal which, so an attacker can't
// use error messages to enumerate valid email addresses.
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
