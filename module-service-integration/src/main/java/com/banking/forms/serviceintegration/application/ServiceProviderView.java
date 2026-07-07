package com.banking.forms.serviceintegration.application;

import com.fasterxml.jackson.databind.JsonNode;

public record ServiceProviderView(
        String code,
        String name,
        String adapterType,
        boolean enabled,
        int priority,
        boolean available,
        JsonNode config) {}
