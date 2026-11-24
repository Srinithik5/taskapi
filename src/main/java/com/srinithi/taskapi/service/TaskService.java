package com.srinithi.taskapi.service;

import com.srinithi.taskapi.dto.TaskCreateRequest;
import com.srinithi.taskapi.dto.TaskResponse;
import com.srinithi.taskapi.dto.TaskUpdateRequest;
import com.srinithi.taskapi.entity.Role;
import com.srinithi.taskapi.entity.Status;
import com.srinithi.taskapi.entity.Task;
import com.srinithi.taskapi.entity.User;
import com.srinithi.taskapi.exception.NotFoundException;
import com.srinithi.taskapi.repository.TaskRepository;
import com.srinithi.taskapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    // TEMPORARY: there's no login yet (arrives in Phase 5/6), so every request acts as
    // this one fixed user. Replaced with the real logged-in user in Phase 6.
    private static final String TEMP_OWNER_EMAIL = "temp-owner@example.com";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        User owner = temporaryOwner();
        Task task = new Task(request.title(), request.description(), owner);
        taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Status status, Pageable pageable) {
        Long ownerId = temporaryOwner().getId();
        Page<Task> page = (status != null)
                ? taskRepository.findByOwnerIdAndStatus(ownerId, status, pageable)
                : taskRepository.findByOwnerId(ownerId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse getOne(Long id) {
        return toResponse(ownedTaskOrThrow(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        Task task = ownedTaskOrThrow(id);
        // No explicit save() call needed: inside a @Transactional method, Hibernate
        // tracks changes to a managed entity and writes them back on commit ("dirty
        // checking") - these setters are enough.
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        return toResponse(task);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(ownedTaskOrThrow(id));
    }

    private Task ownedTaskOrThrow(Long id) {
        Long ownerId = temporaryOwner().getId();
        return taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private User temporaryOwner() {
        return userRepository.findByEmail(TEMP_OWNER_EMAIL)
                .orElseGet(() -> userRepository.save(
                        new User(TEMP_OWNER_EMAIL, "temporary", "Temp Owner", Role.USER)));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getOwner().getId()
        );
    }
}
