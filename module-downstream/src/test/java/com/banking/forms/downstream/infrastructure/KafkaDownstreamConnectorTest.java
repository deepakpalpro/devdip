package com.banking.forms.downstream.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KafkaDownstreamConnectorTest {

    private final KafkaDownstreamConnector connector = new KafkaDownstreamConnector();

    @Test
    void failsWhenBootstrapServersNotConfigured() {
        DispatchResult result = connector.dispatch(
                new OutboundEnvelope(UUID.randomUUID(), UUID.randomUUID(), "LOAN", "SUBMISSION_PROCESSED", "{}"),
                new ConnectorConfig(null));

        assertThat(result.isDispatched()).isFalse();
        assertThat(result.detail()).contains("bootstrapServers");
    }

    @Test
    void failsWhenBrokerUnreachable() {
        var config = new ObjectMapper().createObjectNode();
        config.put("bootstrapServers", "127.0.0.1:59999");
        config.put("topic", "submissions.processed");

        DispatchResult result = connector.dispatch(
                new OutboundEnvelope(UUID.randomUUID(), UUID.randomUUID(), "LOAN", "SUBMISSION_PROCESSED", "{\"x\":1}"),
                new ConnectorConfig(config));

        assertThat(result.isDispatched()).isFalse();
        assertThat(result.detail()).isNotBlank();
    }
}
