package com.banking.forms.submission.application;

import java.time.Instant;
import java.util.UUID;

/** A single audit timeline entry for a submission. */
public record SubmissionEventView(
        String eventType, String note, String fromStatus, String toStatus, UUID actorId, Instant createdAt) {}
