package com.banking.forms.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit record of a submission lifecycle event (submitted, review started, approved,
 * etc.). {@code payloadJson} carries structured context such as {@code from}/{@code to} status and a
 * reviewer note.
 */
@Entity
@Table(name = "submission_event")
public class SubmissionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SubmissionEvent() {}

    public SubmissionEvent(UUID submissionId, String eventType, String payloadJson, UUID actorId) {
        this.submissionId = submissionId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.actorId = actorId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public UUID getActorId() {
        return actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
