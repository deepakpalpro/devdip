package com.banking.forms.pipeline.application;

/** Event types written to the generic {@code outbox_event} table for pipeline processing. */
public final class PipelineEventTypes {

    public static final String PIPELINE_REQUESTED = "PIPELINE_REQUESTED";

    private PipelineEventTypes() {}
}
