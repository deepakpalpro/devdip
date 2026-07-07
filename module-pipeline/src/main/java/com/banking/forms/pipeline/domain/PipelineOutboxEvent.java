package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A row in the generic {@code outbox_event} table representing a queued pipeline run. Written
 * {@code published=false} when a submission is submitted (async mode); the {@code PipelineOutboxDispatcher}
 * picks it up, runs {@code SubmissionPipelineService.process}, and marks it published.
 */
@Entity
@Table(name = "outbox_event")
public class PipelineOutboxEvent {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(nullable = false)
    private boolean published;

    @Column(name = "tenant_id", columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "submission_id", columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(nullable = false)
    private int attempts;

    @Lob
    @Column
    private String error;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PipelineOutboxEvent() {}

    public PipelineOutboxEvent(
            UUID id,
            String eventType,
            String payloadJson,
            UUID tenantId,
            UUID submissionId) {
        this.id = id;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.tenantId = tenantId;
        this.submissionId = submissionId;
        this.published = false;
    }

    public void markPublished() {
        this.published = true;
        this.error = null;
        this.attempts += 1;
        this.updatedAt = Instant.now();
    }

    public void markRetryable(String error) {
        this.attempts += 1;
        this.error = error;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.published = true;
        this.error = error;
        this.attempts += 1;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public boolean isPublished() {
        return published;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getError() {
        return error;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
