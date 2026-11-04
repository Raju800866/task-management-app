package com.taskapp.taskservice.task;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskapp.taskservice.exception.TaskNotFoundException;
import com.taskapp.taskservice.task.dto.TaskRequest;
import com.taskapp.taskservice.task.dto.TaskResponse;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
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
    public TaskResponse create(Long userId, TaskRequest request) {
        Task task = new Task();
        task.setUserId(userId);
        apply(task, request);
        return TaskResponse.from(repository.save(task));
    }

    @Transactional
    public TaskResponse update(Long userId, Long taskId, TaskRequest request) {
        Task task = findOwned(userId, taskId);
        apply(task, request);
        return TaskResponse.from(repository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long userId, Long taskId, TaskStatus status) {
        Task task = findOwned(userId, taskId);
        task.setStatus(status);
        return TaskResponse.from(repository.saveAndFlush(task));
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

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description() == null ? null : request.description().trim());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setDueDate(request.dueDate());
    }
}
