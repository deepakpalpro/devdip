package com.banking.forms.serviceintegration.application;

import com.banking.forms.serviceintegration.domain.ServiceProvider;
import com.banking.forms.serviceintegration.infrastructure.ServiceCallLogRepository;
import com.banking.forms.serviceintegration.infrastructure.ServiceProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServiceSettingsService {

    private final ServiceProviderRepository providerRepository;
    private final ServiceCallLogRepository callLogRepository;
    private final ServiceAdapterRouter adapterRouter;
    private final ObjectMapper objectMapper;

    public ServiceSettingsService(
            ServiceProviderRepository providerRepository,
            ServiceCallLogRepository callLogRepository,
            ServiceAdapterRouter adapterRouter,
            ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.callLogRepository = callLogRepository;
        this.adapterRouter = adapterRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderView> listProviders() {
        return providerRepository.findAllByOrderByAdapterTypeAscPriorityAsc().stream()
                .map(this::toView)
                .toList();
    }

    public ServiceProviderView updateProvider(String code, boolean enabled, int priority, JsonNode config) {
        ServiceProvider provider = providerRepository
                .findByCode(code)
                .orElseThrow(() -> new ServiceIntegrationException("Unknown service provider: " + code));
        provider.update(enabled, priority, writeConfig(config));
        providerRepository.save(provider);
        return toView(provider);
    }

    @Transactional(readOnly = true)
    public List<ServiceCallLogView> listCallsForSubmission(UUID submissionId) {
        return callLogRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId).stream()
                .map(log -> new ServiceCallLogView(
                        log.getId(),
                        log.getSubmissionId(),
                        log.getProviderCode(),
                        log.getAdapterType(),
                        log.getOperation(),
                        log.getFormCode(),
                        log.getStatus().name(),
                        log.getProviderRef(),
                        log.getError(),
                        log.getDurationMs(),
                        log.getCreatedAt()))
                .toList();
    }

    private ServiceProviderView toView(ServiceProvider provider) {
        return new ServiceProviderView(
                provider.getCode(),
                provider.getName(),
                provider.getAdapterType(),
                provider.isEnabled(),
                provider.getPriority(),
                adapterRouter.hasImplementation(provider.getCode()),
                readConfig(provider.getConfigJson()));
    }

    private JsonNode readConfig(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private String writeConfig(JsonNode config) {
        if (config == null || config.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new ServiceIntegrationException("Invalid provider config JSON");
        }
    }
}
