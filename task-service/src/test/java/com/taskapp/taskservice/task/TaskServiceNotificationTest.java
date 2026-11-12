package com.taskapp.taskservice.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.taskapp.taskservice.auth.CurrentUser;
import com.taskapp.taskservice.notification.NotificationService;
import com.taskapp.taskservice.notification.TaskCompletedEvent;
import com.taskapp.taskservice.task.dto.TaskRequest;

/** Activity entries and completion emails triggered by task changes. */
class TaskServiceNotificationTest {

    private static final CurrentUser RAJU = new CurrentUser(42L, "raju@example.com", "raju");

    private TaskRepository repository;
    private ApplicationEventPublisher events;
    private NotificationService notifications;
    private TaskService service;
    private Task task;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        events = mock(ApplicationEventPublisher.class);
        notifications = mock(NotificationService.class);
        service = new TaskService(repository, events, notifications);

        task = new Task();
        task.setUserId(42L);
        task.setTitle("Write report");
        when(repository.findByIdAndUserId(7L, 42L)).thenReturn(Optional.of(task));
        when(repository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createRecordsActivity() {
        service.create(RAJU, new TaskRequest("Plan sprint", null, null, null, null, null));

        verify(notifications).record(eq(42L), any(), eq("raju created \"Plan sprint\""));
    }

    @Test
    void movingToDoneRecordsActivityAndPublishesEmailEvent() {
        task.setStatus(TaskStatus.IN_PROGRESS);

        service.updateStatus(RAJU, 7L, TaskStatus.DONE);

        verify(notifications).record(eq(42L), any(), eq("raju moved \"Write report\" to Done"));
        ArgumentCaptor<TaskCompletedEvent> captor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().ownerEmail()).isEqualTo("raju@example.com");
    }

    @Test
    void otherStatusChangeRecordsActivityButNoEmail() {
        task.setStatus(TaskStatus.TODO);

        service.updateStatus(RAJU, 7L, TaskStatus.IN_PROGRESS);

        verify(notifications).record(eq(42L), any(), eq("raju moved \"Write report\" to In progress"));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void sameStatusRecordsNothing() {
        task.setStatus(TaskStatus.DONE);

        service.updateStatus(RAJU, 7L, TaskStatus.DONE);

        verify(notifications, never()).record(anyLong(), any(), anyString());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void fullUpdateWithoutStatusChangeRecordsNothing() {
        task.setStatus(TaskStatus.TODO);

        service.update(RAJU, 7L, new TaskRequest("Renamed", null, TaskStatus.TODO, null, null, null));

        verify(notifications, never()).record(anyLong(), any(), anyString());
    }

    @Test
    void fullUpdateToDoneRecordsActivityAndPublishesEvent() {
        task.setStatus(TaskStatus.TODO);

        service.update(RAJU, 7L, new TaskRequest("Write report", null, TaskStatus.DONE, null, null, null));

        verify(notifications).record(eq(42L), any(), eq("raju moved \"Write report\" to Done"));
        verify(events).publishEvent(any(TaskCompletedEvent.class));
    }
}
