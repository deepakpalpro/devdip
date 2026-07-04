package com.banking.forms.serviceintegration.notification;

import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.NotificationChannel;
import com.banking.forms.notification.spi.NotificationChannels;
import com.banking.forms.notification.spi.OutboundNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WhatsApp delivery via Meta's <b>Cloud API</b>, implementing the {@link NotificationChannel} SPI. Lives
 * in {@code module-service-integration} because it calls an outside service. Registered as provider
 * {@code whatsapp-cloud} but <b>disabled by default</b>; an admin must set {@code endpoint},
 * {@code phoneNumberId}, and a {@code secretRef} (env var holding the access token) before enabling it.
 *
 * <p><b>Business rules encoded here:</b>
 * <ul>
 *   <li><b>Template vs. session:</b> business-initiated messages outside the 24-hour customer-service
 *       window must use a pre-approved <em>template</em>. When the rendered message carries a
 *       {@code templateName}, a template message is sent; otherwise a free-form text message is sent
 *       (valid only inside the 24h window).
 *   <li><b>Opt-in:</b> consent is enforced upstream by the notification service; this channel assumes
 *       the recipient has opted in.
 * </ul>
 *
 * <p>Fail-safe: configuration or transport problems return {@link DeliveryResult#failed} so the
 * dispatcher can retry/dead-letter — the channel never throws for expected errors.
 */
@Component
public class WhatsAppCloudChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudChannel.class);
    private static final String DEFAULT_ENDPOINT = "https://graph.facebook.com/v20.0";
    private static final int TIMEOUT_SECONDS = 20;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WhatsAppCloudChannel(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String channelId() {
        return "whatsapp-cloud";
    }

    @Override
    public String channelType() {
        return NotificationChannels.WHATSAPP;
    }

    @Override
    public DeliveryResult send(OutboundNotification notification, ChannelConfig config) {
        String endpoint = stripTrailingSlash(config.text("endpoint", DEFAULT_ENDPOINT));
        String phoneNumberId = config.text("phoneNumberId", null);
        String token = config.secret();

        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return DeliveryResult.failed("WhatsApp not configured: missing phoneNumberId");
        }
        if (token == null || token.isBlank()) {
            return DeliveryResult.failed("WhatsApp not configured: missing access token (secretRef)");
        }

        String url = endpoint + "/" + phoneNumberId + "/messages";
        String payload = buildPayload(notification, config.text("templateLocale", notification.locale()));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return DeliveryResult.failed(
                        "WhatsApp API HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            String messageId = parseMessageId(response.body());
            return DeliveryResult.sent(messageId != null ? messageId : "wa-accepted");
        } catch (Exception ex) {
            log.debug("WhatsApp send failed: {}", ex.toString());
            return DeliveryResult.failed("WhatsApp send failed: " + ex.getMessage());
        }
    }

    /**
     * Builds the Cloud API request body. Uses a <em>template</em> message when the notification carries a
     * provider-approved template name (required outside the 24h window); otherwise a plain-text message.
     * Package-visible for testing (no network).
     */
    String buildPayload(OutboundNotification notification, String locale) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", notification.recipient());

        if (notification.templateName() != null && !notification.templateName().isBlank()) {
            root.put("type", "template");
            ObjectNode template = root.putObject("template");
            template.put("name", notification.templateName());
            ObjectNode language = template.putObject("language");
            language.put("code", locale == null || locale.isBlank() ? "en" : locale);
            // Body text carried as a single parameter for templates that expect one.
            if (notification.body() != null && !notification.body().isBlank()) {
                ArrayNode components = template.putArray("components");
                ObjectNode component = components.addObject();
                component.put("type", "body");
                ArrayNode parameters = component.putArray("parameters");
                ObjectNode parameter = parameters.addObject();
                parameter.put("type", "text");
                parameter.put("text", notification.body());
            }
        } else {
            root.put("type", "text");
            ObjectNode text = root.putObject("text");
            text.put("preview_url", false);
            text.put("body", notification.body() == null ? "" : notification.body());
        }
        return root.toString();
    }

    /** Extracts {@code messages[0].id} from a Cloud API response. Package-visible for testing. */
    String parseMessageId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messages = root.path("messages");
            if (messages.isArray() && !messages.isEmpty()) {
                JsonNode id = messages.get(0).path("id");
                if (id.isTextual()) {
                    return id.asText();
                }
            }
        } catch (Exception ex) {
            log.debug("Could not parse WhatsApp response: {}", ex.getMessage());
        }
        return null;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ENDPOINT;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 300 ? value.substring(0, 300) + "…" : value;
    }
}
