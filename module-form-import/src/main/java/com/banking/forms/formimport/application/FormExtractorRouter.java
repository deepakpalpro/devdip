package com.banking.forms.formimport.application;

import com.banking.forms.formimport.domain.FormImportProvider;
import com.banking.forms.formimport.infrastructure.FormImportProviderRepository;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves which configured provider extracts a given source type. Selection is data-driven: the
 * {@code form_import_provider} table (managed in the admin Settings UI) decides which enabled
 * provider — by ascending priority — handles each source type. The matching {@link FormExtractor}
 * bean (in-JVM or an external adapter from {@code module-service-integration}) is then invoked with
 * its stored {@link ProviderConfig}.
 */
@Component
public class FormExtractorRouter {

    private final FormImportProviderRepository providerRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, FormExtractor> extractorsByCode;

    public FormExtractorRouter(
            FormImportProviderRepository providerRepository,
            ObjectMapper objectMapper,
            List<FormExtractor> extractors) {
        this.providerRepository = providerRepository;
        this.objectMapper = objectMapper;
        this.extractorsByCode =
                extractors.stream().collect(Collectors.toMap(FormExtractor::code, Function.identity(), (a, b) -> a));
    }

    /** Picks the highest-priority enabled provider with an available implementation for the type. */
    public ResolvedProvider resolve(String sourceType) {
        List<FormImportProvider> providers =
                providerRepository.findBySourceTypeAndEnabledTrueOrderByPriorityAsc(sourceType);
        if (providers.isEmpty()) {
            throw new FormImportException("No enabled import provider is configured for source type: " + sourceType);
        }
        for (FormImportProvider provider : providers) {
            FormExtractor extractor = extractorsByCode.get(provider.getCode());
            if (extractor != null) {
                return new ResolvedProvider(provider.getCode(), extractor, parseConfig(provider.getConfigJson()));
            }
        }
        throw new FormImportException(
                "Enabled provider(s) for " + sourceType + " have no matching implementation on the classpath");
    }

    /** True if a provider bean exists for the code (used by the settings view to flag availability). */
    public boolean hasImplementation(String code) {
        return extractorsByCode.containsKey(code);
    }

    private ProviderConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return ProviderConfig.empty();
        }
        try {
            return new ProviderConfig(objectMapper.readTree(json));
        } catch (Exception ex) {
            return ProviderConfig.empty();
        }
    }

    public record ResolvedProvider(String code, FormExtractor extractor, ProviderConfig config) {}
}
