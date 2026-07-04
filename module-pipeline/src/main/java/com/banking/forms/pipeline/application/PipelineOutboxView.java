package com.banking.forms.pipeline.application;

import java.time.Instant;
import java.util.UUID;

/** Admin-facing view of a pipeline outbox row. */
public record PipelineOutboxView(
        UUID id,
        UUID submissionId,
        String eventType,
        boolean published,
        int attempts,
        String error,
        Instant occurredAt,
        Instant updatedAt) {}
