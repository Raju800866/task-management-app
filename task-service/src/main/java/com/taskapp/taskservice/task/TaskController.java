package com.taskapp.taskservice.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskapp.taskservice.auth.CurrentUser;
import com.taskapp.taskservice.task.dto.TaskRequest;
import com.taskapp.taskservice.task.dto.TaskResponse;
import com.taskapp.taskservice.task.dto.TaskStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) TaskStatus status) {
        return taskService.list(userId(jwt), status);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return taskService.get(userId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TaskRequest request) {
        return taskService.create(CurrentUser.from(jwt), request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(CurrentUser.from(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(CurrentUser.from(jwt), id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        taskService.delete(userId(jwt), id);
    }

    // The auth service puts the user id in the "sub" claim
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
