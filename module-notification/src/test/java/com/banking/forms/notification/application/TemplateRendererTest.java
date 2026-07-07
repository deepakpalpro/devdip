package com.banking.forms.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.notification.domain.NotificationTemplate;
import com.banking.forms.notification.infrastructure.NotificationTemplateRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private final NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
    private final TemplateRenderer renderer = new TemplateRenderer(repository);

    @Test
    void rendersEmailSubjectAndBodyWithSubstitution() {
        when(repository.findByEventTypeAndChannelTypeAndLocale("APPLICATION_APPROVED", "email", "en"))
                .thenReturn(Optional.of(new NotificationTemplate(
                        UUID.randomUUID(),
                        "APPLICATION_APPROVED",
                        "email",
                        "en",
                        "Your {{formName}} is approved",
                        "Reference {{reference}} approved")));

        RenderedMessage message =
                renderer.render("APPLICATION_APPROVED", "email", "en", Map.of("formName", "Loan", "reference", "ABC123"));

        assertThat(message.subject()).isEqualTo("Your Loan is approved");
        assertThat(message.body()).isEqualTo("Reference ABC123 approved");
        assertThat(message.templateName()).isNull();
    }

    @Test
    void whatsAppUsesSubjectAsTemplateNameAndBodyAsFallback() {
        when(repository.findByEventTypeAndChannelTypeAndLocale("APPLICATION_APPROVED", "whatsapp", "en"))
                .thenReturn(Optional.of(new NotificationTemplate(
                        UUID.randomUUID(),
                        "APPLICATION_APPROVED",
                        "whatsapp",
                        "en",
                        "application_approved",
                        "Approved {{reference}}")));

        RenderedMessage message =
                renderer.render("APPLICATION_APPROVED", "whatsapp", "en", Map.of("reference", "ABC123"));

        assertThat(message.templateName()).isEqualTo("application_approved");
        assertThat(message.body()).isEqualTo("Approved ABC123");
        assertThat(message.subject()).isNull();
    }

    @Test
    void fallsBackToBuiltInDefaultWhenNoTemplateRow() {
        when(repository.findByEventTypeAndChannelTypeAndLocale(any(), any(), any())).thenReturn(Optional.empty());

        RenderedMessage message =
                renderer.render("APPLICATION_SUBMITTED", "email", "fr", Map.of("formName", "Loan", "reference", "X1"));

        assertThat(message.subject()).contains("Loan");
        assertThat(message.body()).contains("X1");
    }
}
