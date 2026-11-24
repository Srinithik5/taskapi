package com.srinithi.taskapi.controller;

import com.srinithi.taskapi.dto.TaskResponse;
import com.srinithi.taskapi.dto.UserResponse;
import com.srinithi.taskapi.service.TaskService;
import com.srinithi.taskapi.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Every endpoint here requires ROLE_ADMIN - enforced by SecurityConfig's
// "/api/admin/**" rule, not by anything in this class itself.
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TaskService taskService;

    public AdminController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @GetMapping("/users")
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userService.listAll(pageable);
    }

    @GetMapping("/tasks")
    public Page<TaskResponse> listTasks(Pageable pageable) {
        return taskService.listAllForAdmin(pageable);
    }
}
