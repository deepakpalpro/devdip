package com.banking.forms.formdefinition.application;

import com.banking.forms.formdefinition.domain.FormDefinition;
import com.banking.forms.formdefinition.domain.FormVersion;
import com.banking.forms.formdefinition.domain.FormVersionStatus;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.banking.forms.formdefinition.infrastructure.FormVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side (authoring) operations for form definitions and versions: create a definition, add
 * versions, edit draft schemas, and publish. Complements {@link FormQueryService}, which is
 * read-only and only ever surfaces published versions to consumers.
 *
 * <p>Invariants enforced here: form {@code code} is unique per tenant; only {@code DRAFT} versions
 * are editable; publishing a version deprecates the previously published one so a form has at most
 * one active published version.
 */
@Service
@Transactional
public class FormCommandService {

    private static final String EMPTY_SCHEMA = "{\"sections\":[]}";

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormVersionRepository formVersionRepository;
    private final ObjectMapper objectMapper;

    public FormCommandService(
            FormDefinitionRepository formDefinitionRepository,
            FormVersionRepository formVersionRepository,
            ObjectMapper objectMapper) {
        this.formDefinitionRepository = formDefinitionRepository;
        this.formVersionRepository = formVersionRepository;
        this.objectMapper = objectMapper;
    }

    /** Creates a form definition and an initial empty {@code DRAFT} version (v1). */
    public FormDetailView createDefinition(
            UUID tenantId,
            UUID actorId,
            String code,
            String name,
            String category,
            StorageStrategy storageStrategy) {
        formDefinitionRepository
                .findByTenantIdAndCode(tenantId, code)
                .ifPresent(existing -> {
                    throw new FormConflictException("Form code already exists: " + code);
                });

        StorageStrategy strategy = storageStrategy == null ? StorageStrategy.JSON_BLOB : storageStrategy;
        FormDefinition definition =
                new FormDefinition(UUID.randomUUID(), tenantId, code, name, category, strategy);
        formDefinitionRepository.save(definition);

        FormVersion firstVersion = new FormVersion(
                UUID.randomUUID(), definition.getId(), 1, FormVersionStatus.DRAFT, EMPTY_SCHEMA, actorId);
        formVersionRepository.save(firstVersion);

        return toDetail(definition);
    }

    /**
     * Creates a new {@code DRAFT} version. Its schema is cloned from {@code cloneFromVersionId} when
     * supplied, otherwise from the latest existing version (a form is never edited in place once
     * published — you clone forward to a new draft).
     */
    public FormVersionView createVersion(UUID tenantId, UUID actorId, UUID formId, UUID cloneFromVersionId) {
        FormDefinition definition = requireForm(tenantId, formId);
        List<FormVersion> versions =
                formVersionRepository.findByFormDefinitionIdOrderByVersionNumberDesc(formId);

        int nextVersion = versions.isEmpty() ? 1 : versions.get(0).getVersionNumber() + 1;

        String schemaJson;
        if (cloneFromVersionId != null) {
            schemaJson = requireVersion(definition.getId(), cloneFromVersionId).getSchemaJson();
        } else if (!versions.isEmpty()) {
            schemaJson = versions.get(0).getSchemaJson();
        } else {
            schemaJson = EMPTY_SCHEMA;
        }

        FormVersion version = new FormVersion(
                UUID.randomUUID(), formId, nextVersion, FormVersionStatus.DRAFT, schemaJson, actorId);
        formVersionRepository.save(version);
        return toVersionView(version);
    }

    /** Replaces the schema of a {@code DRAFT} version after validating its structure. */
    public FormVersionView updateDraftSchema(UUID tenantId, UUID formId, UUID versionId, JsonNode schema) {
        FormDefinition definition = requireForm(tenantId, formId);
        FormVersion version = requireVersion(definition.getId(), versionId);
        if (version.getStatus() != FormVersionStatus.DRAFT) {
            throw new FormConflictException(
                    "Only DRAFT versions can be edited; version " + version.getVersionNumber()
                            + " is " + version.getStatus());
        }
        validateSchema(schema);
        version.updateSchema(writeSchema(schema));
        formVersionRepository.save(version);
        return toVersionView(version);
    }

    /**
     * Publishes a {@code DRAFT} version and deprecates the previously published version (if any),
     * so the form has a single active published version.
     */
    public FormVersionView publish(UUID tenantId, UUID formId, UUID versionId) {
        FormDefinition definition = requireForm(tenantId, formId);
        FormVersion version = requireVersion(definition.getId(), versionId);
        if (version.getStatus() != FormVersionStatus.DRAFT) {
            throw new FormConflictException(
                    "Only DRAFT versions can be published; version " + version.getVersionNumber()
                            + " is " + version.getStatus());
        }
        validateSchema(readSchema(version.getSchemaJson()));

        formVersionRepository
                .findFirstByFormDefinitionIdAndStatusOrderByVersionNumberDesc(
                        definition.getId(), FormVersionStatus.PUBLISHED)
                .ifPresent(current -> {
                    current.deprecate();
                    formVersionRepository.save(current);
                });

        version.publish(Instant.now());
        formVersionRepository.save(version);
        return toVersionView(version);
    }

