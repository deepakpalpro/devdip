package com.banking.forms.downstream.spi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Non-secret connector configuration (endpoint, topic, bucket, …) read from the
 * {@code downstream_provider.config_json} column. Secrets are never stored here: a connector reads
 * the environment variable named by {@code secretRef} at dispatch time.
 */
public record ConnectorConfig(JsonNode raw) {

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

    /** Resolves the secret from the environment variable named by {@code secretRef}, if present. */
    public String secret() {
        String ref = text("secretRef", null);
        if (ref == null || ref.isBlank()) {
            return null;
        }
        return System.getenv(ref);
    }
}
