package com.banking.forms.serviceintegration.application;

import com.banking.forms.serviceintegration.domain.ServiceBinding;
import com.banking.forms.serviceintegration.domain.ServiceBindingScope;
import com.banking.forms.serviceintegration.domain.ServiceInstance;
import com.banking.forms.serviceintegration.domain.ServiceProvider;
import com.banking.forms.serviceintegration.infrastructure.ServiceBindingRepository;
import com.banking.forms.serviceintegration.infrastructure.ServiceInstanceRepository;
import com.banking.forms.serviceintegration.infrastructure.ServiceProviderRepository;
import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves which service adapters to invoke for a pipeline step. Precedence: PIPELET → PIPELINE →
 * FORM → global enabled providers.
 */
@Component
public class ServiceInstanceResolver {

    private final ServiceBindingRepository bindingRepository;
    private final ServiceInstanceRepository instanceRepository;
    private final ServiceProviderRepository providerRepository;
    private final ServiceAdapterRouter adapterRouter;
    private final ObjectMapper objectMapper;

    public ServiceInstanceResolver(
            ServiceBindingRepository bindingRepository,
            ServiceInstanceRepository instanceRepository,
            ServiceProviderRepository providerRepository,
            ServiceAdapterRouter adapterRouter,
            ObjectMapper objectMapper) {
        this.bindingRepository = bindingRepository;
        this.instanceRepository = instanceRepository;
        this.providerRepository = providerRepository;
        this.adapterRouter = adapterRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ServiceAdapterRouter.Selection> resolve(
            UUID tenantId, UUID formVersionId, UUID pipelineDefinitionId, UUID pipelineStepId) {
        List<ServiceBinding> bindings = new ArrayList<>();
        if (pipelineStepId != null) {
            bindings.addAll(bindingRepository.findByTenantIdAndPipelineStepIdAndScopeAndEnabledTrue(
                    tenantId, pipelineStepId, ServiceBindingScope.PIPELET));
        }
        if (bindings.isEmpty() && pipelineDefinitionId != null) {
            bindings.addAll(bindingRepository.findByTenantIdAndPipelineDefinitionIdAndScopeAndEnabledTrue(
                    tenantId, pipelineDefinitionId, ServiceBindingScope.PIPELINE));
        }
        if (bindings.isEmpty() && formVersionId != null) {
            bindings.addAll(bindingRepository.findByTenantIdAndFormVersionIdAndScopeAndEnabledTrue(
                    tenantId, formVersionId, ServiceBindingScope.FORM));
        }
        if (bindings.isEmpty()) {
            return adapterRouter.resolveAllEnabled();
        }
        return resolveInstances(bindings);
    }

    private List<ServiceAdapterRouter.Selection> resolveInstances(List<ServiceBinding> bindings) {
        Set<UUID> instanceIds = new LinkedHashSet<>();
        for (ServiceBinding binding : bindings) {
            instanceIds.add(binding.getServiceInstanceId());
        }
        List<ServiceInstance> instances = instanceRepository.findByIdInAndEnabledTrue(List.copyOf(instanceIds));
        List<ServiceAdapterRouter.Selection> selections = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            toSelection(instance).ifPresent(selections::add);
        }
        return selections;
    }

    private Optional<ServiceAdapterRouter.Selection> toSelection(ServiceInstance instance) {
        Optional<ServiceProvider> providerOpt = providerRepository.findById(instance.getServiceProviderId());
        if (providerOpt.isEmpty()) {
            return Optional.empty();
        }
        ServiceProvider provider = providerOpt.get();
        if (!adapterRouter.hasImplementation(provider.getCode())) {
            return Optional.empty();
        }
        return adapterRouter.resolveProvider(provider.getCode()).map(base -> new ServiceAdapterRouter.Selection(
                instance.getCode(),
                base.adapterType(),
                base.adapter(),
                mergeConfig(provider.getConfigJson(), instance.getConfigJson())));
    }

    private AdapterConfig mergeConfig(String providerJson, String instanceJson) {
        try {
            JsonNode providerNode = providerJson == null || providerJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(providerJson);
            JsonNode instanceNode = instanceJson == null || instanceJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(instanceJson);
            ObjectNode merged = providerNode.deepCopy();
            if (instanceNode.isObject()) {
                instanceNode.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
            }
            return new AdapterConfig(merged);
        } catch (Exception ex) {
            return new AdapterConfig(null);
        }
    }
}
