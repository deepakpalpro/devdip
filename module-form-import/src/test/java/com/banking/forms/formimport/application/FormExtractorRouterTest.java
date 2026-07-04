package com.banking.forms.formimport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.formimport.domain.FormImportProvider;
import com.banking.forms.formimport.infrastructure.FormImportProviderRepository;
import com.banking.forms.formimport.spi.FormExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FormExtractorRouterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FormImportProviderRepository repository = mock(FormImportProviderRepository.class);

    private FormExtractorRouter router(FormExtractor... extractors) {
        return new FormExtractorRouter(repository, objectMapper, List.of(extractors));
    }

    private FormExtractor extractor(String code) {
        FormExtractor extractor = mock(FormExtractor.class);
        when(extractor.code()).thenReturn(code);
        return extractor;
    }

    @Test
    void resolvesEnabledProviderAndParsesConfig() {
        FormExtractor pdf = extractor("pdfbox");
        FormExtractorRouter router = router(pdf);
        when(repository.findBySourceTypeAndEnabledTrueOrderByPriorityAsc("PDF"))
                .thenReturn(List.of(provider("pdfbox", "PDF", true, 10, "{\"model\":\"v2\"}")));

        FormExtractorRouter.ResolvedProvider resolved = router.resolve("PDF");

        assertThat(resolved.code()).isEqualTo("pdfbox");
        assertThat(resolved.extractor()).isSameAs(pdf);
        assertThat(resolved.config().string("model")).isEqualTo("v2");
    }

    @Test
    void skipsEnabledProvidersWithoutImplementationAndUsesNext() {
        FormExtractor fallback = extractor("csv");
        FormExtractorRouter router = router(fallback);
        when(repository.findBySourceTypeAndEnabledTrueOrderByPriorityAsc("CSV"))
                .thenReturn(List.of(
                        provider("premium-csv", "CSV", true, 5, null), // no bean -> skipped
                        provider("csv", "CSV", true, 10, null)));

        assertThat(router.resolve("CSV").code()).isEqualTo("csv");
    }

    @Test
    void failsWhenNoEnabledProvider() {
        FormExtractorRouter router = router(extractor("pdfbox"));
        when(repository.findBySourceTypeAndEnabledTrueOrderByPriorityAsc("IMAGE")).thenReturn(List.of());

        assertThatThrownBy(() -> router.resolve("IMAGE")).isInstanceOf(FormImportException.class);
    }

    @Test
    void failsWhenEnabledProviderHasNoImplementation() {
        FormExtractorRouter router = router(extractor("pdfbox"));
        when(repository.findBySourceTypeAndEnabledTrueOrderByPriorityAsc("IMAGE"))
                .thenReturn(List.of(provider("llm-vision", "IMAGE", true, 10, null)));

        assertThatThrownBy(() -> router.resolve("IMAGE")).isInstanceOf(FormImportException.class);
    }

    private FormImportProvider provider(String code, String type, boolean enabled, int priority, String config) {
        return new FormImportProvider(UUID.randomUUID(), code, code, type, enabled, priority, config);
    }
}
