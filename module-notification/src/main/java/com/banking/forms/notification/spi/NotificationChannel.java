package com.banking.forms.notification.spi;

/**
 * Service Provider Interface for a message-delivery adapter (SMTP email, WhatsApp Cloud API, a log
 * sink, …). Implementations are Spring beans discovered by their {@link #channelId()}, which matches
 * a {@code notification_provider} row's {@code code}. Whether a channel is used at all — and its
 * priority within its logical channel — is data-driven from that registry, mirroring the form-import
 * extractor SPI.
 *
 * <p>Contract: {@link #send} must be fail-safe — return {@link DeliveryResult#failed} for expected
 * delivery errors rather than throwing, so the dispatcher can retry or dead-letter.
 */
public interface NotificationChannel {

    /** Provider code this adapter implements (e.g. {@code smtp-email}, {@code whatsapp-cloud}). */
    String channelId();

    /** Logical channel served, e.g. {@link NotificationChannels#EMAIL}. */
    String channelType();

    DeliveryResult send(OutboundNotification notification, ChannelConfig config);
}
