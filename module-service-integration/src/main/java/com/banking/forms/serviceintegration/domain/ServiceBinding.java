package com.banking.forms.serviceintegration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_binding")
public class ServiceBinding {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "service_instance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID serviceInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ServiceBindingScope scope;

    @Column(name = "form_version_id", columnDefinition = "BINARY(16)")
    private UUID formVersionId;

    @Column(name = "pipeline_definition_id", columnDefinition = "BINARY(16)")
    private UUID pipelineDefinitionId;

    @Column(name = "pipeline_step_id", columnDefinition = "BINARY(16)")
    private UUID pipelineStepId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ServiceBinding() {}

    public ServiceBinding(
            UUID id,
            UUID tenantId,
            UUID serviceInstanceId,
            ServiceBindingScope scope,
            UUID formVersionId,
            UUID pipelineDefinitionId,
            UUID pipelineStepId) {
        this.id = id;
        this.tenantId = tenantId;
        this.serviceInstanceId = serviceInstanceId;
        this.scope = scope;
        this.formVersionId = formVersionId;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.pipelineStepId = pipelineStepId;
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getServiceInstanceId() {
        return serviceInstanceId;
    }

    public ServiceBindingScope getScope() {
        return scope;
    }

    public UUID getFormVersionId() {
        return formVersionId;
    }

    public UUID getPipelineDefinitionId() {
        return pipelineDefinitionId;
    }

    public UUID getPipelineStepId() {
        return pipelineStepId;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
