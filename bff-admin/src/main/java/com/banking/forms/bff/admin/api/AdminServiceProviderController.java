package com.banking.forms.bff.admin.api;

import com.banking.forms.serviceintegration.application.ServiceCallLogView;
import com.banking.forms.serviceintegration.application.ServiceProviderView;
import com.banking.forms.serviceintegration.application.ServiceSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/service-providers")
public class AdminServiceProviderController {

    private final ServiceSettingsService settingsService;

    public AdminServiceProviderController(ServiceSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public List<ServiceProviderView> list() {
        return settingsService.listProviders();
    }

    @PutMapping("/{code}")
    public ServiceProviderView update(
            @PathVariable("code") String code, @Valid @RequestBody UpdateProviderRequest request) {
        return settingsService.updateProvider(code, request.enabled(), request.priority(), request.config());
    }

    @GetMapping("/calls/{submissionId}")
    public List<ServiceCallLogView> calls(@PathVariable("submissionId") UUID submissionId) {
        return settingsService.listCallsForSubmission(submissionId);
    }

    public record UpdateProviderRequest(@NotNull Boolean enabled, int priority, JsonNode config) {}
}
