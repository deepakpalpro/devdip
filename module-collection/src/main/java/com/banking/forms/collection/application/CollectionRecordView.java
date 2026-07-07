package com.banking.forms.collection.application;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One sanitized submission row returned by the collection query API. */
public record CollectionRecordView(
        UUID submissionId,
        String formCode,
        String status,
        Instant submittedAt,
        Map<String, Object> fields) {}
