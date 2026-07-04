package com.banking.forms.notification.application;

import com.banking.forms.notification.domain.NotificationMessage;
import com.banking.forms.notification.domain.NotificationStatus;
import com.banking.forms.notification.infrastructure.NotificationMessageRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox for PENDING messages and hands each to {@link NotificationService#dispatch} (a
 * fresh transaction per message so a single failure cannot roll back the batch). Decoupling enqueue
 * from delivery gives async processing, retries, and dead-lettering without a broker; swapping the
 * poll for a broker consumer later is a localized change.
 */
@Component
public class NotificationDispatcher {

    private final NotificationMessageRepository messageRepository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;

    public NotificationDispatcher(
            NotificationMessageRepository messageRepository,
            NotificationService notificationService,
            NotificationProperties properties) {
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${notifications.dispatch-interval-ms:5000}")
    public void dispatchPending() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        for (NotificationMessage message : pending()) {
            if (isEligible(message, now)) {
                notificationService.dispatch(message.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationMessage> pending() {
        return messageRepository.findByStatusOrderByCreatedAtAsc(
                NotificationStatus.PENDING.name(), PageRequest.of(0, properties.getDispatchBatchSize()));
    }

    /** First attempt is immediate; retries wait a backoff that grows with the attempt count. */
    private boolean isEligible(NotificationMessage message, Instant now) {
        if (message.getAttempts() == 0) {
            return true;
        }
        long waitMs = properties.getRetryBackoffMs() * message.getAttempts();
        return message.getUpdatedAt().plusMillis(waitMs).isBefore(now);
    }
}
