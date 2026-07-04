package com.banking.forms.formimport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A configurable extraction provider. Rows are managed from the admin Settings UI (enable/disable,
 * priority, config) rather than hard-coded, so which engine handles a source type — and whether an
 * external OCR/LLM provider is used at all — is data-driven.
 *
 * <p>{@code code} matches a {@code FormExtractor} bean's {@code code()}. {@code configJson} holds
 * non-secret settings (endpoint, model, {@code secretRef}); secrets themselves are never stored here.
 */
@Entity
@Table(name = "form_import_provider")
public class FormImportProvider {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected FormImportProvider() {}

    public FormImportProvider(
            UUID id, String code, String name, String sourceType, boolean enabled, int priority, String configJson) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sourceType = sourceType;
        this.enabled = enabled;
        this.priority = priority;
        this.configJson = configJson;
    }

    public void update(boolean enabled, int priority, String configJson) {
        this.enabled = enabled;
        this.priority = priority;
        this.configJson = configJson;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
