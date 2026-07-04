package com.banking.forms.bff.admin.api;

import java.util.UUID;

/** Dev-mode actor resolution for admin actions (no auth wired yet). */
public final class AdminRequestContext {

    public static final UUID DEFAULT_ADMIN_USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private AdminRequestContext() {}

    public static UUID resolveActorId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return DEFAULT_ADMIN_USER_ID;
        }
        return UUID.fromString(headerValue);
    }
}
