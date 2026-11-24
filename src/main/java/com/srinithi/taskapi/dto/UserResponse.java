package com.srinithi.taskapi.dto;

import com.srinithi.taskapi.entity.Role;

import java.time.Instant;

// Public-safe view of a User - notice there's no password field here at all,
// not even the hash. It's structurally impossible to leak it through this DTO.
public record UserResponse(
        Long id,
        String email,
        String name,
        Role role,
        Instant createdAt
) {
}
