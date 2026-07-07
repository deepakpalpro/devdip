package com.banking.forms.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.submission.application.SectionStorageRouter;
import com.banking.forms.submission.application.SectionStorageStrategy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecipientResolverTest {

    private final SectionStorageRouter router = mock(SectionStorageRouter.class);
    private final SectionStorageStrategy strategy = mock(SectionStorageStrategy.class);
    private final RecipientResolver resolver = new RecipientResolver(router);

    private PublishedFormView form() {
        return new PublishedFormView(
                UUID.randomUUID(), UUID.randomUUID(), "LOAN", "Loan Application", "LENDING", StorageStrategy.JSON_BLOB, null);
    }

    @Test
    void resolvesEmailPhoneConsentAndLocale() {
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("emailAddress", "jane@example.com");
        contact.put("mobilePhone", "+61 400 123 456");
        contact.put("marketingConsent", true);
        contact.put("preferredLanguage", "en-AU");
        when(router.resolve(any())).thenReturn(strategy);
        when(strategy.loadAllSections(any())).thenReturn(Map.of("contact", contact));

        Recipient recipient = resolver.resolve(form(), UUID.randomUUID());

        assertThat(recipient.email()).isEqualTo("jane@example.com");
        assertThat(recipient.phone()).isEqualTo("+61 400 123 456");
        assertThat(recipient.consentGranted()).isTrue();
        assertThat(recipient.locale()).isEqualTo("en");
    }

    @Test
    void noContactYieldsEmptyRecipientWithUnknownConsent() {
        when(router.resolve(any())).thenReturn(strategy);
        when(strategy.loadAllSections(any())).thenReturn(Map.of("misc", Map.of("note", "hello")));

        Recipient recipient = resolver.resolve(form(), UUID.randomUUID());

        assertThat(recipient.hasAnyContact()).isFalse();
        assertThat(recipient.consentGiven()).isNull();
        assertThat(recipient.consentGranted()).isFalse();
    }

    @Test
    void ignoresInvalidEmailValues() {
        when(router.resolve(any())).thenReturn(strategy);
        when(strategy.loadAllSections(any())).thenReturn(Map.of("contact", Map.of("email", "not-an-email")));

        Recipient recipient = resolver.resolve(form(), UUID.randomUUID());

        assertThat(recipient.hasEmail()).isFalse();
    }

    @Test
    void failsSafeWhenStorageThrows() {
        when(router.resolve(any())).thenThrow(new RuntimeException("boom"));

        Recipient recipient = resolver.resolve(form(), UUID.randomUUID());

        assertThat(recipient.hasAnyContact()).isFalse();
    }
}
