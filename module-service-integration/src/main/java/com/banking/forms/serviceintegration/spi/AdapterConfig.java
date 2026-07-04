package com.banking.forms.serviceintegration.spi;

import com.fasterxml.jackson.databind.JsonNode;

/** Non-secret adapter configuration read from {@code service_provider.config_json}. */
public record AdapterConfig(JsonNode raw) {

    public String text(String key, String defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        JsonNode node = raw.get(key);
        return node == null || node.isNull() ? defaultValue : node.asText(defaultValue);
    }

    public int number(String key, int defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        JsonNode node = raw.get(key);
        return node == null || node.isNull() ? defaultValue : node.asInt(defaultValue);
    }

    public String secret() {
        String ref = text("secretRef", null);
        if (ref == null || ref.isBlank()) {
            return null;
        }
        return System.getenv(ref);
    }
}
