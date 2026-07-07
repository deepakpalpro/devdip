package com.banking.forms.notification.application;

import com.fasterxml.jackson.databind.JsonNode;

/** Admin-facing view of a configurable notification provider. */
public record NotificationProviderView(
        String code,
        String name,
        String channelType,
        boolean enabled,
        int priority,
        boolean available,
        JsonNode config) {}
