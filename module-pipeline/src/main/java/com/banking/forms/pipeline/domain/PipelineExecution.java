package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks one run of the automated processing pipeline for a submission. Maps the pre-existing
 * {@code pipeline_execution} table. {@code pipelineConfigId} is a sentinel until config-driven
 * pipelines land ({@link #SYSTEM_CONFIG_ID}).
 */
@Entity
@Table(name = "pipeline_execution")
public class PipelineExecution {

    public static final UUID SYSTEM_CONFIG_ID = new UUID(0L, 0L);
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "submission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "pipeline_config_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID pipelineConfigId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Lob
    @Column(name = "error_details")
    private String errorDetails;

    protected PipelineExecution() {}

    public PipelineExecution(UUID id, UUID submissionId) {
        this(id, submissionId, SYSTEM_CONFIG_ID);
    }

    public PipelineExecution(UUID id, UUID submissionId, UUID pipelineDefinitionId) {
        this.id = id;
        this.submissionId = submissionId;
        this.pipelineConfigId = pipelineDefinitionId;
        this.status = RUNNING;
        this.currentStep = 0;
        this.startedAt = Instant.now();
    }

    public void advanceTo(int step) {
        this.currentStep = step;
    }

    public void complete(int step) {
        this.currentStep = step;
        this.status = COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(int step, String error) {
        this.currentStep = step;
        this.status = FAILED;
        this.errorDetails = error;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPipelineConfigId() {
        return pipelineConfigId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getStatus() {
        return status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}
