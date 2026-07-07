package com.banking.forms.bff.admin.api;

import com.banking.forms.serviceintegration.application.ServiceInstanceSettingsService;
import com.banking.forms.serviceintegration.application.ServiceInstanceSettingsService.CreateInstanceRequest;
import com.banking.forms.serviceintegration.application.ServiceInstanceSettingsService.ServiceBindingView;
import com.banking.forms.serviceintegration.application.ServiceInstanceSettingsService.ServiceInstanceView;
import com.banking.forms.serviceintegration.application.ServiceInstanceSettingsService.UpdateInstanceRequest;
import com.banking.forms.serviceintegration.application.ServiceInstanceSettingsService.UpsertBindingRequest;
import com.banking.forms.serviceintegration.domain.ServiceBindingScope;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1")
public class AdminServiceInstanceController {

    private final ServiceInstanceSettingsService settingsService;

    public AdminServiceInstanceController(ServiceInstanceSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/service-instances")
    public List<ServiceInstanceView> listInstances(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return settingsService.listInstances(tenantId);
    }

    @PostMapping("/service-instances")
    public ResponseEntity<ServiceInstanceView> createInstance(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @Valid @RequestBody CreateInstanceApiRequest request) {
        ServiceInstanceView created = settingsService.createInstance(
                tenantId,
                new CreateInstanceRequest(request.code(), request.name(), request.providerCode(), request.config()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/service-instances/{code}")
    public ServiceInstanceView updateInstance(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("code") String code,
            @Valid @RequestBody UpdateInstanceApiRequest request) {
        return settingsService.updateInstance(
                tenantId, code, new UpdateInstanceRequest(request.name(), request.config(), request.enabled()));
    }

    @GetMapping("/forms/{formId}/versions/{versionId}/service-bindings")
    public List<ServiceBindingView> listBindings(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("versionId") UUID versionId) {
        return settingsService.listBindings(tenantId, versionId);
    }

    @PutMapping("/forms/{formId}/versions/{versionId}/service-bindings")
    public ServiceBindingView upsertBinding(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody UpsertBindingApiRequest request) {
        return settingsService.upsertBinding(
                tenantId,
                new UpsertBindingRequest(
                        request.instanceCode(),
                        request.scope(),
                        versionId,
                        request.pipelineDefinitionId(),
                        request.pipelineStepId(),
                        request.enabled()));
    }

    public record CreateInstanceApiRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String providerCode,
            JsonNode config) {}

    public record UpdateInstanceApiRequest(@NotBlank String name, JsonNode config, boolean enabled) {}

    public record UpsertBindingApiRequest(
            @NotBlank String instanceCode,
            @NotNull ServiceBindingScope scope,
            UUID pipelineDefinitionId,
            UUID pipelineStepId,
            boolean enabled) {}
}
