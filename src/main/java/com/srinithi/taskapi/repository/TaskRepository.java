package com.srinithi.taskapi.repository;

import com.srinithi.taskapi.entity.Status;
import com.srinithi.taskapi.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // The ownership check happens IN the query itself (WHERE id = ? AND owner_id = ?),
    // not by loading the task first and checking in Java - that's what makes it
    // impossible for user A to ever read user B's task. See Phase 4/6.
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Task> findByOwnerIdAndStatus(Long ownerId, Status status, Pageable pageable);
}
