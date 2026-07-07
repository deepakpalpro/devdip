package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineOutboxEvent;
import com.banking.forms.pipeline.infrastructure.PipelineOutboxRepository;
import com.banking.forms.pipeline.spi.PipelineEventPublisher;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the generic {@code outbox_event} rows that queue pipeline runs. In async mode a submit
 * transition enqueues {@code PIPELINE_REQUESTED}; the {@code PipelineOutboxDispatcher} dequeues and
 * invokes {@link SubmissionPipelineService#process}. Fail-safe — outbox errors never break submit.
 */
@Service
public class PipelineOutboxService {

    private static final Logger log = LoggerFactory.getLogger(PipelineOutboxService.class);
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final PipelineOutboxRepository outboxRepository;
    private final SubmissionPipelineService pipelineService;
    private final SubmissionEventRecorder eventRecorder;
    private final PipelineEventPublisher eventPublisher;
    private final PipelineProperties properties;
    private final ObjectMapper objectMapper;

    public PipelineOutboxService(
            PipelineOutboxRepository outboxRepository,
            SubmissionPipelineService pipelineService,
            SubmissionEventRecorder eventRecorder,
            PipelineEventPublisher eventPublisher,
            PipelineProperties properties,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.pipelineService = pipelineService;
        this.eventRecorder = eventRecorder;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueue a pipeline run after submit. Invoked from an {@code AFTER_COMMIT} listener with
     * {@code REQUIRES_NEW} so the outbox row durably commits.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(SubmissionLifecycleEvent event) {
        try {
            doEnqueue(event);
        } catch (Exception ex) {
            log.warn("Pipeline outbox enqueue failed for submission {}: {}", event.submissionId(), ex.toString());
        }
    }

    private void doEnqueue(SubmissionLifecycleEvent event) {
        if (!properties.isAsync()) {
            return;
        }
        if (event.toStatus() != SubmissionStatus.SUBMITTED) {
            return;
        }
        if (outboxRepository
                .findFirstBySubmissionIdAndEventTypeAndPublishedFalse(
                        event.submissionId(), PipelineEventTypes.PIPELINE_REQUESTED)
                .isPresent()) {
            return; // already queued
        }

        String payloadJson = writePayload(event);
        PipelineOutboxEvent outbox = new PipelineOutboxEvent(
                UUID.randomUUID(),
                PipelineEventTypes.PIPELINE_REQUESTED,
                payloadJson,
                event.tenantId(),
                event.submissionId());
        outboxRepository.save(outbox);
        recordTimeline(
                event.submissionId(),
                "PIPELINE_QUEUED",
                Map.of("outboxId", outbox.getId().toString(), "mode", "async"));
    }

    /** Process a single unpublished outbox row (fresh transaction per row). */
    @Transactional
    public void process(UUID outboxId) {
        PipelineOutboxEvent outbox = outboxRepository.findById(outboxId).orElse(null);
        if (outbox == null || outbox.isPublished()) {
            return;
        }

        try {
            eventPublisher.publish(
                    outbox.getId(),
                    outbox.getEventType(),
                    outbox.getTenantId(),
                    outbox.getSubmissionId(),
                    outbox.getPayloadJson());
            pipelineService.process(outbox.getTenantId(), outbox.getSubmissionId());
            outbox.markPublished();
            outboxRepository.save(outbox);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (outbox.getAttempts() + 1 >= properties.getMaxAttempts()) {
                outbox.markFailed(message);
                outboxRepository.save(outbox);
                recordTimeline(
                        outbox.getSubmissionId(),
                        "PIPELINE_OUTBOX_FAILED",
                        Map.of("outboxId", outbox.getId().toString(), "error", message));
            } else {
                outbox.markRetryable(message);
                outboxRepository.save(outbox);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PipelineOutboxView> listForSubmission(UUID submissionId) {
        return outboxRepository.findBySubmissionIdOrderByOccurredAtAsc(submissionId).stream()
                .map(this::toView)
                .toList();
    }

    private PipelineOutboxView toView(PipelineOutboxEvent event) {
        return new PipelineOutboxView(
                event.getId(),
                event.getSubmissionId(),
                event.getEventType(),
                event.isPublished(),
                event.getAttempts(),
                event.getError(),
                event.getOccurredAt(),
                event.getUpdatedAt());
    }

    private String writePayload(SubmissionLifecycleEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", event.tenantId().toString());
        payload.put("submissionId", event.submissionId().toString());
        payload.put("formVersionId", event.formVersionId().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize pipeline outbox payload", ex);
        }
    }

    private void recordTimeline(UUID submissionId, String type, Map<String, Object> payload) {
        if (submissionId == null) {
            return;
        }
        try {
            eventRecorder.record(submissionId, type, payload, SYSTEM_ACTOR);
        } catch (Exception ex) {
            log.debug("Timeline event {} could not be recorded: {}", type, ex.getMessage());
        }
    }
}
