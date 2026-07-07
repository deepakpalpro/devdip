package com.banking.forms.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the notification subsystem. Defaults keep it demoable with zero setup: enabled, consent
 * not required (so seeded submissions notify), 3 delivery attempts before dead-lettering.
 */
@Component
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {

    /** Master switch — when false, no messages are enqueued or dispatched. */
    private boolean enabled = true;

    /** When true, a positive consent field on the submission is required before contacting a customer. */
    private boolean requireConsent = false;

    /** Delivery attempts before a message is dead-lettered (moved to FAILED). */
    private int maxAttempts = 3;

    /** Max PENDING messages processed per dispatcher tick. */
    private int dispatchBatchSize = 20;

    /** Base backoff between retries; the wait grows linearly with the attempt count. */
    private long retryBackoffMs = 10_000;

    /** Locale used when the submission does not declare one. */
    private String defaultLocale = "en";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequireConsent() {
        return requireConsent;
    }

    public void setRequireConsent(boolean requireConsent) {
        this.requireConsent = requireConsent;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getDispatchBatchSize() {
        return dispatchBatchSize;
    }

    public void setDispatchBatchSize(int dispatchBatchSize) {
        this.dispatchBatchSize = dispatchBatchSize;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
}
