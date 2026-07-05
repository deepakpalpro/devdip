package com.banking.forms.analytics.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** One exportable row: sanitized submission data safe for analytics (no raw PII). */
public record AnalyticsRecordView(
        UUID submissionId,
        String formCode,
        String status,
        Instant submittedAt,
        Map<String, Object> sanitizedFields) {}
