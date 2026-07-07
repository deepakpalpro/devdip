package com.banking.forms.downstream.application;

import java.time.Instant;
import java.util.UUID;

/** Admin-facing view of an outbox event (delivery log). */
public record OutboxEventView(
        UUID id,
        UUID submissionId,
        String eventType,
        String formCode,
        String providerCode,
        String connectorType,
        String status,
        int attempts,
        String providerRef,
        String error,
        Instant createdAt,
        Instant updatedAt) {}
