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
@Table(name = "pipeline_job_run")
public class PipelineJobRun {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "job_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID jobDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PipelineJobRunStatus status;

    @Column(name = "records_processed", nullable = false)
    private int recordsProcessed;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected PipelineJobRun() {}

    public PipelineJobRun(UUID id, UUID jobDefinitionId) {
        this.id = id;
        this.jobDefinitionId = jobDefinitionId;
        this.status = PipelineJobRunStatus.RUNNING;
    }

    public void complete(int recordsProcessed) {
        this.status = PipelineJobRunStatus.COMPLETED;
        this.recordsProcessed = recordsProcessed;
        this.finishedAt = Instant.now();
    }

    public void fail(String message) {
        this.status = PipelineJobRunStatus.FAILED;
        this.errorMessage = message;
        this.finishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobDefinitionId() {
        return jobDefinitionId;
    }

    public PipelineJobRunStatus getStatus() {
        return status;
    }

    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
