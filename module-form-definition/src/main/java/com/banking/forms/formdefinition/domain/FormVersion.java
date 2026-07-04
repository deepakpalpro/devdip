package com.banking.forms.formdefinition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "form_version")
public class FormVersion {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "form_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID formDefinitionId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FormVersionStatus status;

    @Lob
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    protected FormVersion() {}

    public FormVersion(
            UUID id,
            UUID formDefinitionId,
            int versionNumber,
            FormVersionStatus status,
            String schemaJson,
            UUID createdBy) {
        this.id = id;
        this.formDefinitionId = formDefinitionId;
        this.versionNumber = versionNumber;
        this.status = status;
        this.schemaJson = schemaJson;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFormDefinitionId() {
        return formDefinitionId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public FormVersionStatus getStatus() {
        return status;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    /** Replaces the schema of an editable (draft) version. Published versions are immutable. */
    public void updateSchema(String schemaJson) {
        if (status != FormVersionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT versions can be edited (was " + status + ")");
        }
        this.schemaJson = schemaJson;
    }

    /** Transitions a draft version to PUBLISHED. */
    public void publish(Instant when) {
        if (status != FormVersionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT versions can be published (was " + status + ")");
        }
        this.status = FormVersionStatus.PUBLISHED;
        this.publishedAt = when;
    }

    /** Retires a published version so a newer one can take its place. */
    public void deprecate() {
        if (status != FormVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED versions can be deprecated (was " + status + ")");
        }
        this.status = FormVersionStatus.DEPRECATED;
    }
}
