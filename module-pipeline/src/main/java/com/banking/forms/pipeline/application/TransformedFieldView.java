package com.banking.forms.pipeline.application;

/** One field that the PII scrubber transformed, with the strategy applied. */
public record TransformedFieldView(String fieldPath, String strategy) {}
