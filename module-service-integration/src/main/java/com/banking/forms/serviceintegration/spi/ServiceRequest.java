package com.banking.forms.serviceintegration.spi;

import java.util.Map;
import java.util.UUID;

/** Request handed to a {@link ServiceAdapter} for execution against an external system. */
public record ServiceRequest(
        UUID tenantId,
        UUID submissionId,
        String formCode,
        String operation,
        Map<String, Object> payload,
        Map<String, String> context) {}
