package com.banking.forms.notification.spi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Non-secret provider configuration (endpoint, from-address, phone-number-id, …) read from the
 * {@code notification_provider.config_json} column. Secrets are never stored here: a channel reads
 * the environment variable named by {@code secretRef} at send time.
 */
public record ChannelConfig(JsonNode raw) {

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
