package com.banking.forms.notification.spi;

/**
 * Outcome of a {@link NotificationChannel#send} attempt. Channels never throw for expected delivery
 * failures — they return {@link #failed} so the dispatcher can retry or dead-letter fail-safely.
 *
 * @param outcome           SENT or FAILED
 * @param providerMessageId provider-side id (used to correlate delivery-status webhooks), may be null
 * @param detail            human-readable note / error, may be null
 */
public record DeliveryResult(DeliveryOutcome outcome, String providerMessageId, String detail) {

    public static DeliveryResult sent(String providerMessageId) {
        return new DeliveryResult(DeliveryOutcome.SENT, providerMessageId, null);
    }

    public static DeliveryResult sent(String providerMessageId, String detail) {
        return new DeliveryResult(DeliveryOutcome.SENT, providerMessageId, detail);
    }

    public static DeliveryResult failed(String detail) {
        return new DeliveryResult(DeliveryOutcome.FAILED, null, detail);
    }

    public boolean isSent() {
        return outcome == DeliveryOutcome.SENT;
    }
}
