package com.banking.forms.submission.application.event;

import com.banking.forms.submission.domain.SubmissionStatus;
import java.util.UUID;

/**
 * Published (via Spring's {@code ApplicationEventPublisher}) whenever a submission changes status —
 * on submit and on each review decision. Downstream modules (e.g. notifications) subscribe with a
 * {@code @TransactionalEventListener} so side effects run only after the transition has committed.
 *
 * <p>The submission module deliberately knows nothing about who consumes this event; it just announces
 * a domain fact.
 */
public record SubmissionLifecycleEvent(
        UUID tenantId,
        UUID submissionId,
        UUID userId,
        UUID formVersionId,
        SubmissionStatus fromStatus,
        SubmissionStatus toStatus) {}
