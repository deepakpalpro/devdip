package com.banking.forms.downstream.spi;

import java.util.UUID;

/**
 * The sanitized, ready-to-deliver payload handed to a {@link DownstreamConnector}. {@code payloadJson}
 * is the PII-scrubbed submission data plus routing metadata (form code, risk decision), serialized once
 * by the dispatch service so every connector delivers an identical, audit-safe body.
 */
public record OutboundEnvelope(
        UUID tenantId,
        UUID submissionId,
        String formCode,
        String eventType,
        String payloadJson) {}
