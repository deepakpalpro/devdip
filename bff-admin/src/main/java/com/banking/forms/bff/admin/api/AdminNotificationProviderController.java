package com.banking.forms.bff.admin.api;

import com.banking.forms.notification.application.NotificationProviderView;
import com.banking.forms.notification.application.NotificationSettingsService;
import com.banking.forms.notification.application.NotificationTemplateView;
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
 * Admin Settings API for configurable notification providers: list the available channels
 * (email/WhatsApp adapters) and enable/disable, re-prioritize, or configure each. Also exposes the
 * read-only message templates. Drives which provider delivers each channel at runtime.
 */
@RestController
@RequestMapping("/api/admin/v1/notification-providers")
public class AdminNotificationProviderController {

    private final NotificationSettingsService settingsService;

    public AdminNotificationProviderController(NotificationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public List<NotificationProviderView> list() {
        return settingsService.listProviders();
    }

    @PutMapping("/{code}")
    public NotificationProviderView update(
            @PathVariable("code") String code, @Valid @RequestBody UpdateProviderRequest request) {
        return settingsService.updateProvider(code, request.enabled(), request.priority(), request.config());
    }

    @GetMapping("/templates")
    public List<NotificationTemplateView> templates() {
        return settingsService.listTemplates();
    }

    public record UpdateProviderRequest(@NotNull Boolean enabled, int priority, JsonNode config) {}
}
