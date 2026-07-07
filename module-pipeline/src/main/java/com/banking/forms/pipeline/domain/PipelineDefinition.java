package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Assembled pipeline metadata stored by a pipeline author. */
@Entity
@Table(name = "pipeline_definition")
public class PipelineDefinition {

    public static final UUID SYSTEM_DEFAULT_SUBMIT_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PipelineDefinition() {}

    public PipelineDefinition(
            UUID id, UUID tenantId, String code, String name, String description, int version, boolean systemDefault) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        this.status = "ACTIVE";
        this.systemDefault = systemDefault;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String name, String description, String status) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.updatedAt = Instant.now();
    }

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

    public String getDescription() {
        return description;
    }

    public int getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public boolean isSystemDefault() {
        return systemDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
