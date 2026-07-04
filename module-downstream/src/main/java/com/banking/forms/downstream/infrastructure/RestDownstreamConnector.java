package com.banking.forms.downstream.infrastructure;

import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.ConnectorTypes;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.DownstreamConnector;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Delivers the sanitized envelope to a configurable REST endpoint via HTTP POST (or PUT). Uses the
 * JDK {@link HttpClient} — no extra dependencies. Authentication is via a bearer token resolved from
 * the environment variable named by {@code secretRef} in the provider config.
 */
@Component
public class RestDownstreamConnector implements DownstreamConnector {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    @Override
    public String connectorId() {
        return "rest-webhook";
    }

    @Override
    public String connectorType() {
        return ConnectorTypes.REST;
    }

    @Override
    public DispatchResult dispatch(OutboundEnvelope envelope, ConnectorConfig config) {
        String endpoint = config.text("endpoint", null);
        if (endpoint == null || endpoint.isBlank()) {
            return DispatchResult.failed("endpoint not configured");
        }

        String method = config.text("method", "POST").toUpperCase();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("X-Event-Type", envelope.eventType())
                .header("X-Submission-Id", envelope.submissionId().toString())
                .header("X-Form-Code", envelope.formCode() == null ? "" : envelope.formCode());

        String secret = config.secret();
        if (secret != null && !secret.isBlank()) {
            builder.header("Authorization", "Bearer " + secret);
        }

        String body = envelope.payloadJson() == null ? "{}" : envelope.payloadJson();
        builder.method(method, HttpRequest.BodyPublishers.ofString(body));

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return DispatchResult.dispatched("http-" + status, response.body());
            }
            return DispatchResult.failed("HTTP " + status + ": " + truncate(response.body(), 200));
        } catch (Exception ex) {
            return DispatchResult.failed(ex.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
