package com.banking.forms.transformation.application;

import com.banking.forms.transformation.application.ScrubResult.TransformedField;
import com.banking.forms.transformation.domain.PiiStrategy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Produces a sanitized copy of a submission's section data by applying the {@link PiiFieldRegistry}'s
 * strategy to each field. Nested maps (embedded forms) are scrubbed recursively; the original input
 * is never mutated. The dot-delimited {@code fieldPath} of every transformed field is reported for
 * the audit trail.
 */
@Component
public class PiiScrubber {

    private static final String HASH_SALT = "banking-forms-pii-v1";
    private static final String REDACTED = "[REDACTED]";
    private static final int VISIBLE_TAIL = 4;

    private final PiiFieldRegistry registry;

    public PiiScrubber(PiiFieldRegistry registry) {
        this.registry = registry;
    }

    public ScrubResult scrub(String formCode, Map<String, Map<String, Object>> sections) {
        Map<String, Map<String, Object>> sanitized = new LinkedHashMap<>();
        List<TransformedField> transformed = new ArrayList<>();
        if (sections != null) {
            for (Map.Entry<String, Map<String, Object>> section : sections.entrySet()) {
                Map<String, Object> out = new LinkedHashMap<>();
                scrubMap(formCode, section.getKey(), section.getValue(), out, transformed);
                sanitized.put(section.getKey(), out);
            }
        }
        return new ScrubResult(sanitized, transformed);
    }

    @SuppressWarnings("unchecked")
    private void scrubMap(
            String formCode,
            String pathPrefix,
            Map<String, Object> in,
            Map<String, Object> out,
            List<TransformedField> transformed) {
        if (in == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : in.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String path = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;

            if (value instanceof Map<?, ?> nested) {
                Map<String, Object> nestedOut = new LinkedHashMap<>();
                scrubMap(formCode, path, (Map<String, Object>) nested, nestedOut, transformed);
                out.put(key, nestedOut);
                continue;
            }

            PiiStrategy strategy = registry.strategyFor(formCode, key);
            if (strategy == PiiStrategy.NONE || value == null) {
                out.put(key, value);
                continue;
            }
            if (strategy == PiiStrategy.REMOVE) {
                transformed.add(new TransformedField(path, strategy.name()));
                continue;
            }
            out.put(key, apply(strategy, String.valueOf(value)));
            transformed.add(new TransformedField(path, strategy.name()));
        }
    }

    private Object apply(PiiStrategy strategy, String value) {
        return switch (strategy) {
            case MASK -> mask(value);
            case HASH -> "sha256:" + hash(value);
            case REDACT -> REDACTED;
            default -> value;
        };
    }

    private String mask(String value) {
        int length = value.length();
        if (length <= VISIBLE_TAIL) {
            return "*".repeat(length);
        }
        return "*".repeat(length - VISIBLE_TAIL) + value.substring(length - VISIBLE_TAIL);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((HASH_SALT + value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6 && i < bytes.length; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
