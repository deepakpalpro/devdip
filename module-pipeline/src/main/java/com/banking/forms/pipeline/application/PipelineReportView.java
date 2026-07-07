package com.banking.forms.pipeline.application;

import java.util.List;
import java.util.Map;

/**
 * Combined pipeline report for a submission: the latest run's status, the sanitized payload, the list
 * of fields that were transformed, and the advisory AI evaluation. {@code execution} /
 * {@code sanitizedPayload} / {@code aiEvaluation} are null when the pipeline hasn't produced them.
 */
public record PipelineReportView(
        PipelineExecutionView execution,
        Map<String, Map<String, Object>> sanitizedPayload,
        List<TransformedFieldView> transformedFields,
        AiEvaluationView aiEvaluation) {}
