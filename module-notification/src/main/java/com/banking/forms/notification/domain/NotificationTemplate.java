package com.banking.forms.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A message template keyed by (event type, channel, locale). For email {@code subject}/{@code body}
 * hold the rendered text; for WhatsApp {@code subject} holds the provider-approved template name and
 * {@code body} a plain-text fallback. {@code {{placeholder}}} tokens are substituted at render time.
 */
@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "channel_type", nullable = false, length = 32)
    private String channelType;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(length = 256)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String body;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NotificationTemplate() {}

    public NotificationTemplate(
            UUID id, String eventType, String channelType, String locale, String subject, String body) {
        this.id = id;
        this.eventType = eventType;
        this.channelType = channelType;
        this.locale = locale;
        this.subject = subject;
        this.body = body;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getChannelType() {
        return channelType;
    }

    public String getLocale() {
        return locale;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
