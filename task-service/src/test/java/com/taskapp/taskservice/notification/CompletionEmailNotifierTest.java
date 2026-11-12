package com.taskapp.taskservice.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class CompletionEmailNotifierTest {

    private static final TaskCompletedEvent EVENT =
            new TaskCompletedEvent(7L, "Write report", "ann@example.com", Instant.parse("2026-11-12T09:30:00Z"));

    private JavaMailSender sender;
    private ObjectProvider<JavaMailSender> provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sender = mock(JavaMailSender.class);
        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
    }

    private CompletionEmailNotifier notifier(boolean enabled) {
        return new CompletionEmailNotifier(provider, enabled, "TaskFlow <no-reply@test>", "https://app.test");
    }

    @Test
    void sendsEmailToOwner() {
        notifier(true).onTaskCompleted(EVENT);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("ann@example.com");
        assertThat(msg.getSubject()).isEqualTo("Task completed: Write report");
        assertThat(msg.getText()).contains("\"Write report\"", "12 Nov 2026, 09:30 UTC", "https://app.test");
    }

    @Test
    void doesNothingWhenDisabled() {
        notifier(false).onTaskCompleted(EVENT);

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsWhenOwnerEmailMissing() {
        notifier(true).onTaskCompleted(new TaskCompletedEvent(7L, "Write report", null, Instant.now()));

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void mailFailureIsSwallowed() {
        doThrow(new MailSendException("SMTP down")).when(sender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(() -> notifier(true).onTaskCompleted(EVENT));
    }
}
