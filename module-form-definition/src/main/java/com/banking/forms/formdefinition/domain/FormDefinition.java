package com.banking.forms.formdefinition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "form_definition")
public class FormDefinition {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 64)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_strategy", nullable = false, length = 16)
    private StorageStrategy storageStrategy = StorageStrategy.JSON_BLOB;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected FormDefinition() {}

    public FormDefinition(UUID id, UUID tenantId, String code, String name, String category) {
        this(id, tenantId, code, name, category, StorageStrategy.JSON_BLOB);
    }

    public FormDefinition(
            UUID id, UUID tenantId, String code, String name, String category, StorageStrategy storageStrategy) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.category = category;
        this.storageStrategy = storageStrategy;
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

    public String getCategory() {
        return category;
    }

    public StorageStrategy getStorageStrategy() {
        return storageStrategy;
    }
}
