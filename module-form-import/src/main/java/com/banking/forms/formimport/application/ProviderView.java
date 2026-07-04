package com.banking.forms.formimport.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Admin-facing view of a configurable import provider. {@code available} reflects whether a matching
 * {@link com.banking.forms.formimport.spi.FormExtractor} implementation is present on the classpath.
 */
public record ProviderView(
        String code,
        String name,
        String sourceType,
        boolean enabled,
        int priority,
        boolean available,
        JsonNode config) {}
