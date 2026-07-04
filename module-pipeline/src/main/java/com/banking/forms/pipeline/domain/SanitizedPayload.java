package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The PII-sanitized copy of a submission's section data, safe to hand to AI/analytics/downstream.
 * {@code transformedJson} records which fields were transformed and how. Maps
 * {@code submission_sanitized_payload} (one row per submission).
 */
@Entity
@Table(name = "submission_sanitized_payload")
public class SanitizedPayload {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "submission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Lob
    @Column(name = "transformed_json")
    private String transformedJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SanitizedPayload() {}

    public SanitizedPayload(UUID id, UUID submissionId, String payloadJson, String transformedJson) {
        this.id = id;
        this.submissionId = submissionId;
        this.payloadJson = payloadJson;
        this.transformedJson = transformedJson;
        this.createdAt = Instant.now();
    }

    public void update(String payloadJson, String transformedJson) {
        this.payloadJson = payloadJson;
        this.transformedJson = transformedJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getTransformedJson() {
        return transformedJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
