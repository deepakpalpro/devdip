package com.banking.forms.downstream.spi;

/**
 * Service Provider Interface for a downstream delivery adapter (a log sink, a REST webhook, a Kafka
 * topic, an S3 archive, …). Implementations are Spring beans discovered by their {@link #connectorId()},
 * which matches a {@code downstream_provider} row's {@code code}. Whether a connector is used at all —
 * and its priority within its logical type — is data-driven from that registry, mirroring the
 * notification-channel and form-import extractor SPIs.
 *
 * <p>Contract: {@link #dispatch} must be fail-safe — return {@link DispatchResult#failed} for expected
 * delivery errors rather than throwing, so the dispatcher can retry or dead-letter.
 */
public interface DownstreamConnector {

    /** Provider code this adapter implements (e.g. {@code log-sink}, {@code rest-webhook}). */
    String connectorId();

    /** Logical connector type served, e.g. {@link ConnectorTypes#REST}. */
    String connectorType();

    DispatchResult dispatch(OutboundEnvelope envelope, ConnectorConfig config);
}
