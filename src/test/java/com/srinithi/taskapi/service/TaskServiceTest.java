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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    private User alice;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);

        alice = new User("alice@example.com", "hash", "Alice", Role.USER);
        ReflectionTestUtils.setField(alice, "id", 1L);

        // Simulates what JwtAuthFilter does in production: puts the authenticated
        // user's email into the SecurityContext before the service layer runs.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(alice.getEmail(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_savesTaskOwnedByCurrentUser() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));

        TaskResponse response = taskService.create(new TaskCreateRequest("Write tests", "for Phase 8"));

        assertThat(response.title()).isEqualTo("Write tests");
        assertThat(response.description()).isEqualTo("for Phase 8");
        assertThat(response.status()).isEqualTo(Status.OPEN);
        assertThat(response.ownerId()).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getOne_returnsTask_whenOwnedByCurrentUser() {
        Task task = taskWithId(5L, "Alice's task", alice);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getOne(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("Alice's task");
    }

    @Test
    void getOne_throwsNotFound_whenTaskBelongsToAnotherUser() {
        // The owner-scoped query itself finds nothing for Alice - exactly what happens
        // in production when user A asks for user B's task. No data ever reaches here
        // for the service layer to accidentally leak.
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getOne(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_changesFields_whenOwned() {
        Task task = taskWithId(5L, "Old title", alice);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.update(5L, new TaskUpdateRequest("New title", "New desc", Status.DONE));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New desc");
        assertThat(response.status()).isEqualTo(Status.DONE);
    }

    @Test
    void update_throwsNotFound_whenNotOwned() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(5L, new TaskUpdateRequest("x", "y", Status.DONE)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_removesTask_whenOwned() {
        Task task = taskWithId(5L, "Doomed task", alice);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(task));

        taskService.delete(5L);

        verify(taskRepository).delete(task);
    }

    @Test
    void delete_throwsNotFound_andNeverDeletes_whenNotOwned() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(5L)).isInstanceOf(NotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void list_usesStatusFilteredQuery_whenStatusProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(taskWithId(1L, "Open task", alice)));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByOwnerIdAndStatus(eq(1L), eq(Status.OPEN), eq(pageable))).thenReturn(page);

        Page<TaskResponse> result = taskService.list(Status.OPEN, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository, never()).findByOwnerId(any(), any());
    }

    @Test
    void list_usesUnfilteredQuery_whenStatusNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(taskWithId(1L, "Any task", alice)));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(taskRepository.findByOwnerId(1L, pageable)).thenReturn(page);

        Page<TaskResponse> result = taskService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository, never()).findByOwnerIdAndStatus(any(), any(), any());
    }

    private Task taskWithId(Long id, String title, User owner) {
        Task task = new Task(title, "description", owner);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
