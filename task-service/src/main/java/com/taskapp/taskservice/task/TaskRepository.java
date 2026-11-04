package com.taskapp.taskservice.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TaskStatus status);

    // Every single-task lookup is scoped to the owner, so one user
    // can never read or change another user's task.
    Optional<Task> findByIdAndUserId(Long id, Long userId);
}
