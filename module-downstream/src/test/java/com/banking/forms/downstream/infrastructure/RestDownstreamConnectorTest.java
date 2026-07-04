package com.banking.forms.downstream.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestDownstreamConnectorTest {

    private final RestDownstreamConnector connector = new RestDownstreamConnector();

    @Test
    void failsWhenEndpointNotConfigured() {
        DispatchResult result = connector.dispatch(
                new OutboundEnvelope(UUID.randomUUID(), UUID.randomUUID(), "LOAN", "SUBMISSION_PROCESSED", "{}"),
                new ConnectorConfig(null));

        assertThat(result.isDispatched()).isFalse();
        assertThat(result.detail()).contains("endpoint");
    }

    @Test
    void logSinkAlwaysDispatches() {
        LogDownstreamConnector logConnector = new LogDownstreamConnector();
        DispatchResult result = logConnector.dispatch(
                new OutboundEnvelope(UUID.randomUUID(), UUID.randomUUID(), "LOAN", "SUBMISSION_PROCESSED", "{\"x\":1}"),
                new ConnectorConfig(new ObjectMapper().createObjectNode()));

        assertThat(result.isDispatched()).isTrue();
        assertThat(result.providerRef()).startsWith("log-");
    }
}
