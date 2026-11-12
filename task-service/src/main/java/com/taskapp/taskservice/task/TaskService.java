package com.taskapp.taskservice.task;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskapp.taskservice.auth.CurrentUser;
import com.taskapp.taskservice.exception.TaskNotFoundException;
import com.taskapp.taskservice.notification.NotificationService;
import com.taskapp.taskservice.notification.TaskCompletedEvent;
import com.taskapp.taskservice.task.dto.TaskRequest;
import com.taskapp.taskservice.task.dto.TaskResponse;

@Service
public class TaskService {

    private final TaskRepository repository;
    private final ApplicationEventPublisher events;
    private final NotificationService notifications;

    public TaskService(TaskRepository repository, ApplicationEventPublisher events,
                       NotificationService notifications) {
        this.repository = repository;
        this.events = events;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long userId, TaskStatus status) {
        List<Task> tasks = (status == null)
                ? repository.findByUserIdOrderByCreatedAtDesc(userId)
                : repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long userId, Long taskId) {
        return TaskResponse.from(findOwned(userId, taskId));
    }

    @Transactional
    public TaskResponse create(CurrentUser user, TaskRequest request) {
        Task task = new Task();
        task.setUserId(user.id());
        apply(task, request);
        Task saved = repository.save(task);
        notifications.record(user.id(), saved.getId(), "%s created \"%s\"".formatted(user.name(), saved.getTitle()));
        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse update(CurrentUser user, Long taskId, TaskRequest request) {
        Task task = findOwned(user.id(), taskId);
        TaskStatus previous = task.getStatus();
        apply(task, request);
        Task saved = repository.saveAndFlush(task);
        afterStatusChange(user, previous, saved);
        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse updateStatus(CurrentUser user, Long taskId, TaskStatus status) {
        Task task = findOwned(user.id(), taskId);
        TaskStatus previous = task.getStatus();
        task.setStatus(status);
        Task saved = repository.saveAndFlush(task);
        afterStatusChange(user, previous, saved);
        return TaskResponse.from(saved);
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        repository.delete(findOwned(userId, taskId));
    }

    // Returns 404 (not 403) for someone else's task, so ids of other
    // users' tasks aren't revealed.
    private Task findOwned(Long userId, Long taskId) {
        return repository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    // Only a real status change counts; re-saving with the same status records nothing.
    private void afterStatusChange(CurrentUser user, TaskStatus previous, Task task) {
        if (previous == task.getStatus()) {
            return;
        }
        // Activity entry, saved in the same transaction as the task change
        notifications.record(user.id(), task.getId(),
                "%s moved \"%s\" to %s".formatted(user.name(), task.getTitle(), task.getStatus().label()));
        // Completion email, sent after commit (see CompletionEmailNotifier)
        if (task.getStatus() == TaskStatus.DONE) {
            events.publishEvent(new TaskCompletedEvent(task.getId(), task.getTitle(), user.email(), Instant.now()));
        }
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description() == null ? null : request.description().trim());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setCategory(request.category()); // null = no category
        task.setDueDate(request.dueDate());
    }
}
