package com.srinithi.taskapi.service;

import com.srinithi.taskapi.repository.TaskRepository;
import org.springframework.stereotype.Service;

// Business-logic layer, between the controller (HTTP concerns) and the repository
// (database concerns). Full CRUD methods are added in Phase 4.
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
}
