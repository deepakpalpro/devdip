package com.banking.forms.pipeline.application;

import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges submission lifecycle transitions to the async pipeline outbox. Uses
 * {@link TransactionalEventListener} with {@code AFTER_COMMIT} so a pipeline run is only queued once
 * the submit transition has durably committed. Fail-safe — never affects the source flow.
 */
@Component
public class PipelineLifecycleListener {

    private final PipelineOutboxService outboxService;

    public PipelineLifecycleListener(PipelineOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLifecycleEvent(SubmissionLifecycleEvent event) {
        outboxService.enqueue(event);
    }
}
