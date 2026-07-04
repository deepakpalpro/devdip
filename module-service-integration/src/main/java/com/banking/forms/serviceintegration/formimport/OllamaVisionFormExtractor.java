package com.banking.forms.serviceintegration.formimport;

import com.banking.forms.formimport.application.ExtractedField;
import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * External IMAGE provider (code {@code ollama-vision}) backed by a local/remote <b>Ollama</b> server
 * running a multimodal model (e.g. {@code llava}). Lives in {@code module-service-integration} because
 * it calls an outside service; it implements the same {@link FormExtractor} SPI as the in-JVM parsers,
 * so the router treats it identically once an admin enables + configures it.
 *
 * <p>Uses a thin JDK {@link HttpClient} + Jackson (no extra framework dependency). The image is sent to
 * Ollama's {@code /api/generate} endpoint with {@code format:"json"} so the model returns a structured
 * field list, which is mapped to an {@link ExtractedForm}. AI-inferred fields carry a modest confidence
 * so a human always reviews them before a form is created — appropriate for a regulated context.
 *
 * <p>Config keys ({@code form_import_provider.config_json}): {@code endpoint} (base URL, default
 * {@code http://localhost:11434}), {@code model} (default {@code llava}), {@code prompt} (optional
 * override), {@code timeoutSeconds} (default 120), and optional {@code secretRef} (env var holding a
 * bearer token for a secured/remote Ollama).
 */
@Component
public class OllamaVisionFormExtractor implements FormExtractor {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llava";
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_MAX_IMAGE_DIMENSION = 1024;
    private static final double FIELD_CONFIDENCE = 0.55;
    private static final String DEFAULT_PROMPT =
            "You are analyzing an image of a paper or scanned form. Identify every input a person would "
                    + "fill in. Respond ONLY with a JSON object of the shape "
                    + "{\"title\": string, \"fields\": [{\"label\": string, \"type\": \"text\"|\"number\"|"
                    + "\"choice\"|\"checkbox\", \"required\": boolean, \"options\": [string]}]}. Use \"choice\" "
                    + "for fields with selectable options (radio buttons / dropdowns) and list the options. Use "
                    + "\"checkbox\" for yes/no or consent boxes. Use \"number\" for amounts, income, age, etc. "
                    + "Do not include buttons, headings, or instructions. If unsure, use \"text\".";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaVisionFormExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String code() {
        return "ollama-vision";
    }

    @Override
    public ExtractedForm extract(FormImportSource source, ProviderConfig config) {
        if (!source.hasContent()) {
            throw new FormImportException("No image content provided");
        }
        String baseUrl = stripTrailingSlash(config.string("endpoint", DEFAULT_BASE_URL));
        String model = config.string("model", DEFAULT_MODEL);
        String prompt = config.string("prompt", DEFAULT_PROMPT);
        int timeout = config.integer("timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        int maxDimension = config.integer("maxImageDimension", DEFAULT_MAX_IMAGE_DIMENSION);
        String bearer = config.secret("secretRef");

        // Large images explode the vision-token count and slow CPU inference; downscale first.
        byte[] imageBytes = downscale(source.content(), maxDimension);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String requestBody = buildRequest(model, prompt, base64Image);
        String modelJson = callGenerate(baseUrl, requestBody, bearer, timeout);
        return mapResponse(modelJson);
    }

    private String buildRequest(String model, String prompt, String base64Image) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("prompt", prompt);
        root.put("stream", false);
        root.put("format", "json");
        ArrayNode images = root.putArray("images");
        images.add(base64Image);
        ObjectNode options = root.putObject("options");
        options.put("temperature", 0);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new FormImportException("Failed to build Ollama request", ex);
        }
    }

    private String callGenerate(String baseUrl, String requestBody, String bearer, int timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        if (bearer != null && !bearer.isBlank()) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new FormImportException(
                    "Could not reach Ollama at " + baseUrl
                            + ". Ensure the daemon is running and the model is pulled. Cause: " + ex.getMessage(),
                    ex);
        }
        if (response.statusCode() / 100 != 2) {
            throw new FormImportException(
                    "Ollama returned HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode responseNode = root.get("response");
            if (responseNode == null || responseNode.isNull()) {
                throw new FormImportException("Ollama response is missing the 'response' field");
            }
            return responseNode.asText();
        } catch (FormImportException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormImportException("Could not parse Ollama response envelope: " + ex.getMessage(), ex);
        }
    }

    /** Maps the model's JSON (title + fields) to an {@link ExtractedForm}. Package-visible for tests. */
    ExtractedForm mapResponse(String modelJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(modelJson);
        } catch (Exception ex) {
            throw new FormImportException("Ollama did not return valid JSON: " + truncate(modelJson));
        }

        List<ExtractedField> fields = new ArrayList<>();
        JsonNode fieldsNode = root.get("fields");
        if (fieldsNode != null && fieldsNode.isArray()) {
            for (JsonNode fieldNode : fieldsNode) {
                String label = text(fieldNode, "label");
                if (label == null || label.isBlank()) {
                    continue;
                }
                FieldKind kind = mapKind(text(fieldNode, "type"));
                boolean required = fieldNode.path("required").asBoolean(false);
                List<String> options = new ArrayList<>();
                JsonNode optionsNode = fieldNode.get("options");
                if (optionsNode != null && optionsNode.isArray()) {
                    for (JsonNode option : optionsNode) {
                        String value = option.asText();
                        if (value != null && !value.isBlank()) {
                            options.add(value.trim());
                        }
                    }
                }
                fields.add(new ExtractedField(label.trim(), kind, options, required, FIELD_CONFIDENCE, null));
            }
        }
        String title = text(root, "title");
        return new ExtractedForm(title == null || title.isBlank() ? null : title.trim(), "OLLAMA_VISION", fields);
    }

    private FieldKind mapKind(String type) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT).trim();
        return switch (value) {
            case "number", "numeric", "integer", "decimal", "amount", "currency" -> FieldKind.NUMBER;
            case "choice", "select", "dropdown", "radio", "option", "options" -> FieldKind.CHOICE;
            case "checkbox", "boolean", "bool", "consent" -> FieldKind.CHECKBOX;
            default -> FieldKind.TEXT;
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Best-effort downscale of large images to {@code maxDimension} on the longest side (keeps aspect). */
    byte[] downscale(byte[] imageBytes, int maxDimension) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                return imageBytes;
            }
            int width = source.getWidth();
            int height = source.getHeight();
            int longest = Math.max(width, height);
            if (longest <= maxDimension) {
                return imageBytes;
            }
            double scale = (double) maxDimension / longest;
            int newWidth = Math.max(1, (int) Math.round(width * scale));
            int newHeight = Math.max(1, (int) Math.round(height * scale));
            BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, newWidth, newHeight, Color.WHITE, null);
            graphics.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", out);
            return out.toByteArray();
        } catch (Exception ex) {
            return imageBytes;
        }
    }

    private String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 300 ? value.substring(0, 300) + "…" : value;
    }
}
