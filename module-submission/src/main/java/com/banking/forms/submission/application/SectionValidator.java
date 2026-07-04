package com.banking.forms.submission.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SectionValidator {

    private static final String EMBEDDED_FIELD_TYPE = "embedded_form";

    private final ObjectMapper objectMapper;

    public SectionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> validateSection(JsonNode schema, String sectionKey, Map<String, Object> data) {
        var errors = new HashMap<String, String>();
        JsonNode section = findSection(schema, sectionKey);
        if (section == null) {
            errors.put("_section", "Unknown section: " + sectionKey);
            return errors;
        }
        validateFields(section.get("fields"), data, "", errors);
        return errors;
    }

    /** True if the schema declares a section with the given key (used to guard draft saves). */
    public boolean sectionExists(JsonNode schema, String sectionKey) {
        return findSection(schema, sectionKey) != null;
    }

    public void validateAllSections(JsonNode schema, Map<String, Map<String, Object>> sectionData) {
        JsonNode sections = schema.get("sections");
        if (sections == null || !sections.isArray()) {
            throw new SubmissionValidationException("Form schema has no sections");
        }

        var missing = new ArrayList<String>();
        for (JsonNode section : sections) {
            String sectionKey = section.path("key").asText();
            Map<String, Object> data = sectionData.get(sectionKey);
            if (data == null) {
                missing.add(sectionKey);
                continue;
            }
            var errors = validateSection(schema, sectionKey, data);
            if (!errors.isEmpty()) {
                throw new SubmissionValidationException("Section '" + sectionKey + "' has validation errors");
            }
        }

        if (!missing.isEmpty()) {
            throw new SubmissionValidationException("Missing sections: " + String.join(", ", missing));
        }
    }

    /**
     * Validates a list of fields against a flat data map. Fields of type {@code embedded_form} carry a
     * nested (sections -> fields) data structure that is validated recursively against the composed
     * embedded schema. Error keys are namespaced with a dotted path so nested violations remain
     * distinguishable.
     */
    private void validateFields(JsonNode fields, Map<String, Object> data, String prefix, Map<String, String> errors) {
        if (fields == null || !fields.isArray()) {
            return;
        }
        Map<String, Object> safeData = data == null ? Collections.emptyMap() : data;
        for (JsonNode field : fields) {
            String key = field.path("key").asText();
            boolean required = field.path("required").asBoolean(false);
            Object value = safeData.get(key);

            if (EMBEDDED_FIELD_TYPE.equals(field.path("type").asText())) {
                validateEmbeddedField(field, key, required, value, prefix, errors);
            } else if (required && isBlank(value)) {
                errors.put(prefix + key, "Required");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateEmbeddedField(
            JsonNode field, String key, boolean required, Object value, String prefix, Map<String, String> errors) {
        Map<String, Object> nested = value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        if (required && (nested == null || nested.isEmpty())) {
            errors.put(prefix + key, "Required");
            return;
        }

        JsonNode embeddedForm = field.get("embeddedForm");
        if (embeddedForm == null || nested == null) {
            return;
        }
        JsonNode embeddedSections = embeddedForm.get("sections");
        if (embeddedSections == null || !embeddedSections.isArray()) {
            return;
        }
        for (JsonNode embeddedSection : embeddedSections) {
            String embeddedKey = embeddedSection.path("key").asText();
            Object sectionValue = nested.get(embeddedKey);
            Map<String, Object> sectionData =
                    sectionValue instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
            validateFields(
                    embeddedSection.get("fields"), sectionData, prefix + key + "." + embeddedKey + ".", errors);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(Map<String, Object> data) {
        return objectMapper.convertValue(data, Map.class);
    }

    private JsonNode findSection(JsonNode schema, String sectionKey) {
        JsonNode sections = schema.get("sections");
        if (sections == null || !sections.isArray()) {
            return null;
        }
        for (Iterator<JsonNode> it = sections.elements(); it.hasNext(); ) {
            JsonNode section = it.next();
            if (sectionKey.equals(section.path("key").asText())) {
                return section;
            }
        }
        return null;
    }

    private boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return str.isBlank();
        }
        return false;
    }
}
