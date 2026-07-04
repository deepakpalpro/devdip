package com.banking.forms.transformation.application;

import java.util.List;
import java.util.Map;

/**
 * Output of a PII scrub: the sanitized section data plus a summary of what was transformed
 * ({@code fieldPath -> strategy}) for auditing.
 */
public record ScrubResult(Map<String, Map<String, Object>> sanitized, List<TransformedField> transformed) {

    public record TransformedField(String fieldPath, String strategy) {}

    public int transformedCount() {
        return transformed.size();
    }
}
