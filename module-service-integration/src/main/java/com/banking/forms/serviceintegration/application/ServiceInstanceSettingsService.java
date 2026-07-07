package com.banking.forms.serviceintegration.application;

import com.banking.forms.serviceintegration.domain.ServiceBinding;
import com.banking.forms.serviceintegration.domain.ServiceBindingScope;
import com.banking.forms.serviceintegration.domain.ServiceInstance;
import com.banking.forms.serviceintegration.domain.ServiceProvider;
import com.banking.forms.serviceintegration.infrastructure.ServiceBindingRepository;
import com.banking.forms.serviceintegration.infrastructure.ServiceInstanceRepository;
import com.banking.forms.serviceintegration.infrastructure.ServiceProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServiceInstanceSettingsService {

    private final ServiceInstanceRepository instanceRepository;
    private final ServiceBindingRepository bindingRepository;
    private final ServiceProviderRepository providerRepository;
    private final ObjectMapper objectMapper;

    public ServiceInstanceSettingsService(
            ServiceInstanceRepository instanceRepository,
            ServiceBindingRepository bindingRepository,
            ServiceProviderRepository providerRepository,
            ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.bindingRepository = bindingRepository;
        this.providerRepository = providerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ServiceInstanceView> listInstances(UUID tenantId) {
        return instanceRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(this::toInstanceView)
                .toList();
    }

    public ServiceInstanceView createInstance(UUID tenantId, CreateInstanceRequest request) {
        if (instanceRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new ServiceIntegrationException("Service instance code already exists: " + request.code());
        }
        ServiceProvider provider = providerRepository
                .findByCode(request.providerCode())
                .orElseThrow(() -> new ServiceIntegrationException("Unknown provider: " + request.providerCode()));
        ServiceInstance instance = instanceRepository.save(new ServiceInstance(
                UUID.randomUUID(),
                tenantId,
                provider.getId(),
                request.code(),
                request.name(),
                writeConfig(request.config())));
        return toInstanceView(instance);
    }

    public ServiceInstanceView updateInstance(UUID tenantId, String code, UpdateInstanceRequest request) {
        ServiceInstance instance = loadOwned(tenantId, code);
        instance.update(request.name(), writeConfig(request.config()), request.enabled());
        return toInstanceView(instanceRepository.save(instance));
    }

    @Transactional(readOnly = true)
    public List<ServiceBindingView> listBindings(UUID tenantId, UUID formVersionId) {
        return bindingRepository.findByTenantIdAndFormVersionIdOrderByCreatedAtAsc(tenantId, formVersionId).stream()
                .map(this::toBindingView)
                .toList();
    }

    public ServiceBindingView upsertBinding(UUID tenantId, UpsertBindingRequest request) {
        ServiceInstance instance = loadOwned(tenantId, request.instanceCode());
        validateScopeRefs(request);
        ServiceBinding binding = new ServiceBinding(
                UUID.randomUUID(),
                tenantId,
                instance.getId(),
                request.scope(),
                request.formVersionId(),
                request.pipelineDefinitionId(),
                request.pipelineStepId());
        binding.setEnabled(request.enabled());
        return toBindingView(bindingRepository.save(binding));
    }

    private void validateScopeRefs(UpsertBindingRequest request) {
        switch (request.scope()) {
            case FORM -> {
                if (request.formVersionId() == null) {
                    throw new ServiceIntegrationException("formVersionId required for FORM scope");
                }
            }
            case PIPELINE -> {
                if (request.pipelineDefinitionId() == null) {
                    throw new ServiceIntegrationException("pipelineDefinitionId required for PIPELINE scope");
                }
            }
            case PIPELET -> {
                if (request.pipelineStepId() == null) {
                    throw new ServiceIntegrationException("pipelineStepId required for PIPELET scope");
                }
            }
        }
    }

    private ServiceInstance loadOwned(UUID tenantId, String code) {
        return instanceRepository
                .findByTenantIdAndCode(tenantId, code)
                .filter(instance -> instance.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ServiceIntegrationException("Unknown service instance: " + code));
    }

    private ServiceInstanceView toInstanceView(ServiceInstance instance) {
        String providerCode = providerRepository
                .findById(instance.getServiceProviderId())
                .map(ServiceProvider::getCode)
                .orElse("unknown");
        return new ServiceInstanceView(
                instance.getId(),
                instance.getCode(),
                instance.getName(),
                providerCode,
                instance.isEnabled(),
                readConfig(instance.getConfigJson()),
                instance.getCreatedAt());
    }

    private ServiceBindingView toBindingView(ServiceBinding binding) {
        String instanceCode = instanceRepository
                .findById(binding.getServiceInstanceId())
                .map(ServiceInstance::getCode)
                .orElse("unknown");
        return new ServiceBindingView(
                binding.getId(),
                instanceCode,
                binding.getScope(),
                binding.getFormVersionId(),
                binding.getPipelineDefinitionId(),
                binding.getPipelineStepId(),
                binding.isEnabled());
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
            throw new ServiceIntegrationException("Invalid instance config JSON");
        }
    }

    public record ServiceInstanceView(
            UUID id,
            String code,
            String name,
            String providerCode,
            boolean enabled,
            JsonNode config,
            java.time.Instant createdAt) {}

    public record ServiceBindingView(
            UUID id,
            String instanceCode,
            ServiceBindingScope scope,
            UUID formVersionId,
            UUID pipelineDefinitionId,
            UUID pipelineStepId,
            boolean enabled) {}

    public record CreateInstanceRequest(String code, String name, String providerCode, JsonNode config) {}

    public record UpdateInstanceRequest(String name, JsonNode config, boolean enabled) {}

    public record UpsertBindingRequest(
            String instanceCode,
            ServiceBindingScope scope,
            UUID formVersionId,
            UUID pipelineDefinitionId,
            UUID pipelineStepId,
            boolean enabled) {}
}
