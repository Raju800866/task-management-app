package com.taskapp.taskservice.task.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.taskapp.taskservice.task.Task;
import com.taskapp.taskservice.task.TaskPriority;
import com.taskapp.taskservice.task.TaskStatus;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
