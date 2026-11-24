package com.srinithi.taskapi.dto;

import com.srinithi.taskapi.entity.Status;

import java.time.Instant;

// What we actually send back to the client - deliberately NOT the Task entity itself.
public record TaskResponse(
        Long id,
        String title,
        String description,
        Status status,
        Instant createdAt,
        Long ownerId
) {
}
