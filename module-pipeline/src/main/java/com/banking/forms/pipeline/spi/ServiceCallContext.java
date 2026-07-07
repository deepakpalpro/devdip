package com.banking.forms.pipeline.spi;

import java.util.Map;
import java.util.UUID;

/** Context handed to {@link ServiceCallExecutor} during the pipeline SERVICE_CALL step. */
public record ServiceCallContext(
        UUID tenantId,
        UUID submissionId,
        String formCode,
        Map<String, Map<String, Object>> sanitizedPayload,
        String riskRecommendation,
        Double riskScore,
        UUID formVersionId,
        UUID pipelineDefinitionId,
        UUID pipelineStepId) {

    /** Backward-compatible constructor without scoped resolution fields. */
    public ServiceCallContext(
            UUID tenantId,
            UUID submissionId,
            String formCode,
            Map<String, Map<String, Object>> sanitizedPayload,
            String riskRecommendation,
            Double riskScore) {
        this(tenantId, submissionId, formCode, sanitizedPayload, riskRecommendation, riskScore, null, null, null);
    }
}
