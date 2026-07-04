package com.banking.forms.downstream.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the downstream subsystem. Defaults keep it demoable with zero setup: enabled, 3 delivery
 * attempts before dead-lettering.
 */
@Component
@ConfigurationProperties(prefix = "downstream")
public class DownstreamProperties {

    /** Master switch — when false, no outbox rows are enqueued or dispatched. */
    private boolean enabled = true;

    /** Delivery attempts before an outbox row is dead-lettered (moved to FAILED). */
    private int maxAttempts = 3;

    /** Max PENDING rows processed per dispatcher tick. */
    private int dispatchBatchSize = 20;

    /** Base backoff between retries; the wait grows linearly with the attempt count. */
    private long retryBackoffMs = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
