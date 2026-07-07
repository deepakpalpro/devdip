package com.banking.forms.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.pipeline.domain.PipelineDefinition;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.banking.forms.pipeline.infrastructure.AiEvaluationRepository;
import com.banking.forms.pipeline.infrastructure.PipelineExecutionRepository;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionPipelineServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();

    @Mock private SubmissionRepository submissionRepository;
    @Mock private FormPipelineResolver pipelineResolver;
    @Mock private PipelineOrchestrator orchestrator;
    @Mock private PipelineExecutionRepository executionRepository;
    @Mock private SanitizedPayloadRepository sanitizedPayloadRepository;
    @Mock private AiEvaluationRepository aiEvaluationRepository;
    @Mock private Submission submission;

    private SubmissionPipelineService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionPipelineService(
                submissionRepository,
                pipelineResolver,
                orchestrator,
                executionRepository,
                sanitizedPayloadRepository,
                aiEvaluationRepository,
                new ObjectMapper());
    }

    @Test
    void delegatesSubmitPipelineToOrchestrator() {
        PipelineDefinition definition = new PipelineDefinition(
                UUID.randomUUID(), TENANT, "custom", "Custom", null, 1, false);
        ResolvedPipeline resolved = new ResolvedPipeline(definition, java.util.List.of());

        when(submission.getTenantId()).thenReturn(TENANT);
        when(submission.getFormVersionId()).thenReturn(VERSION);
        when(submissionRepository.findById(SUBMISSION)).thenReturn(Optional.of(submission));
        when(pipelineResolver.resolve(TENANT, VERSION, PipelineTrigger.ON_SUBMIT)).thenReturn(Optional.of(resolved));
        when(orchestrator.execute(TENANT, SUBMISSION, resolved, PipelineTrigger.ON_SUBMIT))
                .thenReturn(PipelineResult.completed(2));

        PipelineResult result = service.process(TENANT, SUBMISSION);

        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(orchestrator).execute(TENANT, SUBMISSION, resolved, PipelineTrigger.ON_SUBMIT);
    }

    @Test
    void processTriggerReturnsSkippedWhenNoBinding() {
        when(submission.getTenantId()).thenReturn(TENANT);
        when(submission.getFormVersionId()).thenReturn(VERSION);
        when(submissionRepository.findById(SUBMISSION)).thenReturn(Optional.of(submission));
        when(pipelineResolver.resolve(TENANT, VERSION, PipelineTrigger.ON_APPROVED)).thenReturn(Optional.empty());

        PipelineResult result = service.processTrigger(TENANT, SUBMISSION, PipelineTrigger.ON_APPROVED);

        assertThat(result.status()).isEqualTo("SKIPPED");
    }
}
