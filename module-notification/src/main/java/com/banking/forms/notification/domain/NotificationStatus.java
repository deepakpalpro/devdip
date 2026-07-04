package com.banking.forms.notification.domain;

/**
 * Lifecycle of a queued notification message.
 *
 * <ul>
 *   <li>{@code PENDING} — enqueued in the outbox, awaiting dispatch.
 *   <li>{@code SENT} — accepted by the provider (awaiting delivery confirmation, if any).
 *   <li>{@code DELIVERED} — provider confirmed delivery via webhook.
 *   <li>{@code FAILED} — exhausted retries (dead-lettered).
 *   <li>{@code SKIPPED} — intentionally not sent (no recipient, no consent, no provider).
 * </ul>
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    SKIPPED
}
