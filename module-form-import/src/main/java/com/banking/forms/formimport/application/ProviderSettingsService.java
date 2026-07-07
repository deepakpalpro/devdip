package com.banking.forms.formimport.application;

import com.banking.forms.formimport.domain.FormImportProvider;
import com.banking.forms.formimport.infrastructure.FormImportProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read/update access to the configurable import providers surfaced in the admin Settings UI. Which
 * engine handles a source type, whether it's enabled, its priority, and its (non-secret) config are
 * all data-driven here rather than hard-coded.
 */
@Service
@Transactional
public class ProviderSettingsService {

    private final FormImportProviderRepository providerRepository;
    private final FormExtractorRouter extractorRouter;
    private final ObjectMapper objectMapper;

    public ProviderSettingsService(
            FormImportProviderRepository providerRepository,
            FormExtractorRouter extractorRouter,
            ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.extractorRouter = extractorRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProviderView> list() {
        return providerRepository.findAllByOrderBySourceTypeAscPriorityAsc().stream()
                .map(this::toView)
                .toList();
    }

    public ProviderView update(String code, boolean enabled, int priority, JsonNode config) {
        FormImportProvider provider = providerRepository
                .findByCode(code)
                .orElseThrow(() -> new FormImportException("Unknown import provider: " + code));
        String configJson = writeConfig(config);
        provider.update(enabled, priority, configJson);
        providerRepository.save(provider);
        return toView(provider);
    }

    private ProviderView toView(FormImportProvider provider) {
        return new ProviderView(
                provider.getCode(),
                provider.getName(),
                provider.getSourceType(),
                provider.isEnabled(),
                provider.getPriority(),
                extractorRouter.hasImplementation(provider.getCode()),
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
            throw new FormImportException("Invalid provider config JSON");
        }
    }
}
