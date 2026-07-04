package com.banking.forms.discovery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A config-driven discovery questionnaire. Holds three JSON documents:
 *
 * <ul>
 *   <li>{@code schemaJson} — the questions presented to the user (sections/fields format, so it can
 *       be rendered by the same form renderer as core forms).
 *   <li>{@code rulesJson} — triage rules mapping answers to recommended forms (see the recommendation
 *       engine).
 *   <li>{@code mappingsJson} — per-target-form pre-population mappings (question key -> target
 *       section/field) used to seed the real application and avoid duplicate data entry.
 * </ul>
 */
@Entity
@Table(name = "discovery_questionnaire")
public class DiscoveryQuestionnaire {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @Lob
    @Column(name = "rules_json", nullable = false)
    private String rulesJson;

    @Lob
    @Column(name = "mappings_json", nullable = false)
    private String mappingsJson;

    @Column(nullable = false, length = 16)
    private String status = "PUBLISHED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected DiscoveryQuestionnaire() {}

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public String getRulesJson() {
        return rulesJson;
    }

    public String getMappingsJson() {
        return mappingsJson;
    }

    public String getStatus() {
        return status;
    }
}
