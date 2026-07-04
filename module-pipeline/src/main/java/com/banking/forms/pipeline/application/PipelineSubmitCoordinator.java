package com.banking.forms.pipeline.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Entry point for triggering the pipeline after a consumer submit. In sync mode runs immediately;
 * in async mode the {@link PipelineLifecycleListener} enqueues an outbox row instead.
 */
@Service
public class PipelineSubmitCoordinator {

    private final SubmissionPipelineService pipelineService;
    private final PipelineProperties properties;

    public PipelineSubmitCoordinator(
            SubmissionPipelineService pipelineService, PipelineProperties properties) {
        this.pipelineService = pipelineService;
        this.properties = properties;
    }

    public void onSubmitted(UUID tenantId, UUID submissionId) {
        if (properties.isSync()) {
            pipelineService.process(tenantId, submissionId);
        }
    }
}
