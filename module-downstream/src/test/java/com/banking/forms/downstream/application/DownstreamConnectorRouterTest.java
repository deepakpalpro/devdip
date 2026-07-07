package com.banking.forms.downstream.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.downstream.domain.DownstreamProvider;
import com.banking.forms.downstream.infrastructure.DownstreamProviderRepository;
import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.DownstreamConnector;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DownstreamConnectorRouterTest {

    private final DownstreamProviderRepository repository = mock(DownstreamProviderRepository.class);
    private final DownstreamConnectorRouter router = new DownstreamConnectorRouter(
            repository,
            List.of(connector("log-sink", "log"), connector("rest-webhook", "rest")),
            new ObjectMapper());

    @Test
    void resolveAllEnabledSkipsProvidersWithoutImplementation() {
        when(repository.findByEnabledTrueOrderByPriorityAsc())
                .thenReturn(List.of(
                        provider("kafka-stream", "kafka", 5, null), // no bean
                        provider("log-sink", "log", 10, null)));

        List<DownstreamConnectorRouter.Selection> selections = router.resolveAllEnabled();

        assertThat(selections).hasSize(1);
        assertThat(selections.get(0).providerCode()).isEqualTo("log-sink");
    }

    @Test
    void resolveProviderParsesConfigJson() {
        when(repository.findByCode("rest-webhook"))
                .thenReturn(Optional.of(provider("rest-webhook", "rest", 20, "{\"endpoint\":\"https://x.com\"}")));

        Optional<DownstreamConnectorRouter.Selection> selection = router.resolveProvider("rest-webhook");

        assertThat(selection).isPresent();
        assertThat(selection.get().config().text("endpoint", null)).isEqualTo("https://x.com");
    }

    @Test
    void resolveProviderEmptyWhenNoImplementationBean() {
        assertThat(router.resolveProvider("unknown")).isEmpty();
        assertThat(router.hasImplementation("log-sink")).isTrue();
        assertThat(router.hasImplementation("kafka-stream")).isFalse();
    }

    private static DownstreamProvider provider(String code, String connectorType, int priority, String config) {
        return new DownstreamProvider(UUID.randomUUID(), code, code, connectorType, true, priority, config);
    }

    private static DownstreamConnector connector(String id, String type) {
        return new DownstreamConnector() {
            @Override
            public String connectorId() {
                return id;
            }

            @Override
            public String connectorType() {
                return type;
            }

            @Override
            public DispatchResult dispatch(OutboundEnvelope envelope, ConnectorConfig config) {
                return DispatchResult.dispatched("test");
            }
        };
    }
}
