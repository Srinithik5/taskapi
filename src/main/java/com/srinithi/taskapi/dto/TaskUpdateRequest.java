package com.srinithi.taskapi.dto;

import com.srinithi.taskapi.entity.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// A PUT is a full replace, so unlike the create request, status is required here too.
public record TaskUpdateRequest(

        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "status is required")
        Status status
) {
}
