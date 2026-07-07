package com.banking.forms.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiEvaluator;
import com.banking.forms.pipeline.spi.AiRecommendation;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiEvaluatorRouterTest {

    private static final AiEvaluationContext CTX =
            new AiEvaluationContext(UUID.randomUUID(), "LOAN", Map.of(), Map.of());

    private static AiEvaluator evaluator(String id, AiRecommendation recommendation) {
        return new AiEvaluator() {
            @Override
            public String evaluatorId() {
                return id;
            }

            @Override
            public AiEvaluationResult evaluate(AiEvaluationContext context) {
                return new AiEvaluationResult(id, id + "-model", 0.1, recommendation, "ok", Map.of(), 1L);
            }
        };
    }

    private static AiEvaluator throwing(String id) {
        return new AiEvaluator() {
            @Override
            public String evaluatorId() {
                return id;
            }

            @Override
            public AiEvaluationResult evaluate(AiEvaluationContext context) {
                throw new IllegalStateException("boom");
            }
        };
    }

    @Test
    void picksConfiguredEvaluator() {
        AiEvaluatorRouter router = new AiEvaluatorRouter(
                List.of(evaluator("heuristic", AiRecommendation.APPROVE), evaluator("ollama", AiRecommendation.REJECT)),
                "ollama",
                true);

        assertThat(router.isEnabled()).isTrue();
        assertThat(router.activeEvaluatorId()).isEqualTo("ollama");
        assertThat(router.evaluate(CTX).recommendation()).isEqualTo(AiRecommendation.REJECT);
    }

    @Test
    void fallsBackToHeuristicWhenConfiguredIdMissing() {
        AiEvaluatorRouter router = new AiEvaluatorRouter(
                List.of(evaluator("heuristic", AiRecommendation.APPROVE)), "does-not-exist", true);

        assertThat(router.activeEvaluatorId()).isEqualTo("heuristic");
        assertThat(router.evaluate(CTX).recommendation()).isEqualTo(AiRecommendation.APPROVE);
    }

    @Test
    void failingEvaluatorDegradesToReview() {
        AiEvaluatorRouter router = new AiEvaluatorRouter(List.of(throwing("ollama")), "ollama", true);

        AiEvaluationResult result = router.evaluate(CTX);
        assertThat(result.recommendation()).isEqualTo(AiRecommendation.REVIEW);
        assertThat(result.evaluatorId()).isEqualTo("ollama");
        assertThat(result.rationale()).contains("Evaluator error");
    }

    @Test
    void noEvaluatorsMeansDisabledAndReviewFallback() {
        AiEvaluatorRouter router = new AiEvaluatorRouter(List.of(), "heuristic", true);

        assertThat(router.isEnabled()).isFalse();
        assertThat(router.evaluate(CTX).recommendation()).isEqualTo(AiRecommendation.REVIEW);
    }

    @Test
    void featureFlagDisablesStep() {
        AiEvaluatorRouter router =
                new AiEvaluatorRouter(List.of(evaluator("heuristic", AiRecommendation.APPROVE)), "heuristic", false);

        assertThat(router.isEnabled()).isFalse();
    }
}
