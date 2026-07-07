package com.banking.forms.serviceintegration.infrastructure;

import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.banking.forms.serviceintegration.spi.AdapterTypes;
import com.banking.forms.serviceintegration.spi.ServiceAdapter;
import com.banking.forms.serviceintegration.spi.ServiceRequest;
import com.banking.forms.serviceintegration.spi.ServiceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RestServiceAdapter implements ServiceAdapter {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper;

    public RestServiceAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String adapterId() {
        return "rest-api";
    }

    @Override
    public String adapterType() {
        return AdapterTypes.REST;
    }

    @Override
    public ServiceResult execute(ServiceRequest request, AdapterConfig config) {
        String endpoint = config.text("endpoint", null);
        if (endpoint == null || endpoint.isBlank()) {
            return ServiceResult.failed("endpoint not configured");
        }

        String method = config.text("method", "POST").toUpperCase();
        String body;
        try {
            body = objectMapper.writeValueAsString(
                    Map.of("operation", request.operation(), "formCode", request.formCode(), "payload", request.payload()));
        } catch (Exception ex) {
            return ServiceResult.failed("unable to serialize request: " + ex.getMessage());
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("X-Operation", request.operation())
                .header("X-Submission-Id", request.submissionId().toString());

        String secret = config.secret();
        if (secret != null && !secret.isBlank()) {
            builder.header("Authorization", "Bearer " + secret);
        }

        builder.method(method, HttpRequest.BodyPublishers.ofString(body));

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return ServiceResult.success("http-" + status, Map.of("body", response.body()));
            }
            return ServiceResult.failed("HTTP " + status + ": " + truncate(response.body(), 200));
        } catch (Exception ex) {
            return ServiceResult.failed(ex.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
