package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineJobDefinition;
import com.banking.forms.pipeline.domain.PipelineJobType;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.infrastructure.PipelineJobDefinitionRepository;
import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Runs real-time pipeline jobs when lifecycle events match configured triggers. */
@Component
public class PipelineJobTriggerListener {

    private final PipelineJobDefinitionRepository jobRepository;
    private final PipelineJobRunner jobRunner;
    private final SubmissionRepository submissionRepository;

    public PipelineJobTriggerListener(
            PipelineJobDefinitionRepository jobRepository,
            PipelineJobRunner jobRunner,
            SubmissionRepository submissionRepository) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
        this.submissionRepository = submissionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLifecycleEvent(SubmissionLifecycleEvent event) {
        if (event.toStatus() == SubmissionStatus.SUBMITTED) {
            return;
        }
        submissionRepository.findById(event.submissionId()).ifPresent(submission -> {
            PipelineTrigger trigger = PipelineTrigger.fromReviewTarget(event.toStatus().name());
            for (PipelineJobDefinition job :
                    jobRepository.findByTenantIdAndFormVersionIdAndJobTypeAndTriggerEventAndEnabledTrue(
                            event.tenantId(),
                            submission.getFormVersionId(),
                            PipelineJobType.REALTIME,
                            trigger.name())) {
                try {
                    jobRunner.run(event.tenantId(), job.getId(), event.submissionId());
                } catch (RuntimeException ignored) {
                    // fail-safe
                }
            }
        });
    }
}
