package com.banking.forms.serviceintegration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_instance")
public class ServiceInstance {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "service_provider_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID serviceProviderId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ServiceInstance() {}

    public ServiceInstance(
            UUID id, UUID tenantId, UUID serviceProviderId, String code, String name, String configJson) {
        this.id = id;
        this.tenantId = tenantId;
        this.serviceProviderId = serviceProviderId;
        this.code = code;
        this.name = name;
        this.configJson = configJson;
        this.enabled = true;
    }

    public void update(String name, String configJson, boolean enabled) {
        this.name = name;
        this.configJson = configJson;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getServiceProviderId() {
        return serviceProviderId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getConfigJson() {
        return configJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
