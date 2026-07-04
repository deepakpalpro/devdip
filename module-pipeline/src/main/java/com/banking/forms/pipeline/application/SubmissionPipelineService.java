package com.banking.forms.pipeline.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.pipeline.domain.PipelineExecution;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.PipelineExecutionRepository;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
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
 * {@code VALIDATE → PII_SCRUB → DOWNSTREAM}, advancing the submission
 * {@code SUBMITTED → VALIDATING → PROCESSING → PENDING_REVIEW} and recording every step in the
 * submission audit timeline. Failures are captured (not thrown): the submission is reverted to
 * SUBMITTED and the run is marked FAILED, so a submit request never fails because of the pipeline.
 */
@Service
public class SubmissionPipelineService {

    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);
    private static final int TOTAL_STEPS = 3;

    private final SubmissionRepository submissionRepository;
    private final SectionStorageRouter sectionStorageRouter;
    private final SectionValidator sectionValidator;
    private final FormQueryService formQueryService;
    private final SubmissionEventRecorder eventRecorder;
    private final PiiScrubber piiScrubber;
    private final PipelineExecutionRepository executionRepository;
    private final SanitizedPayloadRepository sanitizedPayloadRepository;
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
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.sectionStorageRouter = sectionStorageRouter;
        this.sectionValidator = sectionValidator;
        this.formQueryService = formQueryService;
        this.eventRecorder = eventRecorder;
        this.piiScrubber = piiScrubber;
        this.executionRepository = executionRepository;
        this.sanitizedPayloadRepository = sanitizedPayloadRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PipelineResult process(UUID tenantId, UUID submissionId) {
        Submission submission = loadOwned(tenantId, submissionId);
        if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
            return PipelineResult.skipped(submission.getStatus().name());
        }

        PipelineExecution execution = executionRepository.save(new PipelineExecution(UUID.randomUUID(), submissionId));
        record(submissionId, "PIPELINE_STARTED", payload("steps", "VALIDATE,PII_SCRUB,DOWNSTREAM"));

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

            // Step 3 — DOWNSTREAM (stubbed dispatch of the sanitized payload)
            record(submissionId, "DOWNSTREAM_DISPATCHED",
                    payload("target", "ANALYTICS_STREAM", "fields", scrub.transformedCount()));
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
        return new PipelineReportView(executionView, payload, transformed);
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
