package com.banking.forms.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A configurable delivery provider. Rows are managed from the admin Settings UI (enable/disable,
 * priority, config) rather than hard-coded, so which adapter serves a logical channel — and whether an
 * external provider (SMTP, WhatsApp) is used at all — is data-driven.
 *
 * <p>{@code code} matches a {@code NotificationChannel} bean's {@code channelId()}. {@code configJson}
 * holds non-secret settings (from-address, endpoint, {@code secretRef}); secrets are never stored here.
 */
@Entity
@Table(name = "notification_provider")
public class NotificationProvider {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "channel_type", nullable = false, length = 32)
    private String channelType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Lob
    @Column(name = "config_json")
    private String configJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NotificationProvider() {}

    public NotificationProvider(
            UUID id, String code, String name, String channelType, boolean enabled, int priority, String configJson) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.channelType = channelType;
        this.enabled = enabled;
        this.priority = priority;
        this.configJson = configJson;
    }

    public void update(boolean enabled, int priority, String configJson) {
        this.enabled = enabled;
        this.priority = priority;
        this.configJson = configJson;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getChannelType() {
        return channelType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
