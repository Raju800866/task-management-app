package com.taskapp.taskservice.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.taskapp.taskservice.config.SecurityConfig;
import com.taskapp.taskservice.exception.TaskNotFoundException;
import com.taskapp.taskservice.task.dto.TaskRequest;
import com.taskapp.taskservice.task.dto.TaskResponse;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        verifyNoInteractions(taskService);
    }

    @Test
    void listsOnlyTheCallersTasks() throws Exception {
        TaskResponse task = new TaskResponse(1L, "Write tests", null, TaskStatus.TODO,
                TaskPriority.HIGH, null, null, Instant.now(), Instant.now());
        when(taskService.list(eq(42L), isNull())).thenReturn(List.of(task));

        mvc.perform(get("/api/tasks").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Write tests"));

        verify(taskService).list(eq(42L), isNull());
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mvc.perform(post("/api/tasks")
                        .with(jwt().jwt(j -> j.subject("42")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Title is required"));

        verifyNoInteractions(taskService);
    }

    @Test
    void createsTaskForCaller() throws Exception {
        TaskResponse created = new TaskResponse(5L, "Deploy", null, TaskStatus.TODO,
                TaskPriority.MEDIUM, null, null, Instant.now(), Instant.now());
        when(taskService.create(eq(42L), any(TaskRequest.class))).thenReturn(created);

        mvc.perform(post("/api/tasks")
                        .with(jwt().jwt(j -> j.subject("42")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Deploy\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void returns404ForSomeoneElsesTask() throws Exception {
        when(taskService.get(42L, 7L)).thenThrow(new TaskNotFoundException(7L));

        mvc.perform(get("/api/tasks/7").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isNotFound());
    }
}
