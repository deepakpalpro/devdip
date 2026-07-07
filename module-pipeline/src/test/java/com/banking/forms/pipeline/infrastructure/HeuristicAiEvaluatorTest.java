package com.banking.forms.pipeline.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiRecommendation;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HeuristicAiEvaluatorTest {

    private final HeuristicAiEvaluator evaluator = new HeuristicAiEvaluator();

    private static AiEvaluationContext context(Map<String, Map<String, Object>> data) {
        return new AiEvaluationContext(UUID.randomUUID(), "LOAN", data, Map.of());
    }

    @Test
    void completeLowRiskApplicationIsApproved() {
        AiEvaluationResult result = evaluator.evaluate(context(Map.of(
                "personal", Map.of("name", "Jane Doe", "email", "jane@example.com", "phone", "555-0100"),
                "loan", Map.of("amount", 5000))));

        assertThat(result.evaluatorId()).isEqualTo("heuristic");
        assertThat(result.model()).isEqualTo("rules-v1");
        assertThat(result.recommendation()).isEqualTo(AiRecommendation.APPROVE);
        assertThat(result.riskScore()).isLessThan(0.30);
        assertThat(result.signals()).containsEntry("hasContact", true).containsEntry("highValueAmount", false);
    }

    @Test
    void highValueWithoutContactRaisesToReview() {
        AiEvaluationResult result = evaluator.evaluate(context(Map.of(
                "loan", Map.of("loanAmount", 250000))));

        assertThat(result.recommendation()).isEqualTo(AiRecommendation.REVIEW);
        assertThat(result.signals()).containsEntry("highValueAmount", true).containsEntry("hasContact", false);
    }

    @Test
    void riskKeywordsAndIncompletenessLeadToReject() {
        AiEvaluationResult result = evaluator.evaluate(context(Map.of(
                "loan", Map.of("amount", "$200,000", "note", "account overdue"),
                "personal", Map.of("name", ""))));

        assertThat(result.recommendation()).isEqualTo(AiRecommendation.REJECT);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(0.70);
        assertThat(result.signals()).containsEntry("riskKeywords", true);
    }

    @Test
    void isDeterministic() {
        Map<String, Map<String, Object>> data = Map.of("loan", Map.of("amount", 250000));
        assertThat(evaluator.evaluate(context(data)).riskScore())
                .isEqualTo(evaluator.evaluate(context(data)).riskScore());
    }
}
