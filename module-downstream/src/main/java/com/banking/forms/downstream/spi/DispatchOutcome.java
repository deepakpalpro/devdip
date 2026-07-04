package com.banking.forms.downstream.spi;

/** Result of a single delivery attempt by a {@link DownstreamConnector}. */
public enum DispatchOutcome {
    DISPATCHED,
    FAILED
}
