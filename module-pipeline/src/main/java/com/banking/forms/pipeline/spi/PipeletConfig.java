package com.banking.forms.pipeline.spi;

import com.fasterxml.jackson.databind.JsonNode;

/** Per-pipeline-step property overrides merged at runtime. */
public record PipeletConfig(JsonNode raw) {

    public String text(String key, String defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        JsonNode node = raw.get(key);
        return node == null || node.isNull() ? defaultValue : node.asText(defaultValue);
    }

    public boolean bool(String key, boolean defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        JsonNode node = raw.get(key);
        return node == null || node.isNull() ? defaultValue : node.asBoolean(defaultValue);
    }

    public java.util.List<String> stringList(String key) {
        if (raw == null) {
            return java.util.List.of();
        }
        JsonNode node = raw.get(key);
        if (node == null || !node.isArray()) {
            return java.util.List.of();
        }
        java.util.List<String> values = new java.util.ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }
}
