package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineDefinition;
import com.banking.forms.pipeline.domain.PipelineStep;
import java.util.List;

/** Resolved pipeline definition with ordered steps ready for orchestration. */
public record ResolvedPipeline(PipelineDefinition definition, List<PipelineStep> steps) {}
