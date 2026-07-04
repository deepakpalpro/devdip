package com.banking.forms.serviceintegration.application;

import com.banking.forms.pipeline.spi.ServiceCallContext;
import com.banking.forms.pipeline.spi.ServiceCallExecutor;
import com.banking.forms.serviceintegration.domain.ServiceCallLog;
import com.banking.forms.serviceintegration.domain.ServiceCallStatus;
import com.banking.forms.serviceintegration.infrastructure.ServiceCallLogRepository;
import com.banking.forms.serviceintegration.spi.ServiceRequest;
import com.banking.forms.serviceintegration.spi.ServiceResult;
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
 * Orchestrates external service adapter invocations during the pipeline SERVICE_CALL step. Fail-safe —
 * service errors never fail the pipeline run.
 */
@Service
public class ServiceIntegrationService implements ServiceCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(ServiceIntegrationService.class);
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final ServiceAdapterRouter adapterRouter;
    private final ServiceCallLogRepository callLogRepository;
    private final SubmissionEventRecorder eventRecorder;
    private final ServiceIntegrationProperties properties;
    private final ObjectMapper objectMapper;

    public ServiceIntegrationService(
            ServiceAdapterRouter adapterRouter,
            ServiceCallLogRepository callLogRepository,
            SubmissionEventRecorder eventRecorder,
            ServiceIntegrationProperties properties,
            ObjectMapper objectMapper) {
        this.adapterRouter = adapterRouter;
        this.callLogRepository = callLogRepository;
        this.eventRecorder = eventRecorder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    @Transactional
    public int invoke(ServiceCallContext context) {
        if (!properties.isEnabled()) {
            recordTimeline(context.submissionId(), "SERVICE_CALL_SKIPPED", Map.of("reason", "disabled"));
            return 0;
        }

        List<ServiceAdapterRouter.Selection> providers = adapterRouter.resolveAllEnabled();
        if (providers.isEmpty()) {
            recordTimeline(context.submissionId(), "SERVICE_CALL_SKIPPED", Map.of("reason", "no-provider"));
            return 0;
        }

        Map<String, Object> payload = buildPayload(context);
        Map<String, String> callContext = Map.of(
                "formCode", context.formCode() == null ? "" : context.formCode(),
                "riskRecommendation", context.riskRecommendation() == null ? "" : context.riskRecommendation());

        int invoked = 0;
        for (ServiceAdapterRouter.Selection selection : providers) {
            invokeOne(context, selection, payload, callContext);
            invoked++;
        }
        recordTimeline(context.submissionId(), "SERVICE_CALL_COMPLETED", Map.of("invoked", invoked));
        return invoked;
    }

    private void invokeOne(
            ServiceCallContext context,
            ServiceAdapterRouter.Selection selection,
            Map<String, Object> payload,
            Map<String, String> callContext) {
        ServiceCallLog callLog = new ServiceCallLog(
                UUID.randomUUID(),
                context.tenantId(),
                context.submissionId(),
                selection.providerCode(),
                selection.adapterType(),
                ServiceOperations.SUBMISSION_PROCESSED,
                context.formCode(),
                ServiceCallStatus.SUCCESS);

        long started = System.currentTimeMillis();
        ServiceRequest request = new ServiceRequest(
                context.tenantId(),
                context.submissionId(),
                context.formCode(),
                ServiceOperations.SUBMISSION_PROCESSED,
                payload,
                callContext);

        ServiceResult result;
        try {
            result = selection.adapter().execute(request, selection.config());
        } catch (Exception ex) {
            result = ServiceResult.failed("adapter threw: " + ex.getMessage());
        }

        long durationMs = System.currentTimeMillis() - started;
        if (result.isSuccess()) {
            callLog.completeSuccess(result.providerRef(), writeJson(result.data()), durationMs);
            recordTimeline(
                    context.submissionId(),
                    "SERVICE_CALL_SUCCEEDED",
                    Map.of("provider", selection.providerCode(), "ref", result.providerRef()));
        } else {
            callLog.completeFailed(result.detail(), durationMs);
            recordTimeline(
                    context.submissionId(),
                    "SERVICE_CALL_FAILED",
                    Map.of("provider", selection.providerCode(), "error", result.detail()));
        }
        callLogRepository.save(callLog);
    }

    @Transactional(readOnly = true)
    public List<ServiceCallLogView> listCallsForSubmission(UUID submissionId) {
        return callLogRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId).stream()
                .map(this::toView)
                .toList();
    }

    private Map<String, Object> buildPayload(ServiceCallContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("submissionId", context.submissionId().toString());
        payload.put("formCode", context.formCode());
        payload.put("sections", context.sanitizedPayload() == null ? Map.of() : context.sanitizedPayload());
        if (context.riskRecommendation() != null) {
            payload.put("riskRecommendation", context.riskRecommendation());
        }
        if (context.riskScore() != null) {
            payload.put("riskScore", context.riskScore());
        }
        return payload;
    }

    private ServiceCallLogView toView(ServiceCallLog log) {
        return new ServiceCallLogView(
                log.getId(),
                log.getSubmissionId(),
                log.getProviderCode(),
                log.getAdapterType(),
                log.getOperation(),
                log.getFormCode(),
                log.getStatus().name(),
                log.getProviderRef(),
                log.getError(),
                log.getDurationMs(),
                log.getCreatedAt());
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
            return "{}";
        }
    }
}
