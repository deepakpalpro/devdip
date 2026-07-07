package com.banking.forms.pipeline.domain;

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
@Table(name = "pipeline_job_definition")
public class PipelineJobDefinition {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "form_version_id", columnDefinition = "BINARY(16)")
    private UUID formVersionId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 16)
    private PipelineJobType jobType;

    @Column(name = "pipeline_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID pipelineDefinitionId;

    @Column(name = "trigger_event", length = 32)
    private String triggerEvent;

    @Lob
    @Column(name = "query_config_json")
    private String queryConfigJson;

    @Column(name = "schedule_cron", length = 64)
    private String scheduleCron;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PipelineJobDefinition() {}

    public PipelineJobDefinition(
            UUID id,
            UUID tenantId,
            UUID formVersionId,
            String code,
            String name,
            PipelineJobType jobType,
            UUID pipelineDefinitionId,
            String triggerEvent,
            String queryConfigJson,
            String scheduleCron) {
        this.id = id;
        this.tenantId = tenantId;
        this.formVersionId = formVersionId;
        this.code = code;
        this.name = name;
        this.jobType = jobType;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.triggerEvent = triggerEvent;
        this.queryConfigJson = queryConfigJson;
        this.scheduleCron = scheduleCron;
        this.enabled = true;
    }

    public void update(
            String name,
            UUID formVersionId,
            UUID pipelineDefinitionId,
            String triggerEvent,
            String queryConfigJson,
            String scheduleCron,
            boolean enabled) {
        this.name = name;
        this.formVersionId = formVersionId;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.triggerEvent = triggerEvent;
        this.queryConfigJson = queryConfigJson;
        this.scheduleCron = scheduleCron;
        this.enabled = enabled;
    }

    public void markRunStarted(Instant when) {
        this.lastRunAt = when;
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public PipelineJobType getJobType() {
        return jobType;
    }

    public UUID getPipelineDefinitionId() {
        return pipelineDefinitionId;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public String getQueryConfigJson() {
        return queryConfigJson;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }
}
