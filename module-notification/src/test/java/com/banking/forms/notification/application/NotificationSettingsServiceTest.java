package com.banking.forms.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.notification.domain.NotificationProvider;
import com.banking.forms.notification.infrastructure.NotificationProviderRepository;
import com.banking.forms.notification.infrastructure.NotificationTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationSettingsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationProviderRepository providerRepository = mock(NotificationProviderRepository.class);
    private final NotificationTemplateRepository templateRepository = mock(NotificationTemplateRepository.class);
    private final NotificationChannelRouter router = mock(NotificationChannelRouter.class);
    private final NotificationSettingsService service =
            new NotificationSettingsService(providerRepository, templateRepository, router, objectMapper);

    @Test
    void listFlagsImplementationAvailability() {
        when(providerRepository.findAllByOrderByChannelTypeAscPriorityAsc())
                .thenReturn(List.of(provider("log-email", "email", true, 10, null), provider("whatsapp-cloud", "whatsapp", false, 10, null)));
        when(router.hasImplementation("log-email")).thenReturn(true);
        when(router.hasImplementation("whatsapp-cloud")).thenReturn(false);

        List<NotificationProviderView> views = service.listProviders();

        assertThat(views).hasSize(2);
        assertThat(views.get(0).available()).isTrue();
        assertThat(views.get(1).available()).isFalse();
    }

    @Test
    void updateAppliesEnabledPriorityAndConfig() throws Exception {
        NotificationProvider provider = provider("whatsapp-cloud", "whatsapp", false, 10, null);
        when(providerRepository.findByCode("whatsapp-cloud")).thenReturn(Optional.of(provider));
        when(router.hasImplementation("whatsapp-cloud")).thenReturn(true);
        var config = objectMapper.readTree("{\"phoneNumberId\":\"123\",\"secretRef\":\"TOKEN\"}");

        NotificationProviderView view = service.updateProvider("whatsapp-cloud", true, 5, config);

        assertThat(view.enabled()).isTrue();
        assertThat(view.priority()).isEqualTo(5);
        assertThat(provider.getConfigJson()).contains("phoneNumberId");
        verify(providerRepository).save(any(NotificationProvider.class));
    }

    @Test
    void updateUnknownProviderThrows() {
        when(providerRepository.findByCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProvider("nope", true, 1, null))
                .isInstanceOf(NotificationException.class);
    }

    private NotificationProvider provider(String code, String channelType, boolean enabled, int priority, String config) {
        return new NotificationProvider(UUID.randomUUID(), code, code, channelType, enabled, priority, config);
    }
}
