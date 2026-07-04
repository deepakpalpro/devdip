package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiEvaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the active {@link AiEvaluator} for the pipeline {@code AI_EVALUATE} step and guarantees the
 * step is fail-safe. The evaluator is chosen by {@code pipeline.ai.evaluator} (default {@code heuristic});
 * the step can be turned off entirely with {@code pipeline.ai.enabled=false}.
 *
 * <p>{@link #evaluate(AiEvaluationContext)} never throws: any evaluator failure (exception, timeout,
 * null result, or none configured) degrades to {@link AiEvaluationResult#fallbackReview}, so AI is
 * always advisory and never blocks a submission.
 */
@Component
public class AiEvaluatorRouter {

    private static final Logger log = LoggerFactory.getLogger(AiEvaluatorRouter.class);
    private static final String DEFAULT_EVALUATOR = "heuristic";

    private final Map<String, AiEvaluator> byId;
    private final String configuredId;
    private final boolean enabled;

    public AiEvaluatorRouter(
            List<AiEvaluator> evaluators,
            @Value("${pipeline.ai.evaluator:heuristic}") String configuredId,
            @Value("${pipeline.ai.enabled:true}") boolean enabled) {
        Map<String, AiEvaluator> map = new LinkedHashMap<>();
        for (AiEvaluator evaluator : evaluators) {
            map.putIfAbsent(evaluator.evaluatorId(), evaluator);
        }
        this.byId = map;
        this.configuredId = configuredId;
        this.enabled = enabled;
    }

    /** Whether the AI step should run at all (feature flag + at least one evaluator on the classpath). */
    public boolean isEnabled() {
        return enabled && !byId.isEmpty();
    }

    /** Id of the evaluator that would run, or {@code null} if none is available. */
    public String activeEvaluatorId() {
        AiEvaluator evaluator = resolve();
        return evaluator == null ? null : evaluator.evaluatorId();
    }

    /** Evaluate a sanitized submission, degrading to a REVIEW recommendation on any failure. */
    public AiEvaluationResult evaluate(AiEvaluationContext context) {
        AiEvaluator evaluator = resolve();
        if (evaluator == null) {
            return AiEvaluationResult.fallbackReview("none", "No AI evaluator available");
        }
        try {
            AiEvaluationResult result = evaluator.evaluate(context);
            if (result == null) {
                return AiEvaluationResult.fallbackReview(evaluator.evaluatorId(), "Evaluator returned no result");
            }
            return result;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            log.warn("AI evaluator '{}' failed; falling back to REVIEW: {}", evaluator.evaluatorId(), message);
            return AiEvaluationResult.fallbackReview(evaluator.evaluatorId(), "Evaluator error: " + message);
        }
    }

    private AiEvaluator resolve() {
        AiEvaluator configured = byId.get(configuredId);
        if (configured != null) {
            return configured;
        }
        AiEvaluator heuristic = byId.get(DEFAULT_EVALUATOR);
        if (heuristic != null) {
            return heuristic;
        }
        return byId.values().stream().findFirst().orElse(null);
    }
}
