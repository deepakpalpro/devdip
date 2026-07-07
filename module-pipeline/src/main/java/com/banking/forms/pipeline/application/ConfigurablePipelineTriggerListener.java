package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import com.banking.forms.submission.domain.SubmissionStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Runs configured pipelines on review lifecycle transitions (ON_APPROVED, ON_REJECTED, etc.). */
@Component
public class ConfigurablePipelineTriggerListener {

    private final SubmissionPipelineService pipelineService;

    public ConfigurablePipelineTriggerListener(SubmissionPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLifecycleEvent(SubmissionLifecycleEvent event) {
        if (event.toStatus() == SubmissionStatus.SUBMITTED) {
            return; // handled by async outbox worker
        }
        PipelineTrigger trigger = PipelineTrigger.fromReviewTarget(event.toStatus().name());
        pipelineService.processTrigger(event.tenantId(), event.submissionId(), trigger);
    }
}
