package com.banking.forms.downstream.application;

import com.banking.forms.downstream.domain.OutboxEvent;
import com.banking.forms.downstream.domain.OutboxStatus;
import com.banking.forms.downstream.infrastructure.OutboxEventRepository;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates downstream delivery. On pipeline completion it fans out one {@link OutboxEvent} per
 * enabled provider (enqueued PENDING in the durable outbox within the pipeline transaction) and
 * records a timeline event. The async {@code DownstreamDispatcher} later delivers each row via its
 * connector. Every path is fail-safe — downstream errors never break the submission flow.
 */
@Service
public class DownstreamDispatchService {

    private static final Logger log = LoggerFactory.getLogger(DownstreamDispatchService.class);
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final DownstreamConnectorRouter connectorRouter;
    private final OutboxEventRepository outboxRepository;
    private final SubmissionEventRecorder eventRecorder;
    private final DownstreamProperties properties;
    private final ObjectMapper objectMapper;

    public DownstreamDispatchService(
            DownstreamConnectorRouter connectorRouter,
            OutboxEventRepository outboxRepository,
            SubmissionEventRecorder eventRecorder,
            DownstreamProperties properties,
            ObjectMapper objectMapper) {
        this.connectorRouter = connectorRouter;
        this.outboxRepository = outboxRepository;
        this.eventRecorder = eventRecorder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueue outbox rows for all enabled providers with an implementation. Called from the pipeline
     * in the same transaction that advances the submission — the outbox write commits atomically with
     * the state change.
     *
     * @return number of rows queued
     */
    @Transactional
    public int enqueueForSubmission(
            UUID tenantId,
            UUID submissionId,
            String formCode,
            Map<String, Map<String, Object>> sanitizedPayload,
            String riskRecommendation,
            Double riskScore) {
        if (!properties.isEnabled()) {
            recordTimeline(submissionId, "DOWNSTREAM_SKIPPED", Map.of("reason", "disabled"));
            return 0;
        }

        List<DownstreamConnectorRouter.Selection> providers = connectorRouter.resolveAllEnabled();
        if (providers.isEmpty()) {
            recordTimeline(submissionId, "DOWNSTREAM_SKIPPED", Map.of("reason", "no-provider"));
            return 0;
        }

        String payloadJson = buildPayloadJson(submissionId, formCode, sanitizedPayload, riskRecommendation, riskScore);
        int queued = 0;
        for (DownstreamConnectorRouter.Selection selection : providers) {
            OutboxEvent event = new OutboxEvent(
                    UUID.randomUUID(),
                    tenantId,
                    submissionId,
                    DownstreamEventTypes.SUBMISSION_PROCESSED,
                    formCode,
                    selection.providerCode(),
                    selection.connectorType(),
                    payloadJson,
                    OutboxStatus.PENDING);
            outboxRepository.save(event);
            queued++;
            recordTimeline(
                    submissionId,
                    "DOWNSTREAM_QUEUED",
                    Map.of(
                            "provider", selection.providerCode(),
                            "connectorType", selection.connectorType(),
                            "event", DownstreamEventTypes.SUBMISSION_PROCESSED));
        }
        return queued;
    }

    /** Deliver a single queued outbox row. Fail-safe with retry/dead-letter semantics. */
    @Transactional
    public void dispatch(UUID outboxId) {
        OutboxEvent event = outboxRepository.findById(outboxId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        var selection = connectorRouter.resolveProvider(event.getProviderCode());
        if (selection.isEmpty()) {
            event.markFailed("No implementation for provider " + event.getProviderCode());
            outboxRepository.save(event);
            recordDelivery(event, false, "no-implementation");
            return;
        }

        OutboundEnvelope envelope = new OutboundEnvelope(
                event.getTenantId(),
                event.getSubmissionId(),
                event.getFormCode(),
                event.getEventType(),
                event.getPayloadJson());

        DispatchResult result;
        try {
            result = selection.get().connector().dispatch(envelope, selection.get().config());
        } catch (Exception ex) {
            result = DispatchResult.failed("connector threw: " + ex.getMessage());
        }

        if (result.isDispatched()) {
            event.markDispatched(result.providerRef());
            outboxRepository.save(event);
            recordDelivery(event, true, result.providerRef());
        } else if (event.getAttempts() + 1 >= properties.getMaxAttempts()) {
            event.markFailed(result.detail());
            outboxRepository.save(event);
            recordDelivery(event, false, result.detail());
        } else {
            event.markRetryable(result.detail()); // stays PENDING, retried on the next tick
            outboxRepository.save(event);
        }
    }

    private String buildPayloadJson(
            UUID submissionId,
            String formCode,
            Map<String, Map<String, Object>> sanitizedPayload,
            String riskRecommendation,
            Double riskScore) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("submissionId", submissionId.toString());
        envelope.put("formCode", formCode);
        envelope.put("eventType", DownstreamEventTypes.SUBMISSION_PROCESSED);
        envelope.put("sections", sanitizedPayload == null ? Map.of() : sanitizedPayload);
        if (riskRecommendation != null) {
            envelope.put("riskRecommendation", riskRecommendation);
        }
        if (riskScore != null) {
            envelope.put("riskScore", riskScore);
        }
        return writeJson(envelope);
    }

    private void recordDelivery(OutboxEvent event, boolean dispatched, String detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", event.getProviderCode());
        payload.put("connectorType", event.getConnectorType());
        payload.put("event", event.getEventType());
        if (detail != null) {
            payload.put("detail", detail);
        }
        recordTimeline(
                event.getSubmissionId(),
                dispatched ? "DOWNSTREAM_DISPATCHED" : "DOWNSTREAM_FAILED",
                payload);
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize downstream payload", ex);
        }
    }
}
