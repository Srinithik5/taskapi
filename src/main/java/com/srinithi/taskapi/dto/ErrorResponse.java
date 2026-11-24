package com.srinithi.taskapi.dto;

import java.time.Instant;
import java.util.Map;

// One consistent shape for every error response in the whole app - built by GlobalExceptionHandler.
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
