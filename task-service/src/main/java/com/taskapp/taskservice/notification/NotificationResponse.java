package com.taskapp.taskservice.notification;

import java.time.Instant;

public record NotificationResponse(Long id, Long taskId, String message, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTaskId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
