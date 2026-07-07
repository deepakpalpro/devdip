package com.banking.forms.serviceintegration.ai;

import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiEvaluator;
import com.banking.forms.pipeline.spi.AiRecommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Optional LLM-backed {@link AiEvaluator} (id {@code ollama}) that scores a sanitized submission using a
 * local/remote <b>Ollama</b> text model (e.g. {@code llama3.2}). Lives in
 * {@code module-service-integration} because it calls an outside service; it implements the same
 * {@link AiEvaluator} seam as the built-in {@code heuristic} evaluator.
 *
 * <p><b>Opt-in:</b> the pipeline uses {@code heuristic} unless {@code pipeline.ai.evaluator=ollama}. This
 * bean is always registered but only calls Ollama when selected, and any failure degrades to a
 * {@code REVIEW} recommendation via {@code AiEvaluatorRouter} — so no external runtime is required for
 * the pipeline (or tests) to work.
 *
 * <p>Config ({@code application.yml}): {@code pipeline.ai.ollama.endpoint} (default
 * {@code http://localhost:11434}), {@code pipeline.ai.ollama.model} (default {@code llama3.2}),
 * {@code pipeline.ai.ollama.timeout-seconds} (default 60).
 */
@Component
public class OllamaAiEvaluator implements AiEvaluator {

    private static final String PROMPT_PREAMBLE =
            "You are a bank risk analyst. Assess the fraud/credit risk of the following PII-sanitized "
                    + "application data. Respond ONLY with a JSON object of the shape "
                    + "{\"riskScore\": number between 0 and 1, \"recommendation\": \"APPROVE\"|\"REVIEW\"|"
                    + "\"REJECT\", \"rationale\": string}. Higher riskScore means higher risk. Application "
                    + "data:\n";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final String model;
    private final int timeoutSeconds;

    public OllamaAiEvaluator(
            ObjectMapper objectMapper,
            @Value("${pipeline.ai.ollama.endpoint:http://localhost:11434}") String endpoint,
            @Value("${pipeline.ai.ollama.model:llama3.2}") String model,
            @Value("${pipeline.ai.ollama.timeout-seconds:60}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.endpoint = stripTrailingSlash(endpoint);
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public String evaluatorId() {
        return "ollama";
    }

    @Override
    public AiEvaluationResult evaluate(AiEvaluationContext context) {
        long start = System.currentTimeMillis();
        String dataJson = writeJson(context.sanitizedData() == null ? Map.of() : context.sanitizedData());
        String requestBody = buildRequest(PROMPT_PREAMBLE + dataJson);
        String modelJson = callGenerate(requestBody);
        return parseResult(modelJson, System.currentTimeMillis() - start);
    }

    private String buildRequest(String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("prompt", prompt);
        root.put("stream", false);
        root.put("format", "json");
        ObjectNode options = root.putObject("options");
        options.put("temperature", 0);
        return writeJson(root);
    }

    private String callGenerate(String requestBody) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/api/generate"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not reach Ollama at " + endpoint + " (is the daemon running and model pulled?): "
                            + ex.getMessage(),
                    ex);
        }
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
        }
        try {
            JsonNode envelope = objectMapper.readTree(response.body());
            JsonNode responseNode = envelope.get("response");
            if (responseNode == null || responseNode.isNull()) {
                throw new IllegalStateException("Ollama response missing 'response' field");
            }
            return responseNode.asText();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Ollama response envelope: " + ex.getMessage(), ex);
        }
    }

    /** Parses the model's JSON into a result. Package-visible for network-free tests. */
    AiEvaluationResult parseResult(String modelJson, long processingTimeMs) {
        JsonNode root;
        try {
            root = objectMapper.readTree(modelJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Ollama did not return valid JSON");
        }
        AiRecommendation recommendation = mapRecommendation(text(root, "recommendation"));
        double riskScore = clamp(root.path("riskScore").asDouble(defaultScore(recommendation)));
        String rationale = text(root, "rationale");
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("source", "ollama");
        signals.put("model", model);
        return new AiEvaluationResult(
                evaluatorId(),
                model,
                riskScore,
                recommendation,
                rationale == null || rationale.isBlank() ? "Ollama risk assessment" : rationale.trim(),
                signals,
                processingTimeMs);
    }

    private static AiRecommendation mapRecommendation(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVE", "APPROVED", "LOW", "PASS" -> AiRecommendation.APPROVE;
            case "REJECT", "REJECTED", "DECLINE", "HIGH", "FAIL" -> AiRecommendation.REJECT;
            default -> AiRecommendation.REVIEW;
        };
    }

    private static double defaultScore(AiRecommendation recommendation) {
        return switch (recommendation) {
            case APPROVE -> 0.1;
            case REJECT -> 0.9;
            default -> 0.5;
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize Ollama request", ex);
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
