package com.banking.forms.downstream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single queued/delivered downstream event and the durable transactional outbox row. Written
 * {@code PENDING} by the pipeline in the same transaction that advances the submission, then picked up
 * by the async {@code DownstreamDispatcher}, delivered via its connector, and moved to
 * {@code DISPATCHED}/{@code FAILED}. {@code payloadJson} is the PII-scrubbed body — safe to persist and
 * replay.
 */
@Entity
@Table(name = "downstream_outbox")
public class OutboxEvent {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "submission_id", columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "form_code", length = 64)
    private String formCode;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "connector_type", nullable = false, length = 32)
    private String connectorType;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "provider_ref", length = 256)
    private String providerRef;

    @Lob
    @Column
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected OutboxEvent() {}

    public OutboxEvent(
            UUID id,
            UUID tenantId,
            UUID submissionId,
            String eventType,
            String formCode,
            String providerCode,
            String connectorType,
            String payloadJson,
            OutboxStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.submissionId = submissionId;
        this.eventType = eventType;
        this.formCode = formCode;
        this.providerCode = providerCode;
        this.connectorType = connectorType;
        this.payloadJson = payloadJson;
        this.status = status.name();
    }

    public void markDispatched(String providerRef) {
        this.status = OutboxStatus.DISPATCHED.name();
        this.providerRef = providerRef;
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
        this.status = OutboxStatus.FAILED.name();
        this.error = error;
        this.attempts += 1;
        this.updatedAt = Instant.now();
    }

    public void markSkipped(String reason) {
        this.status = OutboxStatus.SKIPPED.name();
        this.error = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getFormCode() {
        return formCode;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public OutboxStatus getStatus() {
        return OutboxStatus.valueOf(status);
    }

    public int getAttempts() {
        return attempts;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
