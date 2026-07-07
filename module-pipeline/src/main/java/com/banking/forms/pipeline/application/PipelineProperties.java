package com.banking.forms.pipeline.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the processing pipeline. Defaults to async mode: submit enqueues an outbox row and
 * returns {@code SUBMITTED}; a worker runs the pipeline off the request path.
 */
@Component
@ConfigurationProperties(prefix = "pipeline")
public class PipelineProperties {

    /** {@code async} (default) enqueues via outbox; {@code sync} runs the pipeline in the submit request. */
    private String processMode = "async";

    /** Max outbox delivery attempts before dead-lettering (marking published with error). */
    private int maxAttempts = 3;

    /** Max unpublished outbox rows processed per dispatcher tick. */
    private int dispatchBatchSize = 10;

    /** Base backoff between retries; grows linearly with attempt count. */
    private long retryBackoffMs = 5_000;

    public boolean isAsync() {
        return !"sync".equalsIgnoreCase(processMode);
    }

    public boolean isSync() {
        return !isAsync();
    }

    public String getProcessMode() {
        return processMode;
    }

    public void setProcessMode(String processMode) {
        this.processMode = processMode;
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
