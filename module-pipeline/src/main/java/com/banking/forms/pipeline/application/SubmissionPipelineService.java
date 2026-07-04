package com.banking.forms.pipeline.application;

import com.banking.forms.downstream.application.DownstreamDispatchService;
import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.pipeline.domain.AiEvaluation;
import com.banking.forms.pipeline.domain.PipelineExecution;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.AiEvaluationRepository;
import com.banking.forms.pipeline.infrastructure.PipelineExecutionRepository;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.ServiceCallContext;
import com.banking.forms.pipeline.spi.ServiceCallExecutor;
import com.banking.forms.submission.application.SectionStorageRouter;
import com.banking.forms.submission.application.SectionValidator;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.application.SubmissionValidationException;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.banking.forms.transformation.application.PiiScrubber;
import com.banking.forms.transformation.application.ScrubResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the automated processing pipeline after a submission is submitted:
 * {@code VALIDATE → PII_SCRUB → AI_EVALUATE → SERVICE_CALL → DOWNSTREAM}, advancing the submission
 * {@code SUBMITTED → VALIDATING → PROCESSING → PENDING_REVIEW} and recording every step in the
 * submission audit timeline. In async mode ({@code pipeline.process-mode=async}, the default) the
 * pipeline is triggered by {@link PipelineOutboxDispatcher} rather than inline on submit.
 */
