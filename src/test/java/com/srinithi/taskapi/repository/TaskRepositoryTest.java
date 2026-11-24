package com.srinithi.taskapi.repository;

import com.srinithi.taskapi.entity.Role;
import com.srinithi.taskapi.entity.Status;
import com.srinithi.taskapi.entity.Task;
import com.srinithi.taskapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Real H2 database, real Hibernate, real SQL - not mocked. This is where we PROVE,
// against an actual running database, that user A can never fetch user B's task:
// the ownership check lives in the query itself (see findByIdAndOwnerId_...).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private Task taskOwnedByA;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User("userA@example.com", "hash", "User A", Role.USER));
        userB = userRepository.save(new User("userB@example.com", "hash", "User B", Role.USER));
        taskOwnedByA = taskRepository.save(new Task("Alice's private task", "secret", userA));
    }

    @Test
    void findByIdAndOwnerId_returnsTask_whenRequestedByItsRealOwner() {
        Optional<Task> found = taskRepository.findByIdAndOwnerId(taskOwnedByA.getId(), userA.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Alice's private task");
    }

    @Test
    void findByIdAndOwnerId_returnsEmpty_whenRequestedByADifferentUser() {
        // THE core security guarantee of this whole project: user B asking for user
        // A's task id gets nothing back - enforced by the database query itself, not
        // by a Java "if" that a future refactor could accidentally delete.
        Optional<Task> found = taskRepository.findByIdAndOwnerId(taskOwnedByA.getId(), userB.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByOwnerIdAndStatus_onlyReturnsTasksMatchingBothOwnerAndStatus() {
        Task doneTaskForA = new Task("Finished task", "", userA);
        doneTaskForA.setStatus(Status.DONE);
        taskRepository.save(doneTaskForA);

        Page<Task> openTasksForA = taskRepository.findByOwnerIdAndStatus(
                userA.getId(), Status.OPEN, PageRequest.of(0, 10));

        assertThat(openTasksForA.getContent())
                .hasSize(1)
                .allMatch(t -> t.getStatus() == Status.OPEN && t.getOwner().getId().equals(userA.getId()));
    }
}
