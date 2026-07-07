package com.banking.forms.downstream.domain;

/**
 * Lifecycle of an outbox event.
 *
 * <ul>
 *   <li>{@code PENDING} — enqueued in the outbox (in the pipeline transaction), awaiting dispatch.
 *   <li>{@code DISPATCHED} — accepted by the downstream connector.
 *   <li>{@code FAILED} — exhausted retries (dead-lettered).
 *   <li>{@code SKIPPED} — intentionally not delivered (no enabled provider with an implementation).
 * </ul>
 */
public enum OutboxStatus {
    PENDING,
    DISPATCHED,
    FAILED,
    SKIPPED
}