@Service
public class SubmissionPipelineService {

    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);
    private static final int TOTAL_STEPS = 5;

    private final SubmissionRepository submissionRepository;
    private final SectionStorageRouter sectionStorageRouter;
    private final SectionValidator sectionValidator;
    private final FormQueryService formQueryService;
    private final SubmissionEventRecorder eventRecorder;
    private final PiiScrubber piiScrubber;
    private final PipelineExecutionRepository executionRepository;
    private final SanitizedPayloadRepository sanitizedPayloadRepository;
    private final AiEvaluatorRouter aiEvaluatorRouter;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final DownstreamDispatchService downstreamDispatchService;
    private final ServiceCallExecutor serviceCallExecutor;
    private final ObjectMapper objectMapper;

    public SubmissionPipelineService(
            SubmissionRepository submissionRepository,
            SectionStorageRouter sectionStorageRouter,
            SectionValidator sectionValidator,
            FormQueryService formQueryService,
            SubmissionEventRecorder eventRecorder,
            PiiScrubber piiScrubber,
            PipelineExecutionRepository executionRepository,
            SanitizedPayloadRepository sanitizedPayloadRepository,
            AiEvaluatorRouter aiEvaluatorRouter,
            AiEvaluationRepository aiEvaluationRepository,
            DownstreamDispatchService downstreamDispatchService,
            ServiceCallExecutor serviceCallExecutor,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.sectionStorageRouter = sectionStorageRouter;
        this.sectionValidator = sectionValidator;
        this.formQueryService = formQueryService;
        this.eventRecorder = eventRecorder;
        this.piiScrubber = piiScrubber;
        this.executionRepository = executionRepository;
        this.sanitizedPayloadRepository = sanitizedPayloadRepository;
        this.aiEvaluatorRouter = aiEvaluatorRouter;
        this.aiEvaluationRepository = aiEvaluationRepository;
        this.downstreamDispatchService = downstreamDispatchService;
        this.serviceCallExecutor = serviceCallExecutor;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PipelineResult process(UUID tenantId, UUID submissionId) {
        Submission submission = loadOwned(tenantId, submissionId);
        if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
            return PipelineResult.skipped(submission.getStatus().name());
        }

        PipelineExecution execution = executionRepository.save(new PipelineExecution(UUID.randomUUID(), submissionId));
        record(submissionId, "PIPELINE_STARTED",
                payload("steps", "VALIDATE,PII_SCRUB,AI_EVALUATE,SERVICE_CALL,DOWNSTREAM"));

        try {
            PublishedFormView form = formQueryService
                    .findPublishedByVersionId(submission.getFormVersionId())
                    .orElseThrow(() -> new SubmissionValidationException("Published form version not available"));

            // Step 1 — VALIDATE (server-side defense in depth)
            submission.markValidating(Instant.now());
            submissionRepository.save(submission);
            Map<String, Map<String, Object>> sectionData =
                    sectionStorageRouter.resolve(form.storageStrategy()).loadAllSections(submissionId);
            sectionValidator.validateAllSections(form.schema(), sectionData);
            execution.advanceTo(1);
            record(submissionId, "VALIDATED", payload("sections", sectionData.size()));

            // Step 2 — PII_SCRUB (sanitized copy for AI / analytics / downstream)
            submission.markProcessing(Instant.now());
            submissionRepository.save(submission);
            ScrubResult scrub = piiScrubber.scrub(form.code(), sectionData);
            persistSanitized(submissionId, scrub);
            execution.advanceTo(2);
            record(submissionId, "PII_SCRUBBED", payload("transformedFields", scrub.transformedCount()));

            // Step 3 — AI_EVALUATE (advisory risk score on the sanitized payload; fail-safe → REVIEW)
            String riskRecommendation = null;
            Double riskScore = null;
            if (aiEvaluatorRouter.isEnabled()) {
                AiEvaluationContext aiContext = new AiEvaluationContext(
                        submissionId, form.code(), scrub.sanitized(), Map.of("formCode", form.code()));
                AiEvaluationResult aiResult = aiEvaluatorRouter.evaluate(aiContext);
                persistAiEvaluation(submissionId, aiResult);
                riskRecommendation = aiResult.recommendation().name();
                riskScore = aiResult.riskScore();
                record(submissionId, "AI_EVALUATED",
                        payload(
                                "recommendation", riskRecommendation,
                                "riskScore", riskScore,
                                "evaluator", aiResult.evaluatorId()));
            } else {
                record(submissionId, "AI_EVALUATION_SKIPPED", payload("reason", "disabled"));
            }
            execution.advanceTo(3);

            // Step 4 — SERVICE_CALL (external API adapters on sanitized payload; fail-safe)
            int serviceInvoked = 0;
            if (serviceCallExecutor.isEnabled()) {
                serviceInvoked = serviceCallExecutor.invoke(new ServiceCallContext(
                        tenantId, submissionId, form.code(), scrub.sanitized(), riskRecommendation, riskScore));
                record(submissionId, "SERVICE_CALL_INVOKED", payload("invoked", serviceInvoked));
            } else {
                record(submissionId, "SERVICE_CALL_SKIPPED", payload("reason", "disabled"));
            }
            execution.advanceTo(4);

            // Step 5 — DOWNSTREAM (enqueue sanitized payload to enabled connectors via transactional outbox)
            int queued = downstreamDispatchService.enqueueForSubmission(
                    tenantId, submissionId, form.code(), scrub.sanitized(), riskRecommendation, riskScore);
            record(submissionId, "DOWNSTREAM_ENQUEUED", payload("queued", queued));
            execution.advanceTo(TOTAL_STEPS);

            // Automated stages passed → hand off to the manual review queue
            submission.markUnderReview(Instant.now());
            submissionRepository.save(submission);
            execution.complete(TOTAL_STEPS);
            executionRepository.save(execution);
            record(submissionId, "PIPELINE_COMPLETED",
                    payload("to", SubmissionStatus.PENDING_REVIEW.name(), "transformedFields", scrub.transformedCount()));
            return PipelineResult.completed(scrub.transformedCount());
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            submission.revertToSubmitted(Instant.now());
            submissionRepository.save(submission);
            execution.fail(execution.getCurrentStep(), message);
            executionRepository.save(execution);
            record(submissionId, "PIPELINE_FAILED", payload("error", message, "atStep", execution.getCurrentStep()));
            return PipelineResult.failed(message);
        }
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

    private void persistSanitized(UUID submissionId, ScrubResult scrub) {
        String payloadJson = writeJson(scrub.sanitized());
        String transformedJson = writeJson(scrub.transformed());
        sanitizedPayloadRepository
                .findBySubmissionId(submissionId)
                .ifPresentOrElse(
                        existing -> existing.update(payloadJson, transformedJson),
                        () -> sanitizedPayloadRepository.save(
                                new SanitizedPayload(UUID.randomUUID(), submissionId, payloadJson, transformedJson)));
    }

    private void persistAiEvaluation(UUID submissionId, AiEvaluationResult result) {
        String signalsJson = writeJson(result.signals());
        aiEvaluationRepository
                .findBySubmissionId(submissionId)
                .ifPresentOrElse(
                        existing -> existing.update(
                                result.evaluatorId(),
                                result.model(),
                                result.riskScore(),
                                result.recommendation().name(),
                                result.rationale(),
                                signalsJson,
                                result.processingTimeMs()),
                        () -> aiEvaluationRepository.save(new AiEvaluation(
                                UUID.randomUUID(),
                                submissionId,
                                result.evaluatorId(),
                                result.model(),
                                result.riskScore(),
                                result.recommendation().name(),
                                result.rationale(),
                                signalsJson,
                                result.processingTimeMs())));
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
                TOTAL_STEPS,
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getErrorDetails());
    }

    private void record(UUID submissionId, String eventType, Map<String, Object> payload) {
        eventRecorder.record(submissionId, eventType, payload, SYSTEM_ACTOR);
    }

    private static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize pipeline payload", ex);
        }
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
