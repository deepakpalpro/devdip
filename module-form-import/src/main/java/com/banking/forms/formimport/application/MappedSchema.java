package com.banking.forms.formimport.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output of {@link SchemaMapper}: a form schema ready for {@code FormCommandService.validateSchema},
 * a parallel confidence document, and an overall 0..1 confidence for the whole proposal.
 */
public record MappedSchema(JsonNode schema, JsonNode confidence, double overallConfidence) {}
