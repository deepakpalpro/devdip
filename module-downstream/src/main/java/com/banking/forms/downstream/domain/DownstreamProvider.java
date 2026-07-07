package com.banking.forms.downstream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A configurable downstream delivery provider. Rows are managed from the admin Settings UI
 * (enable/disable, priority, config) rather than hard-coded, so which adapter serves a connector type —
 * and whether an external sink (REST, Kafka, S3) is used at all — is data-driven.
 *
 * <p>{@code code} matches a {@code DownstreamConnector} bean's {@code connectorId()}. {@code configJson}
 * holds non-secret settings (endpoint, topic, bucket, {@code secretRef}); secrets are never stored here.
 */
@Entity
@Table(name = "downstream_provider")
public class DownstreamProvider {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "connector_type", nullable = false, length = 32)
    private String connectorType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DownstreamProvider() {}

    public DownstreamProvider(
            UUID id, String code, String name, String connectorType, boolean enabled, int priority, String configJson) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.connectorType = connectorType;
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

    public String getConnectorType() {
        return connectorType;
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
