package com.banking.forms.downstream.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.downstream.domain.DownstreamProvider;
import com.banking.forms.downstream.infrastructure.DownstreamProviderRepository;
import com.banking.forms.downstream.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DownstreamSettingsServiceTest {

    private final DownstreamProviderRepository providerRepository = mock(DownstreamProviderRepository.class);
    private final OutboxEventRepository outboxRepository = mock(OutboxEventRepository.class);
    private final DownstreamConnectorRouter router = mock(DownstreamConnectorRouter.class);
    private final DownstreamSettingsService service =
            new DownstreamSettingsService(providerRepository, outboxRepository, router, new ObjectMapper());

    @Test
    void listProvidersMarksAvailabilityFromRouter() {
        when(providerRepository.findAllByOrderByConnectorTypeAscPriorityAsc())
                .thenReturn(List.of(provider("log-sink", "log", true, 10)));
        when(router.hasImplementation("log-sink")).thenReturn(true);

        List<DownstreamProviderView> views = service.listProviders();

        assertThat(views).hasSize(1);
        assertThat(views.get(0).available()).isTrue();
        assertThat(views.get(0).code()).isEqualTo("log-sink");
    }

    @Test
    void updateProviderThrowsForUnknownCode() {
        when(providerRepository.findByCode("missing")).thenReturn(Optional.empty());

        ObjectNode config = new ObjectMapper().createObjectNode();
        assertThatThrownBy(() -> service.updateProvider("missing", true, 10, config))
                .isInstanceOf(DownstreamException.class)
                .hasMessageContaining("missing");
    }

    private static DownstreamProvider provider(String code, String type, boolean enabled, int priority) {
        return new DownstreamProvider(UUID.randomUUID(), code, code, type, enabled, priority, null);
    }
}
