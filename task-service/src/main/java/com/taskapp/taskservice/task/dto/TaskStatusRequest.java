package com.taskapp.taskservice.task.dto;

import com.taskapp.taskservice.task.TaskStatus;

import jakarta.validation.constraints.NotNull;

/** Body for PATCH /api/tasks/{id}/status, used by the dashboard's quick status change. */
public record TaskStatusRequest(@NotNull(message = "Status is required") TaskStatus status) {
}
