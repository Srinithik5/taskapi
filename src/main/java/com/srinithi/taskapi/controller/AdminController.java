package com.srinithi.taskapi.controller;

import com.srinithi.taskapi.dto.TaskResponse;
import com.srinithi.taskapi.dto.UserResponse;
import com.srinithi.taskapi.service.TaskService;
import com.srinithi.taskapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Every endpoint here requires ROLE_ADMIN - enforced by SecurityConfig's
// "/api/admin/**" rule, not by anything in this class itself.
@Tag(name = "Admin", description = "Requires ADMIN role")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TaskService taskService;

    public AdminController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @Operation(summary = "List every registered user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "Logged in, but not an admin")
    })
    @GetMapping("/users")
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userService.listAll(pageable);
    }

    @Operation(summary = "List every task across all users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "Logged in, but not an admin")
    })
    @GetMapping("/tasks")
    public Page<TaskResponse> listTasks(Pageable pageable) {
        return taskService.listAllForAdmin(pageable);
    }
}
