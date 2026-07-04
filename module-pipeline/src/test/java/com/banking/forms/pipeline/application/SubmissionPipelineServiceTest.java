package com.banking.forms.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.downstream.application.DownstreamDispatchService;
import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.pipeline.domain.AiEvaluation;
import com.banking.forms.pipeline.domain.PipelineExecution;
import com.banking.forms.pipeline.infrastructure.AiEvaluationRepository;
import com.banking.forms.pipeline.infrastructure.PipelineExecutionRepository;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiRecommendation;
import com.banking.forms.pipeline.spi.ServiceCallExecutor;
import com.banking.forms.submission.application.SectionStorageRouter;
import com.banking.forms.submission.application.SectionStorageStrategy;
import com.banking.forms.submission.application.SectionValidator;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.banking.forms.transformation.application.PiiScrubber;
import com.banking.forms.transformation.application.ScrubResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionPipelineServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();

    @Mock private SubmissionRepository submissionRepository;
    @Mock private SectionStorageRouter sectionStorageRouter;
    @Mock private SectionStorageStrategy sectionStorage;
    @Mock private SectionValidator sectionValidator;
    @Mock private FormQueryService formQueryService;
    @Mock private SubmissionEventRecorder eventRecorder;
    @Mock private PiiScrubber piiScrubber;
    @Mock private PipelineExecutionRepository executionRepository;
    @Mock private SanitizedPayloadRepository sanitizedPayloadRepository;
    @Mock private AiEvaluatorRouter aiEvaluatorRouter;
    @Mock private AiEvaluationRepository aiEvaluationRepository;
    @Mock private DownstreamDispatchService downstreamDispatchService;
    @Mock private ServiceCallExecutor serviceCallExecutor;
    @Mock private Submission submission;

    private SubmissionPipelineService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionPipelineService(
                submissionRepository,
                sectionStorageRouter,
                sectionValidator,
                formQueryService,
                eventRecorder,
                piiScrubber,
                executionRepository,
                sanitizedPayloadRepository,
                aiEvaluatorRouter,
                aiEvaluationRepository,
                downstreamDispatchService,
                serviceCallExecutor,
                new ObjectMapper());

        Map<String, Map<String, Object>> sectionData = Map.of("loan", Map.of("amount", 5000));

        when(submission.getTenantId()).thenReturn(TENANT);
        when(submission.getStatus()).thenReturn(SubmissionStatus.SUBMITTED);
        when(submission.getFormVersionId()).thenReturn(VERSION);
        when(submissionRepository.findById(SUBMISSION)).thenReturn(Optional.of(submission));
        when(executionRepository.save(any(PipelineExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(formQueryService.findPublishedByVersionId(VERSION))
                .thenReturn(Optional.of(new PublishedFormView(
                        UUID.randomUUID(), VERSION, "LOAN", "Loan", "lending", StorageStrategy.JSON_BLOB, null)));
        when(sectionStorageRouter.resolve(StorageStrategy.JSON_BLOB)).thenReturn(sectionStorage);
        when(sectionStorage.loadAllSections(SUBMISSION)).thenReturn(sectionData);
        when(piiScrubber.scrub(eq("LOAN"), any())).thenReturn(new ScrubResult(sectionData, List.of()));
        when(sanitizedPayloadRepository.findBySubmissionId(SUBMISSION)).thenReturn(Optional.empty());
        when(aiEvaluationRepository.findBySubmissionId(SUBMISSION)).thenReturn(Optional.empty());
        when(downstreamDispatchService.enqueueForSubmission(
                        eq(TENANT), eq(SUBMISSION), anyString(), any(), any(), any()))
                .thenReturn(1);
        when(serviceCallExecutor.isEnabled()).thenReturn(true);
        when(serviceCallExecutor.invoke(any())).thenReturn(1);
    }

    @Test
    void runsAiStepAndPersistsResult() {
        when(aiEvaluatorRouter.isEnabled()).thenReturn(true);
        when(aiEvaluatorRouter.evaluate(any())).thenReturn(new AiEvaluationResult(
                "heuristic", "rules-v1", 0.12, AiRecommendation.APPROVE, "looks fine", Map.of("x", 1), 3L));

        PipelineResult result = service.process(TENANT, SUBMISSION);

        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(submission).markUnderReview(any());
        verify(eventRecorder).record(eq(SUBMISSION), eq("AI_EVALUATED"), any(), any());
        verify(eventRecorder).record(eq(SUBMISSION), eq("DOWNSTREAM_ENQUEUED"), any(), any());
        verify(serviceCallExecutor).invoke(any());
        verify(downstreamDispatchService)
                .enqueueForSubmission(eq(TENANT), eq(SUBMISSION), eq("LOAN"), any(), eq("APPROVE"), eq(0.12));

        ArgumentCaptor<AiEvaluation> captor = ArgumentCaptor.forClass(AiEvaluation.class);
        verify(aiEvaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecommendation()).isEqualTo("APPROVE");
        assertThat(captor.getValue().getEvaluatorId()).isEqualTo("heuristic");
    }

    @Test
    void skipsAiStepWhenDisabled() {
        when(aiEvaluatorRouter.isEnabled()).thenReturn(false);

        PipelineResult result = service.process(TENANT, SUBMISSION);

        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(eventRecorder).record(eq(SUBMISSION), eq("AI_EVALUATION_SKIPPED"), any(), any());
        verify(aiEvaluationRepository, never()).save(any());
        verify(aiEvaluatorRouter, never()).evaluate(any());
    }

    @Test
    void skipsWhenSubmissionNotSubmitted() {
        when(submission.getStatus()).thenReturn(SubmissionStatus.DRAFT);

        PipelineResult result = service.process(TENANT, SUBMISSION);

        assertThat(result.status()).isEqualTo("SKIPPED");
        verify(eventRecorder, never()).record(eq(SUBMISSION), eq("AI_EVALUATED"), any(), any());
    }
}
