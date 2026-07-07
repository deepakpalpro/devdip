package com.banking.forms.pipeline.spi;

import java.util.Map;

/**
 * Output of an {@link AiEvaluator}. The {@code riskScore} is normalised to {@code [0.0, 1.0]}
 * (0 = low risk, 1 = high risk); {@code signals} is an explainability map of the factors that drove
 * the score. Results are advisory and always reviewed by a human.
 *
 * @param evaluatorId     id of the evaluator that produced this result
 * @param model           model/algorithm identifier (e.g. {@code rules-v1}, {@code llama3}); may be null
 * @param riskScore       normalised risk in {@code [0.0, 1.0]}
 * @param recommendation  advisory {@link AiRecommendation}
 * @param rationale       short human-readable explanation
 * @param signals         explainability factors (name → value)
 * @param processingTimeMs wall-clock time spent evaluating
 */
public record AiEvaluationResult(
        String evaluatorId,
        String model,
        double riskScore,
        AiRecommendation recommendation,
        String rationale,
        Map<String, Object> signals,
        long processingTimeMs) {

    /**
     * Safe fallback used when an evaluator is unavailable, times out, or returns an invalid response.
     * Sits at mid risk and always recommends a human {@link AiRecommendation#REVIEW}.
     */
    public static AiEvaluationResult fallbackReview(String evaluatorId, String rationale) {
        return new AiEvaluationResult(
                evaluatorId, null, 0.5, AiRecommendation.REVIEW, rationale, Map.of("fallback", true), 0L);
    }
}
