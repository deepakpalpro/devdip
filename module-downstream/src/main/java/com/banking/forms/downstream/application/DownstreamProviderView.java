package com.banking.forms.downstream.application;

import com.fasterxml.jackson.databind.JsonNode;

/** Admin-facing view of a configurable downstream provider. */
public record DownstreamProviderView(
        String code,
        String name,
        String connectorType,
        boolean enabled,
        int priority,
        boolean available,
        JsonNode config) {}
