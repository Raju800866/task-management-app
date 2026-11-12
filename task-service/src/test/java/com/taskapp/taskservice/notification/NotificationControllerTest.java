package com.taskapp.taskservice.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.taskapp.taskservice.config.SecurityConfig;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void requiresToken() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/notifications/read-all")).andExpect(status().isUnauthorized());
        verifyNoInteractions(notificationService);
    }

    @Test
    void listsCallersNotifications() throws Exception {
        when(notificationService.list(42L)).thenReturn(List.of(
                new NotificationResponse(2L, 7L, "raju moved \"Report\" to Done", false, Instant.now())));

        mvc.perform(get("/api/notifications").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("raju moved \"Report\" to Done"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void returnsUnreadCount() throws Exception {
        when(notificationService.unreadCount(42L)).thenReturn(3L);

        mvc.perform(get("/api/notifications/unread-count").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void marksAllReadForCaller() throws Exception {
        mvc.perform(patch("/api/notifications/read-all").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isNoContent());
        verify(notificationService).markAllRead(42L);
    }
}