    @Transactional(readOnly = true)
    public List<FormAdminSummaryView> listForms(UUID tenantId) {
        return formDefinitionRepository.findByTenantId(tenantId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public FormDetailView getForm(UUID tenantId, UUID formId) {
        return toDetail(requireForm(tenantId, formId));
    }

    private FormDefinition requireForm(UUID tenantId, UUID formId) {
        return formDefinitionRepository
                .findByIdAndTenantId(formId, tenantId)
                .orElseThrow(() -> new FormNotFoundException("Form not found: " + formId));
    }

    private FormVersion requireVersion(UUID formId, UUID versionId) {
        return formVersionRepository
                .findById(versionId)
                .filter(version -> version.getFormDefinitionId().equals(formId))
                .orElseThrow(() -> new FormNotFoundException("Form version not found: " + versionId));
    }

    private FormAdminSummaryView toSummary(FormDefinition definition) {
        List<FormVersion> versions =
                formVersionRepository.findByFormDefinitionIdOrderByVersionNumberDesc(definition.getId());
        Optional<FormVersion> latest = versions.stream().findFirst();
        return new FormAdminSummaryView(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getCategory(),
                definition.getStorageStrategy().name(),
                latest.map(FormVersion::getVersionNumber).orElse(null),
                latest.map(version -> version.getStatus().name()).orElse(null));
    }

    private FormDetailView toDetail(FormDefinition definition) {
        List<FormVersionView> versions =
                formVersionRepository.findByFormDefinitionIdOrderByVersionNumberDesc(definition.getId()).stream()
                        .map(this::toVersionView)
                        .toList();
        return new FormDetailView(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getCategory(),
                definition.getStorageStrategy().name(),
                versions);
    }

    private FormVersionView toVersionView(FormVersion version) {
        return new FormVersionView(
                version.getId(),
                version.getVersionNumber(),
                version.getStatus().name(),
                version.getPublishedAt(),
                readSchema(version.getSchemaJson()));
    }

    /**
     * Validates the structural shape of a form schema: a {@code sections} array of objects each
     * with a non-blank {@code key}/{@code title} and a {@code fields} array whose entries carry a
     * non-blank {@code key}/{@code type}/{@code label}. Section and field keys must be unique within
     * their scope and must not contain a dot (reserved as the KEY_VALUE path separator, see §5.5).
     */
    private void validateSchema(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            throw new FormSchemaException("Schema must be a JSON object");
        }
        JsonNode sections = schema.get("sections");
        if (sections == null || !sections.isArray()) {
            throw new FormSchemaException("Schema must contain a 'sections' array");
        }
        Set<String> sectionKeys = new HashSet<>();
        for (JsonNode section : sections) {
            String sectionKey = requireText(section, "section.key");
            requireText(section, "section.title");
            if (sectionKey.contains(".")) {
                throw new FormSchemaException("Section key must not contain '.': " + sectionKey);
            }
            if (!sectionKeys.add(sectionKey)) {
                throw new FormSchemaException("Duplicate section key: " + sectionKey);
            }
            JsonNode fields = section.get("fields");
            if (fields == null || !fields.isArray()) {
                throw new FormSchemaException("Section '" + sectionKey + "' must contain a 'fields' array");
            }
            Set<String> fieldKeys = new HashSet<>();
            for (JsonNode field : fields) {
                String fieldKey = requireText(field, "field.key");
                requireText(field, "field.type");
                requireText(field, "field.label");
                if (fieldKey.contains(".")) {
                    throw new FormSchemaException("Field key must not contain '.': " + fieldKey);
                }
                if (!fieldKeys.add(fieldKey)) {
                    throw new FormSchemaException(
                            "Duplicate field key '" + fieldKey + "' in section '" + sectionKey + "'");
                }
            }
        }
    }

    private String requireText(JsonNode node, String label) {
        String[] parts = label.split("\\.");
        JsonNode value = node == null ? null : node.get(parts[parts.length - 1]);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new FormSchemaException("Missing or blank '" + parts[parts.length - 1] + "' in " + label);
        }
        return value.asText();
    }

    private JsonNode readSchema(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (Exception ex) {
            throw new FormSchemaException("Stored schema is not valid JSON");
        }
    }

    private String writeSchema(JsonNode schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception ex) {
            throw new FormSchemaException("Unable to serialize schema");
        }
    }
}
