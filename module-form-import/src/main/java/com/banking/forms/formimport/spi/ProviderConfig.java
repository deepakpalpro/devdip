package com.banking.forms.formimport.spi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Per-provider configuration loaded from the {@code form_import_provider} row's {@code config_json}.
 *
 * <p>Secrets are never stored here: config holds a {@code secretRef} (the name of an environment
 * variable / secret-manager key), and {@link #secret(String)} resolves the actual value at call time.
 */
public final class ProviderConfig {

    private final JsonNode config;

    public ProviderConfig(JsonNode config) {
        this.config = config;
    }

    public static ProviderConfig empty() {
        return new ProviderConfig(null);
    }

    public String string(String key) {
        JsonNode node = config == null ? null : config.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    public String string(String key, String defaultValue) {
        String value = string(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public int integer(String key, int defaultValue) {
        JsonNode node = config == null ? null : config.get(key);
        return node == null || !node.canConvertToInt() ? defaultValue : node.asInt();
    }

    public boolean bool(String key, boolean defaultValue) {
        JsonNode node = config == null ? null : config.get(key);
        return node == null || node.isNull() ? defaultValue : node.asBoolean(defaultValue);
    }

    /** Resolves the secret referenced by config key {@code secretRef} from the environment (never persisted). */
    public String secret(String secretRefKey) {
        String ref = string(secretRefKey);
        if (ref == null || ref.isBlank()) {
            return null;
        }
        return System.getenv(ref);
    }

    public JsonNode raw() {
        return config;
    }
}
