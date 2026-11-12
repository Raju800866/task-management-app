package com.taskapp.taskservice.notification;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** An activity entry shown in the bell dropdown, e.g. "raju moved "Report" to Done". */
@Entity
@Table(name = "notifications",
       indexes = @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Recipient (the task owner), from the JWT "sub". No foreign key: users live in auth_db.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Plain column, no foreign key, so history survives task deletion
    @Column(name = "task_id")
    private Long taskId;

    @Column(nullable = false, length = 500)
    private String message;

    // "read" is avoided as a column name; it's a keyword in some databases
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long userId, Long taskId, String message) {
        this.userId = userId;
        this.taskId = taskId;
        this.message = message;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getTaskId() { return taskId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
