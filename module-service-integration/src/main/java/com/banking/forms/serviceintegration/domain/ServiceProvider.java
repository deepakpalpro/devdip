package com.banking.forms.serviceintegration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_provider")
public class ServiceProvider {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "adapter_type", nullable = false, length = 32)
    private String adapterType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ServiceProvider() {}

    public ServiceProvider(
            UUID id, String code, String name, String adapterType, boolean enabled, int priority, String configJson) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.adapterType = adapterType;
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

    public String getAdapterType() {
        return adapterType;
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
