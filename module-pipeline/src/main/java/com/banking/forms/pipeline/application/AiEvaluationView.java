package com.banking.forms.pipeline.application;

import java.time.Instant;
import java.util.Map;

/** Admin-facing view of a submission's advisory AI evaluation (part of the pipeline report). */
public record AiEvaluationView(
        String evaluatorId,
        String model,
        double riskScore,
        String recommendation,
        String rationale,
        Map<String, Object> signals,
        Instant evaluatedAt) {}
