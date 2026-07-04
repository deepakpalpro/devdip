package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The latest advisory AI risk evaluation for a submission, produced by the pipeline {@code AI_EVALUATE}
 * step from the sanitized payload. {@code recommendation} is stored as a string
 * ({@code APPROVE}/{@code REVIEW}/{@code REJECT}); {@code signalsJson} holds the explainability factors.
 * Maps {@code submission_ai_evaluation} (one row per submission).
 */
@Entity
@Table(name = "submission_ai_evaluation")
public class AiEvaluation {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "submission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "evaluator_id", nullable = false, length = 64)
    private String evaluatorId;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "risk_score", nullable = false)
    private double riskScore;

    @Column(name = "recommendation", nullable = false, length = 16)
    private String recommendation;

    @Lob
    @Column(name = "rationale")
    private String rationale;

    @Lob
    @Column(name = "signals_json")
    private String signalsJson;

    @Column(name = "processing_time_ms", nullable = false)
    private long processingTimeMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AiEvaluation() {}

    public AiEvaluation(
            UUID id,
            UUID submissionId,
            String evaluatorId,
            String model,
            double riskScore,
            String recommendation,
            String rationale,
            String signalsJson,
            long processingTimeMs) {
        this.id = id;
        this.submissionId = submissionId;
        this.evaluatorId = evaluatorId;
        this.model = model;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.rationale = rationale;
        this.signalsJson = signalsJson;
        this.processingTimeMs = processingTimeMs;
        this.createdAt = Instant.now();
    }

    public void update(
            String evaluatorId,
            String model,
            double riskScore,
            String recommendation,
            String rationale,
            String signalsJson,
            long processingTimeMs) {
        this.evaluatorId = evaluatorId;
        this.model = model;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.rationale = rationale;
        this.signalsJson = signalsJson;
        this.processingTimeMs = processingTimeMs;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getEvaluatorId() {
        return evaluatorId;
    }

    public String getModel() {
        return model;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getRationale() {
        return rationale;
    }

    public String getSignalsJson() {
        return signalsJson;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
