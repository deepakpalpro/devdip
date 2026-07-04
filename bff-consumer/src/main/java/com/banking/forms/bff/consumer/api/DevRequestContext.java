package com.banking.forms.bff.consumer.api;

import java.util.UUID;

public final class DevRequestContext {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private DevRequestContext() {}

    public static UUID resolveUserId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return DEFAULT_USER_ID;
        }
        return UUID.fromString(headerValue);
    }
}
