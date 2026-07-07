package com.banking.forms.notification.application;

import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges submission lifecycle transitions to the notification subsystem. Uses
 * {@link TransactionalEventListener} with {@code AFTER_COMMIT} so a customer is only notified once the
 * transition has durably committed; {@code fallbackExecution} keeps it working if the publisher runs
 * outside a transaction (e.g. in tests). The handler is fail-safe and never affects the source flow.
 */
@Component
public class NotificationLifecycleListener {

    private final NotificationService notificationService;

    public NotificationLifecycleListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLifecycleEvent(SubmissionLifecycleEvent event) {
        notificationService.handle(event);
    }
}
