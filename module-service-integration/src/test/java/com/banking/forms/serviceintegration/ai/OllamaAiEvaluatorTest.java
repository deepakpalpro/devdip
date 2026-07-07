package com.banking.forms.serviceintegration.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiRecommendation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Network-free tests for the Ollama response parsing (no daemon required). */
class OllamaAiEvaluatorTest {

    private final OllamaAiEvaluator evaluator =
            new OllamaAiEvaluator(new ObjectMapper(), "http://localhost:11434", "llama3.2", 60);

    @Test
    void parsesValidResponse() {
        AiEvaluationResult result = evaluator.parseResult(
                "{\"riskScore\":0.8,\"recommendation\":\"REJECT\",\"rationale\":\"high risk\"}", 10L);

        assertThat(result.evaluatorId()).isEqualTo("ollama");
        assertThat(result.model()).isEqualTo("llama3.2");
        assertThat(result.recommendation()).isEqualTo(AiRecommendation.REJECT);
        assertThat(result.riskScore()).isEqualTo(0.8);
        assertThat(result.rationale()).isEqualTo("high risk");
    }

    @Test
    void defaultsScoreFromRecommendationWhenMissing() {
        AiEvaluationResult result = evaluator.parseResult("{\"recommendation\":\"APPROVE\"}", 0L);

        assertThat(result.recommendation()).isEqualTo(AiRecommendation.APPROVE);
        assertThat(result.riskScore()).isEqualTo(0.1);
    }

    @Test
    void clampsScoreAndDefaultsUnknownRecommendationToReview() {
        AiEvaluationResult result = evaluator.parseResult("{\"riskScore\":1.5,\"recommendation\":\"maybe\"}", 0L);

        assertThat(result.recommendation()).isEqualTo(AiRecommendation.REVIEW);
        assertThat(result.riskScore()).isEqualTo(1.0);
    }

    @Test
    void invalidJsonThrows() {
        assertThatThrownBy(() -> evaluator.parseResult("not json", 0L)).isInstanceOf(IllegalStateException.class);
    }
}
