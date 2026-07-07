package com.banking.forms.notification.spi;

import java.util.Map;
import java.util.UUID;

/**
 * A fully-rendered message handed to a {@link NotificationChannel} for delivery. The recipient is the
 * channel-appropriate address (an email address for {@code email}, an E.164 phone number for
 * {@code whatsapp}). {@code templateName} carries the provider-registered template id for channels
 * that require pre-approved templates (WhatsApp); {@code body}/{@code subject} carry the freeform
 * rendered text used by email and as WhatsApp fallback/body variables.
 */
public record OutboundNotification(
        UUID tenantId,
        UUID submissionId,
        String eventType,
        String channelType,
        String recipient,
        String subject,
        String body,
        String templateName,
        String locale,
        Map<String, String> variables) {}
