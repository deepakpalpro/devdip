package com.banking.forms.bff.admin.api;

import com.banking.forms.downstream.application.DownstreamProviderView;
import com.banking.forms.downstream.application.DownstreamSettingsService;
import com.banking.forms.downstream.application.OutboxEventView;
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

/**
 * Admin Settings API for configurable downstream providers: list the available connectors (log, REST,
 * Kafka, S3) and enable/disable, re-prioritize, or configure each. Also exposes the outbox delivery
 * log per submission.
 */
@RestController
@RequestMapping("/api/admin/v1/downstream-providers")
public class AdminDownstreamProviderController {

    private final DownstreamSettingsService settingsService;

    public AdminDownstreamProviderController(DownstreamSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public List<DownstreamProviderView> list() {
        return settingsService.listProviders();
    }

    @PutMapping("/{code}")
    public DownstreamProviderView update(
            @PathVariable("code") String code, @Valid @RequestBody UpdateProviderRequest request) {
        return settingsService.updateProvider(code, request.enabled(), request.priority(), request.config());
    }

    @GetMapping("/outbox/{submissionId}")
    public List<OutboxEventView> outbox(@PathVariable("submissionId") UUID submissionId) {
        return settingsService.listOutboxForSubmission(submissionId);
    }

    public record UpdateProviderRequest(@NotNull Boolean enabled, int priority, JsonNode config) {}
}
