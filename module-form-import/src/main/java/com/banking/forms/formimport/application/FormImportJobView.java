package com.banking.forms.formimport.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/** Admin-facing view of an import job, including the proposed schema and confidence for review. */
public record FormImportJobView(
        UUID id,
        String status,
        String sourceType,
        String providerCode,
        String fileName,
        String source,
        String suggestedName,
        JsonNode proposedSchema,
        JsonNode confidence,
        String error,
        UUID formId,
        Instant createdAt) {}
