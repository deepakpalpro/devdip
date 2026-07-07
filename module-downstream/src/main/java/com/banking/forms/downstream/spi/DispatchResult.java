package com.banking.forms.downstream.spi;

/**
 * Outcome of a {@link DownstreamConnector#dispatch} attempt. Connectors never throw for expected
 * delivery failures — they return {@link #failed} so the dispatcher can retry or dead-letter fail-safely.
 *
 * @param outcome     DISPATCHED or FAILED
 * @param providerRef provider-side reference (HTTP status, message key, object key), may be null
 * @param detail      human-readable note / error, may be null
 */
public record DispatchResult(DispatchOutcome outcome, String providerRef, String detail) {

    public static DispatchResult dispatched(String providerRef) {
        return new DispatchResult(DispatchOutcome.DISPATCHED, providerRef, null);
    }

    public static DispatchResult dispatched(String providerRef, String detail) {
        return new DispatchResult(DispatchOutcome.DISPATCHED, providerRef, detail);
    }

    public static DispatchResult failed(String detail) {
        return new DispatchResult(DispatchOutcome.FAILED, null, detail);
    }

    public boolean isDispatched() {
        return outcome == DispatchOutcome.DISPATCHED;
    }
}
