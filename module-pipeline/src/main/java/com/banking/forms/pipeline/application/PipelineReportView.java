package com.banking.forms.pipeline.application;

import java.util.List;
import java.util.Map;

/**
 * Combined pipeline report for a submission: the latest run's status plus the sanitized payload and
 * the list of fields that were transformed. {@code execution} / {@code sanitizedPayload} are null
 * when the pipeline hasn't produced them.
 */
public record PipelineReportView(
        PipelineExecutionView execution,
        Map<String, Map<String, Object>> sanitizedPayload,
        List<TransformedFieldView> transformedFields) {}
