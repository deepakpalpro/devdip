package com.banking.forms.formdefinition.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Admin-facing view of a single form version. Carries the <em>raw</em> (uncomposed) schema so the
 * form builder edits the source definition rather than the embedded-form composed output.
 */
public record FormVersionView(
        UUID id, int versionNumber, String status, Instant publishedAt, JsonNode schema) {}
