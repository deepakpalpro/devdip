package com.banking.forms.serviceintegration.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.OutboundNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsAppCloudChannelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WhatsAppCloudChannel channel = new WhatsAppCloudChannel(objectMapper);

    private OutboundNotification notification(String templateName) {
        return new OutboundNotification(
                UUID.randomUUID(), UUID.randomUUID(), "APPLICATION_APPROVED", "whatsapp", "+61400000000",
                null, "Your application is approved", templateName, "en", Map.of());
    }

    @Test
    void buildsTemplateMessageWhenTemplateNamePresent() throws Exception {
        String payload = channel.buildPayload(notification("application_approved"), "en");
        JsonNode root = objectMapper.readTree(payload);

        assertThat(root.path("type").asText()).isEqualTo("template");
        assertThat(root.path("to").asText()).isEqualTo("+61400000000");
        assertThat(root.path("template").path("name").asText()).isEqualTo("application_approved");
        assertThat(root.path("template").path("language").path("code").asText()).isEqualTo("en");
    }

    @Test
    void buildsTextMessageWhenNoTemplateName() throws Exception {
        String payload = channel.buildPayload(notification(null), "en");
        JsonNode root = objectMapper.readTree(payload);

        assertThat(root.path("type").asText()).isEqualTo("text");
        assertThat(root.path("text").path("body").asText()).isEqualTo("Your application is approved");
    }

    @Test
    void parsesMessageIdFromCloudApiResponse() {
        String id = channel.parseMessageId("{\"messages\":[{\"id\":\"wamid.ABC123\"}]}");
        assertThat(id).isEqualTo("wamid.ABC123");
    }

    @Test
    void returnsNullMessageIdForUnexpectedResponse() {
        assertThat(channel.parseMessageId("{\"error\":\"x\"}")).isNull();
        assertThat(channel.parseMessageId("not json")).isNull();
    }

    @Test
    void sendFailsSafeWhenNotConfigured() {
        DeliveryResult result = channel.send(notification("application_approved"), new ChannelConfig(null));
        assertThat(result.isSent()).isFalse();
        assertThat(result.detail()).contains("not configured");
    }

    @Test
    void exposesStableChannelIdAndType() {
        assertThat(channel.channelId()).isEqualTo("whatsapp-cloud");
        assertThat(channel.channelType()).isEqualTo("whatsapp");
    }
}
