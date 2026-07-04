package com.banking.forms.pipeline.spi;

/**
 * Pipeline seam for external service integrations (credit bureau, identity verification, tax APIs, …).
 * Implemented by {@code module-service-integration}; the pipeline invokes it during the SERVICE_CALL
 * step on the PII-scrubbed payload. Fail-safe — failures never fail the pipeline run.
 */
public interface ServiceCallExecutor {

    /** Master switch — when false the SERVICE_CALL step is skipped. */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Invoke all enabled service providers for a submission. Returns the number of providers invoked.
     */
    int invoke(ServiceCallContext context);
}
