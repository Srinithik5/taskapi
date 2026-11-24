package com.srinithi.taskapi.service;

import com.srinithi.taskapi.dto.TaskCreateRequest;
import com.srinithi.taskapi.dto.TaskResponse;
import com.srinithi.taskapi.dto.TaskUpdateRequest;
import com.srinithi.taskapi.entity.Status;
import com.srinithi.taskapi.entity.Task;
import com.srinithi.taskapi.entity.User;
import com.srinithi.taskapi.exception.NotFoundException;
import com.srinithi.taskapi.repository.TaskRepository;
import com.srinithi.taskapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        User owner = currentUser();
        Task task = new Task(request.title(), request.description(), owner);
        taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Status status, Pageable pageable) {
        Long ownerId = currentUser().getId();
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

    // ADMIN ONLY - deliberately separate from list() above, which is always scoped
    // to the current user. Called only from AdminController, which SecurityConfig
    // already restricts to ROLE_ADMIN.
    @Transactional(readOnly = true)
    public Page<TaskResponse> listAllForAdmin(Pageable pageable) {
        return taskRepository.findAll(pageable).map(this::toResponse);
    }

    private Task ownedTaskOrThrow(Long id) {
        Long ownerId = currentUser().getId();
        // The ownership check happens IN the query (WHERE id = ? AND owner_id = ?),
        // not by loading the task first and checking in Java - this is what makes it
        // structurally impossible for user A to ever read user B's task. A task that
        // exists but belongs to someone else returns 404, identical to a task that
        // doesn't exist at all - we never reveal which.
        return taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
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
