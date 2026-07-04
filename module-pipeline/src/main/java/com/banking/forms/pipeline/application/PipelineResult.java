package com.banking.forms.pipeline.application;

/** Outcome of an automated pipeline run (never thrown — pipeline failures are recorded, not fatal). */
public record PipelineResult(String status, int transformedFields, String error) {

    public static PipelineResult completed(int transformedFields) {
        return new PipelineResult("COMPLETED", transformedFields, null);
    }

    public static PipelineResult failed(String error) {
        return new PipelineResult("FAILED", 0, error);
    }

    public static PipelineResult skipped(String currentStatus) {
        return new PipelineResult("SKIPPED", 0, "status=" + currentStatus);
    }
}
