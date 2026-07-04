package com.banking.forms.pipeline.spi;

/**
 * Pluggable AI risk-evaluation provider for the pipeline {@code AI_EVALUATE} step. Implementations are
 * Spring beans discovered by {@code AiEvaluatorRouter}; the active one is chosen by configuration
 * ({@code pipeline.ai.evaluator}). In-JVM/deterministic evaluators live in {@code module-pipeline};
 * external/LLM-backed ones live in {@code module-service-integration} and implement this same seam.
 *
 * <p>Contract: evaluation is <b>advisory</b> and must run on sanitized data only. Implementations may
 * throw — the router wraps every call and falls back to {@link AiRecommendation#REVIEW}, so the AI step
 * never fails a submission.
 */
public interface AiEvaluator {

    /** Stable id used to select this evaluator via {@code pipeline.ai.evaluator}. */
    String evaluatorId();

    /** Evaluate a sanitized submission and return a risk score + advisory recommendation. */
    AiEvaluationResult evaluate(AiEvaluationContext context);
}
