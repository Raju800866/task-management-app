package com.taskapp.taskservice.task.dto;

import java.time.LocalDate;

import com.taskapp.taskservice.task.TaskCategory;
import com.taskapp.taskservice.task.TaskPriority;
import com.taskapp.taskservice.task.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for create (POST) and full update (PUT). Status/priority default when omitted; category is optional. */
public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        TaskStatus status,
        TaskPriority priority,
        TaskCategory category,
        LocalDate dueDate) {
}
