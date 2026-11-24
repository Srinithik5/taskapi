package com.srinithi.taskapi.dto;

import com.srinithi.taskapi.entity.Role;

public record AuthResponse(String token, Role role) {
}
