package com.banking.forms.pipeline.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.pipeline.domain.AiEvaluation;
import com.banking.forms.pipeline.domain.PipelineExecution;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.AiEvaluationRepository;
import com.banking.forms.pipeline.infrastructure.PipelineExecutionRepository;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry point for automated pipeline runs. Resolves the configured pipeline for the submission's
 * form version and delegates step execution to {@link PipelineOrchestrator}.
 */
@Service
public class SubmissionPipelineService {

    private static final int LEGACY_TOTAL_STEPS = 5;

    private final SubmissionRepository submissionRepository;
    private final FormPipelineResolver pipelineResolver;
    private final PipelineOrchestrator orchestrator;
    private final PipelineExecutionRepository executionRepository;
    private final SanitizedPayloadRepository sanitizedPayloadRepository;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final ObjectMapper objectMapper;

    public SubmissionPipelineService(
            SubmissionRepository submissionRepository,
            FormPipelineResolver pipelineResolver,
            PipelineOrchestrator orchestrator,
            PipelineExecutionRepository executionRepository,
            SanitizedPayloadRepository sanitizedPayloadRepository,
            AiEvaluationRepository aiEvaluationRepository,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.pipelineResolver = pipelineResolver;
        this.orchestrator = orchestrator;
        this.executionRepository = executionRepository;
        this.sanitizedPayloadRepository = sanitizedPayloadRepository;
        this.aiEvaluationRepository = aiEvaluationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PipelineResult process(UUID tenantId, UUID submissionId) {
        Submission submission = loadOwned(tenantId, submissionId);
        ResolvedPipeline pipeline = pipelineResolver
                .resolve(tenantId, submission.getFormVersionId(), PipelineTrigger.ON_SUBMIT)
                .orElseThrow(() -> new PipelineConfigurationException("No submit pipeline configured"));
        return orchestrator.execute(tenantId, submissionId, pipeline, PipelineTrigger.ON_SUBMIT);
    }

    @Transactional
    public PipelineResult processTrigger(UUID tenantId, UUID submissionId, PipelineTrigger trigger) {
        Submission submission = loadOwned(tenantId, submissionId);
        return pipelineResolver
                .resolve(tenantId, submission.getFormVersionId(), trigger)
                .map(pipeline -> orchestrator.execute(tenantId, submissionId, pipeline, trigger))
                .orElse(PipelineResult.skipped("no-pipeline"));
    }

    @Transactional(readOnly = true)
    public PipelineReportView getReport(UUID tenantId, UUID submissionId) {
        loadOwned(tenantId, submissionId);
        PipelineExecutionView executionView = executionRepository
                .findFirstBySubmissionIdOrderByStartedAtDesc(submissionId)
                .map(this::toExecutionView)
                .orElse(null);
        SanitizedPayload sanitized = sanitizedPayloadRepository.findBySubmissionId(submissionId).orElse(null);
        Map<String, Map<String, Object>> payload = sanitized == null ? null : readSections(sanitized.getPayloadJson());
        List<TransformedFieldView> transformed =
                sanitized == null ? List.of() : readTransformed(sanitized.getTransformedJson());
        AiEvaluationView aiEvaluation = aiEvaluationRepository
                .findBySubmissionId(submissionId)
                .map(this::toAiView)
                .orElse(null);
        return new PipelineReportView(executionView, payload, transformed, aiEvaluation);
    }

    private Submission loadOwned(UUID tenantId, UUID submissionId) {
        return submissionRepository
                .findById(submissionId)
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }

    private AiEvaluationView toAiView(AiEvaluation evaluation) {
        return new AiEvaluationView(
                evaluation.getEvaluatorId(),
                evaluation.getModel(),
                evaluation.getRiskScore(),
                evaluation.getRecommendation(),
                evaluation.getRationale(),
                readSignals(evaluation.getSignalsJson()),
                evaluation.getCreatedAt());
    }

    private Map<String, Object> readSignals(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private PipelineExecutionView toExecutionView(PipelineExecution execution) {
        return new PipelineExecutionView(
                execution.getStatus(),
                execution.getCurrentStep(),
                LEGACY_TOTAL_STEPS,
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getErrorDetails());
    }

    private Map<String, Map<String, Object>> readSections(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<TransformedFieldView> readTransformed(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
