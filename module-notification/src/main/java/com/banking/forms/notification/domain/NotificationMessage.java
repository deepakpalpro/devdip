package com.banking.forms.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single queued/delivered notification. Doubles as the durable outbox: a {@code PENDING} row is
 * picked up by the async {@code NotificationDispatcher}, sent via its channel, and moved to
 * {@code SENT}/{@code FAILED}. {@code recipient} is the real address (needed for async delivery);
 * views and timeline events mask it.
 */
@Entity
@Table(name = "notification_message")
public class NotificationMessage {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "submission_id", columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "channel_type", nullable = false, length = 32)
    private String channelType;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(nullable = false, length = 256)
    private String recipient;

    @Column(length = 256)
    private String subject;

    @Lob
    @Column
    private String body;

    @Column(name = "template_name", length = 128)
    private String templateName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Lob
    @Column
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NotificationMessage() {}

    public NotificationMessage(
            UUID id,
            UUID tenantId,
            UUID submissionId,
            String eventType,
            String channelType,
            String providerCode,
            String recipient,
            String subject,
            String body,
            String templateName,
            NotificationStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.submissionId = submissionId;
        this.eventType = eventType;
        this.channelType = channelType;
        this.providerCode = providerCode;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.templateName = templateName;
        this.status = status.name();
    }

    public void markSent(String providerMessageId) {
        this.status = NotificationStatus.SENT.name();
        this.providerMessageId = providerMessageId;
        this.error = null;
        this.attempts += 1;
        this.updatedAt = Instant.now();
    }

    public void markDelivered() {
        this.status = NotificationStatus.DELIVERED.name();
        this.updatedAt = Instant.now();
    }

    public void markRetryable(String error) {
        this.attempts += 1;
        this.error = error;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = NotificationStatus.FAILED.name();
        this.error = error;
        this.attempts += 1;
        this.updatedAt = Instant.now();
    }

    public void markSkipped(String reason) {
        this.status = NotificationStatus.SKIPPED.name();
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

    public String getChannelType() {
        return channelType;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getTemplateName() {
        return templateName;
    }

    public NotificationStatus getStatus() {
        return NotificationStatus.valueOf(status);
    }

    public int getAttempts() {
        return attempts;
    }

    public String getProviderMessageId() {
        return providerMessageId;
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
