package com.taskapp.taskservice.notification;

import java.time.Instant;

/** Published when a task moves into DONE. Handled after the transaction commits. */
public record TaskCompletedEvent(Long taskId, String title, String ownerEmail, Instant completedAt) {
}
