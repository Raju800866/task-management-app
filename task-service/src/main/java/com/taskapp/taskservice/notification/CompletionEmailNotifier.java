package com.taskapp.taskservice.notification;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails the task owner when a task is completed.
 * Runs after commit (no email for rolled-back changes) and on a background
 * thread, so a slow or failing mail server never affects the API response.
 */
@Component
public class CompletionEmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(CompletionEmailNotifier.class);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String appUrl;

    public CompletionEmailNotifier(ObjectProvider<JavaMailSender> mailSender,
                                   @Value("${app.notifications.email.enabled:false}") boolean enabled,
                                   @Value("${app.notifications.email.from}") String from,
                                   @Value("${app.notifications.app-url}") String appUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        if (!enabled) {
            log.debug("Email notifications disabled; skipping task {}", event.taskId());
            return;
        }
        if (event.ownerEmail() == null || event.ownerEmail().isBlank()) {
            log.warn("No email claim in token; cannot notify for task {}", event.taskId());
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Email notifications enabled but no mail server configured (MAIL_HOST)");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.ownerEmail());
        message.setSubject("Task completed: " + event.title());
        message.setText("""
                Hi,

                Nice work! Your task "%s" was marked as done on %s.

                Open TaskFlow: %s

                — TaskFlow
                """.formatted(event.title(), TIME.format(event.completedAt()), appUrl));
        try {
            sender.send(message);
            log.info("Sent completion email for task {}", event.taskId());
        } catch (MailException e) {
            // Never propagate: the task is already saved.
            log.warn("Failed to send completion email for task {}: {}", event.taskId(), e.getMessage());
        }
    }
}
