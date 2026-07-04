package com.banking.forms.pipeline.spi;

import java.util.Map;
import java.util.UUID;

/**
 * Input handed to an {@link AiEvaluator}. Contains only the <b>PII-sanitized</b> section data
 * (PII scrubbing always runs before the AI step), keyed by section then field.
 *
 * @param submissionId  the submission being evaluated
 * @param formCode      the published form's code (lets an evaluator pick a per-form prompt/policy)
 * @param sanitizedData section → (field → value) sanitized copy safe to send to an evaluator
 * @param metadata      free-form context (e.g. tenant, form version) for prompts/telemetry
 */
public record AiEvaluationContext(
        UUID submissionId,
        String formCode,
        Map<String, Map<String, Object>> sanitizedData,
        Map<String, String> metadata) {}
