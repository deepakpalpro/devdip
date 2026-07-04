package com.banking.forms.pipeline.application;

import java.time.Instant;

/** Admin-facing summary of a pipeline run. */
public record PipelineExecutionView(
        String status,
        int currentStep,
        int totalSteps,
        Instant startedAt,
        Instant completedAt,
        String errorDetails) {}
