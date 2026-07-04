package com.banking.forms.formdefinition.application;

import com.banking.forms.formdefinition.domain.FormVersionStatus;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.banking.forms.formdefinition.infrastructure.FormVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves a form's schema into a fully composed schema by inlining any embedded form references.
 *
 * <p>A field of type {@code embedded_form} with a {@code formCode} is expanded so the referenced
 * (latest published) form's sections are attached under an {@code embeddedForm} node. Composition is
 * recursive with cycle detection and a maximum depth guard so misconfigured references cannot cause
 * infinite expansion.
 */
@Component
public class FormSchemaComposer {

    public static final String EMBEDDED_FIELD_TYPE = "embedded_form";
    private static final int MAX_DEPTH = 5;

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormVersionRepository formVersionRepository;
    private final ObjectMapper objectMapper;

    public FormSchemaComposer(
            FormDefinitionRepository formDefinitionRepository,
            FormVersionRepository formVersionRepository,
            ObjectMapper objectMapper) {
        this.formDefinitionRepository = formDefinitionRepository;
        this.formVersionRepository = formVersionRepository;
        this.objectMapper = objectMapper;
    }

    /** Returns the schema with all embedded form references expanded in place. */
    public JsonNode compose(UUID tenantId, JsonNode schema) {
        expand(tenantId, schema, new ArrayDeque<>(), 0);
        return schema;
    }

    private void expand(UUID tenantId, JsonNode schema, Deque<String> path, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("Embedded form nesting exceeds max depth " + MAX_DEPTH);
        }
        JsonNode sections = schema.get("sections");
        if (sections == null || !sections.isArray()) {
            return;
        }
        for (JsonNode section : sections) {
            JsonNode fields = section.get("fields");
            if (fields == null || !fields.isArray()) {
                continue;
            }
            for (JsonNode field : fields) {
                if (!EMBEDDED_FIELD_TYPE.equals(field.path("type").asText()) || !(field instanceof ObjectNode fieldNode)) {
                    continue;
                }
                expandField(tenantId, fieldNode, path, depth);
            }
        }
    }

    private void expandField(UUID tenantId, ObjectNode fieldNode, Deque<String> path, int depth) {
        String code = fieldNode.path("formCode").asText(null);
        if (code == null || code.isBlank()) {
            fieldNode.put("embeddedUnavailable", true);
            return;
        }
        if (path.contains(code)) {
            throw new IllegalStateException("Cyclic embedded form reference detected for code: " + code);
        }

        Optional<ResolvedForm> resolved = resolve(tenantId, code);
        if (resolved.isEmpty()) {
            fieldNode.put("embeddedUnavailable", true);
            return;
        }

        ResolvedForm form = resolved.get();
        path.push(code);
        expand(tenantId, form.schema(), path, depth + 1);
        path.pop();

        ObjectNode embeddedForm = objectMapper.createObjectNode();
        embeddedForm.put("code", form.code());
        embeddedForm.put("name", form.name());
        embeddedForm.set("sections", form.schema().get("sections"));
        fieldNode.set("embeddedForm", embeddedForm);
    }

    private Optional<ResolvedForm> resolve(UUID tenantId, String code) {
        return formDefinitionRepository
                .findByTenantIdAndCode(tenantId, code)
                .flatMap(definition -> formVersionRepository
                        .findFirstByFormDefinitionIdAndStatusOrderByVersionNumberDesc(
                                definition.getId(), FormVersionStatus.PUBLISHED)
                        .map(version -> new ResolvedForm(
                                definition.getCode(), definition.getName(), parse(version.getSchemaJson()))));
    }

    private JsonNode parse(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid embedded form schema JSON", ex);
        }
    }

    private record ResolvedForm(String code, String name, JsonNode schema) {}
}
