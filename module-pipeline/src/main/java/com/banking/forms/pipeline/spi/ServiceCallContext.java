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
        Double riskScore) {}
