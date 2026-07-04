package com.banking.forms.formimport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.formimport.domain.FormImportProvider;
import com.banking.forms.formimport.infrastructure.FormImportProviderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderSettingsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FormImportProviderRepository repository = mock(FormImportProviderRepository.class);
    private final FormExtractorRouter router = mock(FormExtractorRouter.class);
    private final ProviderSettingsService service = new ProviderSettingsService(repository, router, objectMapper);

    @Test
    void listFlagsImplementationAvailability() {
        when(repository.findAllByOrderBySourceTypeAscPriorityAsc())
                .thenReturn(List.of(
                        provider("pdfbox", "PDF", true, 10, null),
                        provider("llm-vision", "IMAGE", false, 10, null)));
        when(router.hasImplementation("pdfbox")).thenReturn(true);
        when(router.hasImplementation("llm-vision")).thenReturn(false);

        List<ProviderView> views = service.list();

        assertThat(views).hasSize(2);
        assertThat(views.get(0).available()).isTrue();
        assertThat(views.get(1).available()).isFalse();
    }

    @Test
    void updateAppliesEnabledPriorityAndConfig() throws Exception {
        FormImportProvider provider = provider("llm-vision", "IMAGE", false, 10, null);
        when(repository.findByCode("llm-vision")).thenReturn(Optional.of(provider));
        when(router.hasImplementation("llm-vision")).thenReturn(true);
        var config = objectMapper.readTree("{\"endpoint\":\"https://x\",\"secretRef\":\"KEY\"}");

        ProviderView view = service.update("llm-vision", true, 5, config);

        assertThat(view.enabled()).isTrue();
        assertThat(view.priority()).isEqualTo(5);
        assertThat(provider.isEnabled()).isTrue();
        assertThat(provider.getPriority()).isEqualTo(5);
        assertThat(provider.getConfigJson()).contains("endpoint");
        verify(repository).save(any(FormImportProvider.class));
    }

    @Test
    void updateUnknownProviderThrows() {
        when(repository.findByCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("nope", true, 1, null)).isInstanceOf(FormImportException.class);
    }

    private FormImportProvider provider(String code, String type, boolean enabled, int priority, String config) {
        return new FormImportProvider(UUID.randomUUID(), code, code, type, enabled, priority, config);
    }
}
