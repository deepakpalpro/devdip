package com.banking.forms.notification.spi;

/**
 * Logical delivery channels. A logical channel (e.g. {@code email}) may be served by any of several
 * configurable providers (e.g. {@code log-email}, {@code smtp-email}), selected at runtime by
 * priority from the {@code notification_provider} registry.
 */
public final class NotificationChannels {

    public static final String EMAIL = "email";
    public static final String WHATSAPP = "whatsapp";

    private NotificationChannels() {}
}
