package com.banking.forms.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banking.forms.notification.domain.NotificationProvider;
import com.banking.forms.notification.infrastructure.NotificationProviderRepository;
import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.NotificationChannel;
import com.banking.forms.notification.spi.OutboundNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationChannelRouterTest {

    private final NotificationProviderRepository repository = mock(NotificationProviderRepository.class);
    private final NotificationChannelRouter router =
            new NotificationChannelRouter(repository, List.of(channel("log-email", "email"), channel("smtp-email", "email")), new ObjectMapper());

    @Test
    void picksFirstEnabledProviderThatHasAnImplementation() {
        when(repository.findByChannelTypeAndEnabledTrueOrderByPriorityAsc("email"))
                .thenReturn(List.of(
                        provider("sendgrid", "email", 5, null), // enabled, higher priority, but NO bean
                        provider("log-email", "email", 10, null)));

        Optional<NotificationChannelRouter.Selection> selection = router.resolve("email");

        assertThat(selection).isPresent();
        assertThat(selection.get().providerCode()).isEqualTo("log-email");
    }

    @Test
    void emptyWhenNoEnabledProviderForChannel() {
        when(repository.findByChannelTypeAndEnabledTrueOrderByPriorityAsc("whatsapp")).thenReturn(List.of());

        assertThat(router.resolve("whatsapp")).isEmpty();
    }

    @Test
    void resolveProviderParsesConfigJson() {
        when(repository.findByCode("smtp-email"))
                .thenReturn(Optional.of(provider("smtp-email", "email", 20, "{\"from\":\"x@y.com\"}")));

        Optional<NotificationChannelRouter.Selection> selection = router.resolveProvider("smtp-email");

        assertThat(selection).isPresent();
        assertThat(selection.get().config().text("from", null)).isEqualTo("x@y.com");
    }

    @Test
    void resolveProviderEmptyWhenNoImplementationBean() {
        assertThat(router.resolveProvider("unknown")).isEmpty();
        assertThat(router.hasImplementation("log-email")).isTrue();
        assertThat(router.hasImplementation("unknown")).isFalse();
    }

    private static NotificationProvider provider(String code, String channelType, int priority, String config) {
        return new NotificationProvider(UUID.randomUUID(), code, code, channelType, true, priority, config);
    }

    private static NotificationChannel channel(String id, String type) {
        return new NotificationChannel() {
            @Override
            public String channelId() {
                return id;
            }

            @Override
            public String channelType() {
                return type;
            }

            @Override
            public DeliveryResult send(OutboundNotification notification, ChannelConfig config) {
                return DeliveryResult.sent("test");
            }
        };
    }
}
