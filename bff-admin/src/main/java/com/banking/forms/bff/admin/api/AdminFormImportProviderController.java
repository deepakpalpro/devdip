package com.banking.forms.bff.admin.api;

import com.banking.forms.formimport.application.ProviderSettingsService;
import com.banking.forms.formimport.application.ProviderView;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Settings API for configurable import providers: list the available engines and enable/disable,
 * re-prioritize, or configure each (endpoint/model/secretRef). Drives which provider extracts each
 * source type at runtime.
 */
@RestController
@RequestMapping("/api/admin/v1/form-import-providers")
public class AdminFormImportProviderController {

    private final ProviderSettingsService providerSettingsService;

    public AdminFormImportProviderController(ProviderSettingsService providerSettingsService) {
        this.providerSettingsService = providerSettingsService;
    }

    @GetMapping
    public List<ProviderView> list() {
        return providerSettingsService.list();
    }

    @PutMapping("/{code}")
    public ProviderView update(@PathVariable("code") String code, @Valid @RequestBody UpdateProviderRequest request) {
        return providerSettingsService.update(code, request.enabled(), request.priority(), request.config());
    }

    public record UpdateProviderRequest(@NotNull Boolean enabled, int priority, JsonNode config) {}
}
