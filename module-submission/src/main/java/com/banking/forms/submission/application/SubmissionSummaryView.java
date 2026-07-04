package com.banking.forms.submission.application;

import java.time.Instant;
import java.util.UUID;

/** Lightweight submission row for admin listing (no section data). */
public record SubmissionSummaryView(
        UUID id,
        String formCode,
        String formName,
        String status,
        Instant createdAt,
        Instant submittedAt) {}
