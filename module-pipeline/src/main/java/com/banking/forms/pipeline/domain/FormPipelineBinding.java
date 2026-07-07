package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Associates a form version with a pipeline and trigger event. */
@Entity
@Table(name = "form_pipeline_binding")
public class FormPipelineBinding {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "form_version_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID formVersionId;

    @Column(name = "pipeline_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID pipelineDefinitionId;

    @Column(name = "trigger_event", nullable = false, length = 32)
    private String triggerEvent;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FormPipelineBinding() {}

    public FormPipelineBinding(
            UUID id, UUID tenantId, UUID formVersionId, UUID pipelineDefinitionId, PipelineTrigger trigger, boolean enabled) {
        this.id = id;
        this.tenantId = tenantId;
        this.formVersionId = formVersionId;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.triggerEvent = trigger.name();
        this.enabled = enabled;
        this.createdAt = Instant.now();
    }

    public void update(UUID pipelineDefinitionId, boolean enabled) {
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getFormVersionId() {
        return formVersionId;
    }

    public UUID getPipelineDefinitionId() {
        return pipelineDefinitionId;
    }

    public PipelineTrigger getTrigger() {
        return PipelineTrigger.valueOf(triggerEvent);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
