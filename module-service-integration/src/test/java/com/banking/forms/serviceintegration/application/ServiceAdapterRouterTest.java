package com.banking.forms.serviceintegration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.serviceintegration.domain.ServiceProvider;
import com.banking.forms.serviceintegration.infrastructure.ServiceProviderRepository;
import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.banking.forms.serviceintegration.spi.ServiceAdapter;
import com.banking.forms.serviceintegration.spi.ServiceRequest;
import com.banking.forms.serviceintegration.spi.ServiceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceAdapterRouterTest {

    private final ServiceProviderRepository repository = mock(ServiceProviderRepository.class);
    private final ServiceAdapterRouter router = new ServiceAdapterRouter(
            repository,
            List.of(adapter("log-service", "log"), adapter("rest-api", "rest")),
            new ObjectMapper());

    @Test
    void resolveAllEnabledSkipsProvidersWithoutImplementation() {
        when(repository.findByEnabledTrueOrderByPriorityAsc())
                .thenReturn(List.of(
                        provider("credit-bureau", "credit", 5),
                        provider("log-service", "log", 10)));

        assertThat(router.resolveAllEnabled()).hasSize(1).extracting(ServiceAdapterRouter.Selection::providerCode)
                .containsExactly("log-service");
    }

    private static ServiceProvider provider(String code, String type, int priority) {
        return new ServiceProvider(UUID.randomUUID(), code, code, type, true, priority, null);
    }

    private static ServiceAdapter adapter(String id, String type) {
        return new ServiceAdapter() {
            @Override
            public String adapterId() {
                return id;
            }

            @Override
            public String adapterType() {
                return type;
            }

            @Override
            public ServiceResult execute(ServiceRequest request, AdapterConfig config) {
                return ServiceResult.success("ok", java.util.Map.of());
            }
        };
    }
}
