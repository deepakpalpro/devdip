package com.banking.forms.serviceintegration.application;

import java.time.Instant;
import java.util.UUID;

public record ServiceCallLogView(
        UUID id,
        UUID submissionId,
        String providerCode,
        String adapterType,
        String operation,
        String formCode,
        String status,
        String providerRef,
        String error,
        Long durationMs,
        Instant createdAt) {}
