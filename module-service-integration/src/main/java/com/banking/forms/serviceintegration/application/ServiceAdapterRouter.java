package com.banking.forms.serviceintegration.application;

import com.banking.forms.serviceintegration.domain.ServiceProvider;
import com.banking.forms.serviceintegration.infrastructure.ServiceProviderRepository;
import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.banking.forms.serviceintegration.spi.ServiceAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ServiceAdapterRouter {

    private final ServiceProviderRepository providerRepository;
    private final Map<String, ServiceAdapter> adaptersByCode = new HashMap<>();
    private final ObjectMapper objectMapper;

    public ServiceAdapterRouter(
            ServiceProviderRepository providerRepository, List<ServiceAdapter> adapters, ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.objectMapper = objectMapper;
        for (ServiceAdapter adapter : adapters) {
            adaptersByCode.put(adapter.adapterId(), adapter);
        }
    }

    public record Selection(String providerCode, String adapterType, ServiceAdapter adapter, AdapterConfig config) {}

    @Transactional(readOnly = true)
    public List<Selection> resolveAllEnabled() {
        return providerRepository.findByEnabledTrueOrderByPriorityAsc().stream()
                .filter(provider -> adaptersByCode.containsKey(provider.getCode()))
                .map(this::toSelection)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Selection> resolveProvider(String providerCode) {
        ServiceAdapter adapter = adaptersByCode.get(providerCode);
        if (adapter == null) {
            return Optional.empty();
        }
        return providerRepository.findByCode(providerCode).map(this::toSelection);
    }

    public boolean hasImplementation(String providerCode) {
        return adaptersByCode.containsKey(providerCode);
    }

    private Selection toSelection(ServiceProvider provider) {
        return new Selection(
                provider.getCode(),
                provider.getAdapterType(),
                adaptersByCode.get(provider.getCode()),
                parseConfig(provider.getConfigJson()));
    }

    private AdapterConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return new AdapterConfig(null);
        }
        try {
            return new AdapterConfig(objectMapper.readTree(json));
        } catch (Exception ex) {
            return new AdapterConfig(null);
        }
    }
}
