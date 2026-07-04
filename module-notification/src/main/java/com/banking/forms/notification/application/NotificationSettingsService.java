package com.banking.forms.notification.application;

import com.banking.forms.notification.domain.NotificationProvider;
import com.banking.forms.notification.infrastructure.NotificationProviderRepository;
import com.banking.forms.notification.infrastructure.NotificationTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read/update access to the configurable notification providers and templates surfaced in the admin
 * Settings UI. Which adapter serves a channel, whether it's enabled, its priority, and its non-secret
 * config are all data-driven here, mirroring the form-import provider settings.
 */
@Service
@Transactional
public class NotificationSettingsService {

    private final NotificationProviderRepository providerRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationChannelRouter channelRouter;
    private final ObjectMapper objectMapper;

    public NotificationSettingsService(
            NotificationProviderRepository providerRepository,
            NotificationTemplateRepository templateRepository,
            NotificationChannelRouter channelRouter,
            ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.templateRepository = templateRepository;
        this.channelRouter = channelRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationProviderView> listProviders() {
        return providerRepository.findAllByOrderByChannelTypeAscPriorityAsc().stream()
                .map(this::toView)
                .toList();
    }

    public NotificationProviderView updateProvider(String code, boolean enabled, int priority, JsonNode config) {
        NotificationProvider provider = providerRepository
                .findByCode(code)
                .orElseThrow(() -> new NotificationException("Unknown notification provider: " + code));
        provider.update(enabled, priority, writeConfig(config));
        providerRepository.save(provider);
        return toView(provider);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateView> listTemplates() {
        return templateRepository.findAllByOrderByEventTypeAscChannelTypeAscLocaleAsc().stream()
                .map(t -> new NotificationTemplateView(
                        t.getEventType(), t.getChannelType(), t.getLocale(), t.getSubject(), t.getBody()))
                .toList();
    }

    private NotificationProviderView toView(NotificationProvider provider) {
        return new NotificationProviderView(
                provider.getCode(),
                provider.getName(),
                provider.getChannelType(),
                provider.isEnabled(),
                provider.getPriority(),
                channelRouter.hasImplementation(provider.getCode()),
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
            throw new NotificationException("Invalid provider config JSON");
        }
    }
}
