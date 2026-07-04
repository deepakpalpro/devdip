package com.banking.forms.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.OutboundNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailChannelsTest {

    private OutboundNotification email() {
        return new OutboundNotification(
                UUID.randomUUID(), UUID.randomUUID(), "APPLICATION_APPROVED", "email", "jane@example.com",
                "Subject", "Body", null, "en", Map.of());
    }

    @Test
    void logEmailChannelAlwaysReportsSent() {
        LogEmailChannel channel = new LogEmailChannel();
        DeliveryResult result = channel.send(email(), new ChannelConfig(null));
        assertThat(result.isSent()).isTrue();
        assertThat(result.providerMessageId()).startsWith("log-");
        assertThat(channel.channelId()).isEqualTo("log-email");
    }

    @Test
    @SuppressWarnings("unchecked")
    void smtpChannelFailsSafeWhenMailSenderNotConfigured() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        SmtpEmailChannel channel = new SmtpEmailChannel(provider);

        DeliveryResult result = channel.send(email(), new ChannelConfig(null));

        assertThat(result.isSent()).isFalse();
        assertThat(result.detail()).contains("not configured");
    }

    @Test
    @SuppressWarnings("unchecked")
    void smtpChannelSendsWhenMailSenderAvailable() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        SmtpEmailChannel channel = new SmtpEmailChannel(provider);
        ChannelConfig config = new ChannelConfig(new ObjectMapper().readTree("{\"from\":\"no-reply@bank.local\"}"));

        DeliveryResult result = channel.send(email(), config);

        assertThat(result.isSent()).isTrue();
        verify(sender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}
