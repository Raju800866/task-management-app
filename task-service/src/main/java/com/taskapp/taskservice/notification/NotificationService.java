package com.taskapp.taskservice.notification;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** Joins the caller's transaction, so the entry is saved only if the task change is. */
    @Transactional
    public void record(Long userId, Long taskId, String message) {
        repository.save(new Notification(userId, taskId, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return repository.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllRead(userId);
    }
}
