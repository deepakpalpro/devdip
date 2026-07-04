package com.banking.forms.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submission")
public class Submission {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "form_version_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID formVersionId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Submission() {}

    public Submission(UUID id, UUID tenantId, UUID formVersionId, UUID userId) {
        this.id = id;
        this.tenantId = tenantId;
        this.formVersionId = formVersionId;
        this.userId = userId;
        this.status = SubmissionStatus.DRAFT;
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

    public UUID getUserId() {
        return userId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assignIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void markSubmitted(Instant submittedAt) {
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
    }

    public void markValidating(Instant at) {
        this.status = SubmissionStatus.VALIDATING;
        this.updatedAt = at;
    }

    /** Returns an in-flight submission to SUBMITTED (e.g. after a failed automated pipeline run). */
    public void revertToSubmitted(Instant at) {
        this.status = SubmissionStatus.SUBMITTED;
        this.updatedAt = at;
    }

    public void markProcessing(Instant at) {
        this.status = SubmissionStatus.PROCESSING;
        this.updatedAt = at;
    }

    public void markUnderReview(Instant at) {
        this.status = SubmissionStatus.PENDING_REVIEW;
        this.updatedAt = at;
    }

    public void markApproved(Instant at) {
        this.status = SubmissionStatus.APPROVED;
        this.updatedAt = at;
    }

    public void markRejected(Instant at) {
        this.status = SubmissionStatus.REJECTED;
        this.updatedAt = at;
    }

    public void markNeedsInfo(Instant at) {
        this.status = SubmissionStatus.NEEDS_INFO;
        this.updatedAt = at;
    }
}
